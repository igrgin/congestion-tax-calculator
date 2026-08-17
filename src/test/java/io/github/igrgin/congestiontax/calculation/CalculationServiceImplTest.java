package io.github.igrgin.congestiontax.calculation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import io.github.igrgin.congestiontax.calculation.exception.CityNotFoundException;
import io.github.igrgin.congestiontax.calculation.exception.VehicleTypeNotFoundException;
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
import io.github.igrgin.congestiontax.taxrule.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    void calculatesTaxWithCityLocalTimeAndApplicableTaxRules() {
        var cityCode = "gothenburg";
        var vehicleTypeCode = "OTHER";
        var passageInstant = Instant.parse("2013-02-08T05:20:27Z");
        var calculationDate = LocalDate.of(2013, 2, 8);
        var currency = Currency.getInstance("SEK");
        var taxAmount = new TaxAmount(new BigDecimal("8.00"), currency);
        var passage = new Passage(passageInstant, LocalDateTime.of(2013, 2, 8, 6, 20, 27));
        var passages = List.of(passage);
        var calculationDates = Set.of(calculationDate);
        var vehicleType = new VehicleType(vehicleTypeCode, "Other vehicle");
        var taxRuleSet = new TaxRuleSet(
                cityCode,
                LocalDate.of(2013, 1, 1),
                currency,
                List.of(new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), taxAmount)));
        var applicableTaxRuleSets = Map.of(calculationDate, taxRuleSet);
        var calculationResult =
                new CalculationResult(vehicleType, List.of(new DailyTax(calculationDate, taxAmount)), taxAmount);

        given(taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .willReturn(applicableTaxRuleSets);
        given(taxRuleService.getVehicleType(vehicleTypeCode)).willReturn(vehicleType);
        given(taxCalculator.calculate(vehicleType, passages, applicableTaxRuleSets))
                .willReturn(calculationResult);

        var result = calculationService.calculate(new CalculationCommand(cityCode, vehicleTypeCode, passages));

        assertThat(result).isEqualTo(new CalculatedTax(cityCode, calculationResult));

        assertCalculationTimer("success");
    }

    @Test
    void translatesUnknownCityAndRecordsRejectedOutcome() {
        var cityCode = "unknown";
        var passage = new Passage(Instant.parse("2013-02-08T05:20:27Z"), LocalDateTime.of(2013, 2, 8, 6, 20, 27));
        var command = new CalculationCommand(cityCode, "OTHER", List.of(passage));
        var calculationDates = Set.of(LocalDate.of(2013, 2, 8));
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
        var passageInstant = Instant.parse("2013-02-08T05:20:27Z");
        var passages = List.of(new Passage(passageInstant, LocalDateTime.of(2013, 2, 8, 6, 20, 27)));
        var command = new CalculationCommand(cityCode, vehicleTypeCode, passages);
        var calculationDate = LocalDate.of(2013, 2, 8);
        var applicableTaxRuleSets = Map.<LocalDate, TaxRuleSet>of();
        var cause = new UnknownVehicleTypeException(vehicleTypeCode);

        given(taxRuleService.getApplicableTaxRuleSets(cityCode, Set.of(calculationDate)))
                .willReturn(applicableTaxRuleSets);
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
        var passage = new Passage(Instant.parse("2013-02-08T05:20:27Z"), LocalDateTime.of(2013, 2, 8, 6, 20, 27));
        var command = new CalculationCommand(cityCode, "OTHER", List.of(passage));
        var calculationDates = Set.of(LocalDate.of(2013, 2, 8));
        var exception = new IllegalStateException("Unexpected failure.");

        given(taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .willThrow(exception);

        assertThatThrownBy(() -> calculationService.calculate(command)).isSameAs(exception);

        assertCalculationTimer("failed");
    }

    private void assertCalculationTimer(String outcome) {
        var timers = meterRegistry.find("congestion.tax.calculation").timers();

        assertThat(timers).singleElement().satisfies(timer -> {
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.getId().getTags()).containsExactly(Tag.of("outcome", outcome));
        });
    }
}
