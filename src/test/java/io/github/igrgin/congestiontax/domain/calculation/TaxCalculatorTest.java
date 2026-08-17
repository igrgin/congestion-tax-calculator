package io.github.igrgin.congestiontax.domain.calculation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.calculation.exception.InvalidCalculationInputException;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.domain.rule.TaxTimeBand;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaxCalculatorTest {

    private static final LocalDate DATE = LocalDate.of(2013, Month.FEBRUARY, 8);
    private static final Currency SEK = Currency.getInstance("SEK");
    private static final VehicleType VEHICLE_TYPE = new VehicleType("OTHER", "Other vehicle");
    private static final TaxAmount TAX_AMOUNT = new TaxAmount(new BigDecimal("8.00"), SEK);
    private static final TaxRuleSet TAX_RULE_SET = new TaxRuleSet(
            "gothenburg",
            LocalDate.of(2013, Month.JANUARY, 1),
            SEK,
            List.of(new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), TAX_AMOUNT)));

    private final TaxCalculator calculator = new TaxCalculator();

    @Test
    void calculatesOneTaxedPassage() {
        var passage = new Passage(
                Instant.parse("2013-02-08T05:20:27Z"), LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27));

        var result = calculator.calculate(VEHICLE_TYPE, List.of(passage), Map.of(DATE, TAX_RULE_SET));

        assertThat(result)
                .isEqualTo(new CalculationResult(VEHICLE_TYPE, List.of(new DailyTax(DATE, TAX_AMOUNT)), TAX_AMOUNT));
    }

    @Test
    void calculatesZeroOutsideTaxTimeBands() {
        var passage =
                new Passage(Instant.parse("2013-02-08T04:59:00Z"), LocalDateTime.of(2013, Month.FEBRUARY, 8, 5, 59));

        var result = calculator.calculate(VEHICLE_TYPE, List.of(passage), Map.of(DATE, TAX_RULE_SET));

        var zero = TaxAmount.zero(SEK);

        assertThat(result).isEqualTo(new CalculationResult(VEHICLE_TYPE, List.of(new DailyTax(DATE, zero)), zero));
    }

    @Test
    void rejectsEmptyPassages() {
        var passages = List.<Passage>of();
        var applicableTaxRuleSets = Map.of(DATE, TAX_RULE_SET);

        assertThatThrownBy(() -> calculator.calculate(VEHICLE_TYPE, passages, applicableTaxRuleSets))
                .isInstanceOf(InvalidCalculationInputException.class)
                .hasMessage("Passages must not be empty.");
    }

    @Test
    void rejectsEmptyApplicableTaxRuleSets() {
        var passage = new Passage(
                Instant.parse("2013-02-08T05:20:27Z"), LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27));
        var passages = List.of(passage);
        var applicableTaxRuleSets = Map.<LocalDate, TaxRuleSet>of();

        assertThatThrownBy(() -> calculator.calculate(VEHICLE_TYPE, passages, applicableTaxRuleSets))
                .isInstanceOf(InvalidCalculationInputException.class)
                .hasMessage("Applicable Tax Rule Sets must not be empty.");
    }

    @Test
    void rejectsMissingApplicableTaxRuleSetForPassageDate() {
        var passage = new Passage(
                Instant.parse("2013-02-08T05:20:27Z"), LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27));
        var passages = List.of(passage);
        var applicableTaxRuleSets = Map.of(DATE.minusDays(1), TAX_RULE_SET);

        assertThatThrownBy(() -> calculator.calculate(VEHICLE_TYPE, passages, applicableTaxRuleSets))
                .isInstanceOf(InvalidCalculationInputException.class)
                .hasMessage("Applicable Tax Rule Set is missing" + " for calculation date: 2013-02-08.");
    }

    @Test
    void rejectsNullVehicleType() {
        var passage = new Passage(
                Instant.parse("2013-02-08T05:20:27Z"), LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27));
        var passages = List.of(passage);
        var applicableTaxRuleSets = Map.of(DATE, TAX_RULE_SET);

        assertThatThrownBy(() -> calculator.calculate(null, passages, applicableTaxRuleSets))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullPassages() {
        var applicableTaxRuleSets = Map.of(DATE, TAX_RULE_SET);

        assertThatThrownBy(() -> calculator.calculate(VEHICLE_TYPE, null, applicableTaxRuleSets))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullApplicableTaxRuleSets() {
        var passage = new Passage(
                Instant.parse("2013-02-08T05:20:27Z"), LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27));
        var passages = List.of(passage);

        assertThatThrownBy(() -> calculator.calculate(VEHICLE_TYPE, passages, null))
                .isInstanceOf(NullPointerException.class);
    }
}
