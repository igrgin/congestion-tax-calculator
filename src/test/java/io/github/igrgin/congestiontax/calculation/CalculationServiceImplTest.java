package io.github.igrgin.congestiontax.calculation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import io.github.igrgin.congestiontax.calculation.exception.CityNotFoundException;
import io.github.igrgin.congestiontax.calculation.exception.InvalidStoredTaxRuleOptionException;
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
import io.micrometer.core.instrument.Tag;
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
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        var calculationMetrics = new CalculationMetrics(meterRegistry);

        calculationService = new CalculationServiceImpl(taxRuleService, taxCalculator, calculationMetrics);
    }

    @ParameterizedTest
    @MethodSource("cityTimes")
    void calculatesTaxWithStoredCityTimeZone(LocalDateTime cityDateTime, Instant passageInstant) {
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

        var result =
                calculationService.calculate(new CalculationCommand(cityCode, vehicleTypeCode, List.of(cityDateTime)));

        assertThat(result).isEqualTo(new CalculatedTax(cityCode, calculationResult));

        assertCalculationTimer("success");
    }

    @Test
    void translatesUnknownCityAndRecordsRejectedOutcome() {
        var cityCode = "unknown";
        var cityDateTime = LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27);
        var command = new CalculationCommand(cityCode, "OTHER", List.of(cityDateTime));
        var calculationDates = Set.of(cityDateTime.toLocalDate());
        var cause = new UnknownCityException(cityCode);

        given(taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .willThrow(cause);

        assertThatThrownBy(() -> calculationService.calculate(command))
                .isInstanceOf(CityNotFoundException.class)
                .hasMessage("City does not exist: unknown.")
                .hasCause(cause);

        assertCalculationTimer("rejected");
    }

    @Test
    void translatesUnknownVehicleTypeAndRecordsRejectedOutcome() {
        var cityCode = "gothenburg";
        var vehicleTypeCode = "UNKNOWN";
        var cityDateTime = LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27);
        var command = new CalculationCommand(cityCode, vehicleTypeCode, List.of(cityDateTime));
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

        assertCalculationTimer("rejected");
    }

    @Test
    void recordsFailedOutcomeForUnexpectedFailure() {
        var cityCode = "gothenburg";
        var cityDateTime = LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27);
        var command = new CalculationCommand(cityCode, "OTHER", List.of(cityDateTime));
        var calculationDates = Set.of(cityDateTime.toLocalDate());
        var exception = new IllegalStateException("Unexpected failure.");

        given(taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .willThrow(exception);

        assertThatThrownBy(() -> calculationService.calculate(command)).isSameAs(exception);

        assertCalculationTimer("failed");
    }

    @Test
    void translatesInvalidStoredTaxRuleOptionAndRecordsFailedOutcome() {
        var cityCode = "gothenburg";
        var cityDateTime = LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27);
        var command = new CalculationCommand(cityCode, "OTHER", List.of(cityDateTime));
        var calculationDates = Set.of(cityDateTime.toLocalDate());
        var cause = new InvalidTaxRuleOptionException("CHARGE_WINDOW");

        given(taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .willThrow(cause);

        assertThatThrownBy(() -> calculationService.calculate(command))
                .isInstanceOfSatisfying(InvalidStoredTaxRuleOptionException.class, exception -> {
                    assertThat(exception.optionTypeCode()).isEqualTo("CHARGE_WINDOW");
                    assertThat(exception).hasCause(cause);
                });

        assertCalculationTimer("failed");
    }

    private static Stream<Arguments> cityTimes() {
        return Stream.of(
                Arguments.of(
                        LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27), Instant.parse("2013-02-08T05:20:27Z")),
                Arguments.of(LocalDateTime.of(2013, Month.JULY, 8, 6, 20, 27), Instant.parse("2013-07-08T04:20:27Z")));
    }

    private void assertCalculationTimer(String outcome) {
        var timers = meterRegistry.find("congestion.tax.calculation").timers();

        assertThat(timers).singleElement().satisfies(timer -> {
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.getId().getTags()).containsExactly(Tag.of("outcome", outcome));
        });
    }
}
