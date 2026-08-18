package io.github.igrgin.congestiontax.calculation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.igrgin.congestiontax.calculation.exception.CityNotFoundException;
import io.github.igrgin.congestiontax.calculation.exception.InvalidPassageTimestampException;
import io.github.igrgin.congestiontax.calculation.exception.InvalidStoredTaxRuleOptionException;
import io.github.igrgin.congestiontax.calculation.exception.UnsupportedPassageYearException;
import io.github.igrgin.congestiontax.calculation.exception.VehicleTypeNotFoundException;
import io.github.igrgin.congestiontax.calculation.model.CalculatedTax;
import io.github.igrgin.congestiontax.calculation.model.CalculationCommand;
import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.calculation.CalculationResult;
import io.github.igrgin.congestiontax.domain.calculation.DailyTax;
import io.github.igrgin.congestiontax.domain.calculation.Passage;
import io.github.igrgin.congestiontax.domain.calculation.TaxCalculator;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.domain.rule.TaxTimeBand;
import io.github.igrgin.congestiontax.metrics.CalculationMetrics;
import io.github.igrgin.congestiontax.taxrule.TaxRuleService;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidTaxRuleOptionException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import io.github.igrgin.congestiontax.taxrule.model.ApplicableTaxRuleSets;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalculationServiceImplTest {

    @Mock
    private TaxRuleService taxRuleService;

    @Mock
    private TaxCalculator taxCalculator;

    private CalculationService calculationService;

    @BeforeEach
    void setUp() {
        var calculationMetrics = new CalculationMetrics(new SimpleMeterRegistry());

        calculationService = new CalculationServiceImpl(taxRuleService, taxCalculator, calculationMetrics);
    }

    @ParameterizedTest
    @MethodSource("cityTimes")
    void calculatesTaxWithStoredCityTimeZone(
            String passageTimestamp, LocalDateTime cityDateTime, Instant passageInstant) {
        var cityCode = "gothenburg";
        var vehicleTypeCode = "OTHER";
        var calculationDate = cityDateTime.toLocalDate();
        var currency = Currency.getInstance("SEK");
        var taxAmount = new TaxAmount(new BigDecimal("8.00"), currency);
        var passage = new Passage(passageInstant, cityDateTime);
        var passages = List.of(passage);
        var calculationDates = Set.of(calculationDate);
        var vehicleType = new VehicleType(vehicleTypeCode, "Other vehicle");
        var taxRuleSet = new TaxRuleSet(
                cityCode,
                LocalDate.of(2013, Month.JANUARY, 1),
                currency,
                List.of(new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), taxAmount)));
        var applicableTaxRuleSets = Map.of(calculationDate, taxRuleSet);
        var calculationResult =
                new CalculationResult(vehicleType, List.of(new DailyTax(calculationDate, taxAmount)), taxAmount);

        given(taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .willReturn(new ApplicableTaxRuleSets(ZoneId.of("Europe/Stockholm"), applicableTaxRuleSets));
        given(taxRuleService.getVehicleType(vehicleTypeCode)).willReturn(vehicleType);
        given(taxCalculator.calculate(vehicleType, passages, applicableTaxRuleSets))
                .willReturn(calculationResult);

        var result = calculationService.calculate(
                new CalculationCommand(cityCode, vehicleTypeCode, List.of(passageTimestamp)));

        assertThat(result).isEqualTo(new CalculatedTax(cityCode, calculationResult));
    }

    @Test
    void translatesUnknownCity() {
        var cityCode = "unknown";
        var cityDateTime = LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27);
        var command = new CalculationCommand(cityCode, "OTHER", List.of("2013-02-08 06:20:27"));
        var calculationDates = Set.of(cityDateTime.toLocalDate());
        var cause = new UnknownCityException(cityCode);

        given(taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .willThrow(cause);

        assertThatThrownBy(() -> calculationService.calculate(command))
                .isInstanceOf(CityNotFoundException.class)
                .hasMessage("City does not exist: unknown.")
                .hasCause(cause);
    }

    @Test
    void translatesUnknownVehicleType() {
        var cityCode = "gothenburg";
        var vehicleTypeCode = "UNKNOWN";
        var cityDateTime = LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27);
        var command = new CalculationCommand(cityCode, vehicleTypeCode, List.of("2013-02-08 06:20:27"));
        var calculationDate = LocalDate.of(2013, Month.FEBRUARY, 8);
        var applicableTaxRuleSets = Map.<LocalDate, TaxRuleSet>of();
        var cause = new UnknownVehicleTypeException(vehicleTypeCode);

        given(taxRuleService.getApplicableTaxRuleSets(cityCode, Set.of(calculationDate)))
                .willReturn(new ApplicableTaxRuleSets(ZoneId.of("Europe/Stockholm"), applicableTaxRuleSets));
        given(taxRuleService.getVehicleType(vehicleTypeCode)).willThrow(cause);

        assertThatThrownBy(() -> calculationService.calculate(command))
                .isInstanceOf(VehicleTypeNotFoundException.class)
                .hasMessage("Vehicle Type does not exist: UNKNOWN.")
                .hasCause(cause);
    }

    @Test
    void propagatesUnexpectedFailure() {
        var cityCode = "gothenburg";
        var cityDateTime = LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27);
        var command = new CalculationCommand(cityCode, "OTHER", List.of("2013-02-08 06:20:27"));
        var calculationDates = Set.of(cityDateTime.toLocalDate());
        var exception = new IllegalStateException("Unexpected failure.");

        given(taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .willThrow(exception);

        assertThatThrownBy(() -> calculationService.calculate(command)).isSameAs(exception);
    }

    @Test
    void translatesInvalidStoredTaxRuleOption() {
        var cityCode = "gothenburg";
        var cityDateTime = LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27);
        var command = new CalculationCommand(cityCode, "OTHER", List.of("2013-02-08 06:20:27"));
        var calculationDates = Set.of(cityDateTime.toLocalDate());
        var cause = new InvalidTaxRuleOptionException("CHARGE_WINDOW");

        given(taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .willThrow(cause);

        assertThatThrownBy(() -> calculationService.calculate(command))
                .isInstanceOfSatisfying(InvalidStoredTaxRuleOptionException.class, exception -> {
                    assertThat(exception.optionTypeCode()).isEqualTo("CHARGE_WINDOW");
                    assertThat(exception).hasCause(cause);
                });
    }

    @Test
    void rejectsFirstMalformedPassageBeforeYearValidationAndTaxRuleLookup() {
        var command = new CalculationCommand(
                "gothenburg", "OTHER", List.of("2012-02-08 06:20:27", "invalid", "2013-02-08T07:20:27"));

        assertThatThrownBy(() -> calculationService.calculate(command))
                .isInstanceOfSatisfying(
                        InvalidPassageTimestampException.class,
                        exception -> assertThat(exception.passageIndex()).isEqualTo(1));

        verifyNoInteractions(taxRuleService, taxCalculator);
    }

    @Test
    void rejectsEveryUnsupportedPassageYearBeforeTaxRuleLookup() {
        var command = new CalculationCommand(
                "gothenburg",
                "OTHER",
                List.of("2012-12-31 23:59:59", "2013-06-15 12:00:00", "2014-01-01 00:00:00", "2020-02-29 18:30:00"));

        assertThatThrownBy(() -> calculationService.calculate(command))
                .isInstanceOfSatisfying(
                        UnsupportedPassageYearException.class,
                        exception -> assertThat(exception.passageIndexes()).containsExactly(0, 2, 3));

        verifyNoInteractions(taxRuleService, taxCalculator);
    }

    private static Stream<Arguments> cityTimes() {
        return Stream.of(
                Arguments.of(
                        "2013-01-01 00:00:00",
                        LocalDateTime.of(2013, Month.JANUARY, 1, 0, 0),
                        Instant.parse("2012-12-31T23:00:00Z")),
                Arguments.of(
                        "2013-02-08 06:20:27",
                        LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27),
                        Instant.parse("2013-02-08T05:20:27Z")),
                Arguments.of(
                        "2013-07-08 06:20:27",
                        LocalDateTime.of(2013, Month.JULY, 8, 6, 20, 27),
                        Instant.parse("2013-07-08T04:20:27Z")),
                Arguments.of(
                        "2013-12-31 23:59:59",
                        LocalDateTime.of(2013, Month.DECEMBER, 31, 23, 59, 59),
                        Instant.parse("2013-12-31T22:59:59Z")));
    }
}
