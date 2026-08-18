package io.github.igrgin.congestiontax.domain.calculation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.calculation.exception.InvalidCalculationInputException;
import io.github.igrgin.congestiontax.domain.rule.ChargeWindow;
import io.github.igrgin.congestiontax.domain.rule.DailyMaximum;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleOptions;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.domain.rule.TaxTimeBand;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("optionalDailyRuleScenarios")
    void appliesOptionalDailyRules(
            String scenario, TaxRuleOptions taxRuleOptions, List<Passage> passages, String expectedAmount) {
        var taxRuleSet = new TaxRuleSet(
                "gothenburg",
                LocalDate.of(2013, Month.JANUARY, 1),
                SEK,
                List.of(new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(8, 0), TAX_AMOUNT)),
                taxRuleOptions);

        var result = calculator.calculate(VEHICLE_TYPE, passages, Map.of(DATE, taxRuleSet));

        var amount = new TaxAmount(new BigDecimal(expectedAmount), SEK);

        assertThat(result).isEqualTo(new CalculationResult(VEHICLE_TYPE, List.of(new DailyTax(DATE, amount)), amount));
    }

    @Test
    void groupsDailyTaxesByDateInAscendingOrder() {
        var laterDate = DATE.plusDays(1);
        var laterPassage =
                new Passage(Instant.parse("2013-02-09T05:20:00Z"), LocalDateTime.of(2013, Month.FEBRUARY, 9, 6, 20));
        var earlierPassage =
                new Passage(Instant.parse("2013-02-08T05:20:00Z"), LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20));

        var result = calculator.calculate(
                VEHICLE_TYPE,
                List.of(laterPassage, earlierPassage),
                Map.of(DATE, TAX_RULE_SET, laterDate, TAX_RULE_SET));

        var totalAmount = new TaxAmount(new BigDecimal("16.00"), SEK);

        assertThat(result)
                .isEqualTo(new CalculationResult(
                        VEHICLE_TYPE,
                        List.of(new DailyTax(DATE, TAX_AMOUNT), new DailyTax(laterDate, TAX_AMOUNT)),
                        totalAmount));
    }

    @Test
    void keepsChargeWindowsInsideEachCityLocalTimeDate() {
        var laterDate = DATE.plusDays(1);
        var laterTaxAmount = new TaxAmount(new BigDecimal("13.00"), SEK);
        var taxRuleSet = new TaxRuleSet(
                "gothenburg",
                LocalDate.of(2013, Month.JANUARY, 1),
                SEK,
                List.of(
                        new TaxTimeBand(LocalTime.of(0, 0), LocalTime.of(1, 0), laterTaxAmount),
                        new TaxTimeBand(LocalTime.of(23, 0), LocalTime.MAX, TAX_AMOUNT)),
                new TaxRuleOptions(List.of(new ChargeWindow(Duration.ofMinutes(60)))));
        var earlierPassage = new Passage(
                Instant.parse("2013-02-08T22:59:50Z"), LocalDateTime.of(2013, Month.FEBRUARY, 8, 23, 59, 50));
        var laterPassage =
                new Passage(Instant.parse("2013-02-08T23:00:10Z"), LocalDateTime.of(2013, Month.FEBRUARY, 9, 0, 0, 10));

        var result = calculator.calculate(
                VEHICLE_TYPE, List.of(earlierPassage, laterPassage), Map.of(DATE, taxRuleSet, laterDate, taxRuleSet));

        var totalAmount = new TaxAmount(new BigDecimal("21.00"), SEK);

        assertThat(result)
                .isEqualTo(new CalculationResult(
                        VEHICLE_TYPE,
                        List.of(new DailyTax(DATE, TAX_AMOUNT), new DailyTax(laterDate, laterTaxAmount)),
                        totalAmount));
    }

    @Test
    void usesHighestChargeInChargeWindowAfterInstantOrdering() {
        var higherTaxAmount = new TaxAmount(new BigDecimal("13.00"), SEK);
        var taxRuleSet = new TaxRuleSet(
                "gothenburg",
                LocalDate.of(2013, Month.JANUARY, 1),
                SEK,
                List.of(
                        new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), TAX_AMOUNT),
                        new TaxTimeBand(LocalTime.of(6, 30), LocalTime.of(7, 0), higherTaxAmount)),
                new TaxRuleOptions(List.of(new ChargeWindow(Duration.ofMinutes(60)))));
        var laterPassage =
                new Passage(Instant.parse("2013-02-08T05:45:00Z"), LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 45));
        var earlierPassage =
                new Passage(Instant.parse("2013-02-08T05:10:00Z"), LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 10));

        var result =
                calculator.calculate(VEHICLE_TYPE, List.of(laterPassage, earlierPassage), Map.of(DATE, taxRuleSet));

        assertThat(result)
                .isEqualTo(new CalculationResult(
                        VEHICLE_TYPE, List.of(new DailyTax(DATE, higherTaxAmount)), higherTaxAmount));
    }

    @ParameterizedTest
    @CsvSource({"3600, 8.00", "3601, 16.00"})
    void includesConfiguredChargeWindowBoundary(long secondsAfterWindowStart, String expectedAmount) {
        var taxRuleSet = new TaxRuleSet(
                "gothenburg",
                LocalDate.of(2013, Month.JANUARY, 1),
                SEK,
                List.of(new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(8, 0), TAX_AMOUNT)),
                new TaxRuleOptions(List.of(new ChargeWindow(Duration.ofMinutes(60)))));
        var windowStart = Instant.parse("2013-02-08T05:10:00Z");
        var firstPassage = new Passage(windowStart, LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 10));
        var secondPassage = new Passage(
                windowStart.plusSeconds(secondsAfterWindowStart),
                LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 10).plusSeconds(secondsAfterWindowStart));

        var result = calculator.calculate(VEHICLE_TYPE, List.of(firstPassage, secondPassage), Map.of(DATE, taxRuleSet));

        var amount = new TaxAmount(new BigDecimal(expectedAmount), SEK);

        assertThat(result).isEqualTo(new CalculationResult(VEHICLE_TYPE, List.of(new DailyTax(DATE, amount)), amount));
    }

    @Test
    void doesNotSlideChargeWindowFromLaterPassage() {
        var higherTaxAmount = new TaxAmount(new BigDecimal("13.00"), SEK);
        var taxRuleSet = new TaxRuleSet(
                "gothenburg",
                LocalDate.of(2013, Month.JANUARY, 1),
                SEK,
                List.of(
                        new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), TAX_AMOUNT),
                        new TaxTimeBand(LocalTime.of(6, 30), LocalTime.of(7, 0), higherTaxAmount),
                        new TaxTimeBand(LocalTime.of(7, 0), LocalTime.of(8, 0), TAX_AMOUNT)),
                new TaxRuleOptions(List.of(new ChargeWindow(Duration.ofMinutes(60)))));
        var firstPassage =
                new Passage(Instant.parse("2013-02-08T05:00:00Z"), LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 0));
        var secondPassage =
                new Passage(Instant.parse("2013-02-08T05:50:00Z"), LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 50));
        var thirdPassage =
                new Passage(Instant.parse("2013-02-08T06:40:00Z"), LocalDateTime.of(2013, Month.FEBRUARY, 8, 7, 40));

        var result = calculator.calculate(
                VEHICLE_TYPE, List.of(firstPassage, secondPassage, thirdPassage), Map.of(DATE, taxRuleSet));

        var amount = new TaxAmount(new BigDecimal("21.00"), SEK);

        assertThat(result).isEqualTo(new CalculationResult(VEHICLE_TYPE, List.of(new DailyTax(DATE, amount)), amount));
    }

    @ParameterizedTest
    @CsvSource({
        "05:59:59, 0.00",
        "06:00:00, 8.00",
        "06:29:59, 8.00",
        "06:30:00, 13.00",
        "06:59:59, 13.00",
        "07:00:00, 0.00"
    })
    void selectsTaxTimeBandAtSecondPrecision(String localTime, String expectedAmount) {
        var higherTaxAmount = new TaxAmount(new BigDecimal("13.00"), SEK);
        var taxRuleSet = new TaxRuleSet(
                "gothenburg",
                LocalDate.of(2013, Month.JANUARY, 1),
                SEK,
                List.of(
                        new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), TAX_AMOUNT),
                        new TaxTimeBand(LocalTime.of(6, 30), LocalTime.of(7, 0), higherTaxAmount)));
        var cityDateTime = LocalDateTime.of(DATE, LocalTime.parse(localTime));
        var passage = new Passage(cityDateTime.toInstant(ZoneOffset.ofHours(1)), cityDateTime);

        var result = calculator.calculate(VEHICLE_TYPE, List.of(passage), Map.of(DATE, taxRuleSet));

        var amount = new TaxAmount(new BigDecimal(expectedAmount), SEK);

        assertThat(result).isEqualTo(new CalculationResult(VEHICLE_TYPE, List.of(new DailyTax(DATE, amount)), amount));
    }

    private static Stream<Arguments> optionalDailyRuleScenarios() {
        var chargeWindow = new ChargeWindow(Duration.ofMinutes(60));
        var chargeWindowOptions = new TaxRuleOptions(List.of(chargeWindow));
        var maximumAmount = new TaxAmount(new BigDecimal("15.00"), SEK);
        var maximumOptions = new TaxRuleOptions(List.of(chargeWindow, new DailyMaximum(maximumAmount)));
        var repeatedPassage = passage("2013-02-08T05:20:00Z", 6, 20);

        return Stream.of(
                Arguments.of(
                        "Charge Window and Daily Maximum absent",
                        TaxRuleOptions.empty(),
                        List.of(passage("2013-02-08T05:10:00Z", 6, 10), passage("2013-02-08T05:20:00Z", 6, 20)),
                        "16.00"),
                Arguments.of(
                        "Charge Window present and Daily Maximum absent",
                        chargeWindowOptions,
                        List.of(passage("2013-02-08T05:10:00Z", 6, 10), passage("2013-02-08T05:20:00Z", 6, 20)),
                        "8.00"),
                Arguments.of(
                        "zero Tax Amount Passage participates",
                        chargeWindowOptions,
                        List.of(
                                passage("2013-02-08T04:30:00Z", 5, 30),
                                passage("2013-02-08T05:20:00Z", 6, 20),
                                passage("2013-02-08T06:10:00Z", 7, 10)),
                        "16.00"),
                Arguments.of(
                        "repeated Passage participates",
                        chargeWindowOptions,
                        List.of(repeatedPassage, repeatedPassage),
                        "8.00"),
                Arguments.of(
                        "Daily Maximum present after Charge Windows",
                        maximumOptions,
                        List.of(
                                passage("2013-02-08T05:10:00Z", 6, 10),
                                passage("2013-02-08T05:20:00Z", 6, 20),
                                passage("2013-02-08T06:20:00Z", 7, 20)),
                        "15.00"));
    }

    private static Passage passage(String instant, int hour, int minute) {
        return new Passage(Instant.parse(instant), LocalDateTime.of(2013, Month.FEBRUARY, 8, hour, minute));
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
    void rejectsMissingApplicableTaxRuleSetForAnyPassageDate() {
        var laterDate = DATE.plusDays(1);
        var earlierPassage =
                new Passage(Instant.parse("2013-02-08T05:20:00Z"), LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20));
        var laterPassage =
                new Passage(Instant.parse("2013-02-09T05:20:00Z"), LocalDateTime.of(2013, Month.FEBRUARY, 9, 6, 20));

        assertThatThrownBy(() -> calculator.calculate(
                        VEHICLE_TYPE, List.of(earlierPassage, laterPassage), Map.of(DATE, TAX_RULE_SET)))
                .isInstanceOf(InvalidCalculationInputException.class)
                .hasMessage("Applicable Tax Rule Set is missing" + " for calculation date: " + laterDate + ".");
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
