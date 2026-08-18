package io.github.igrgin.congestiontax.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.calculation.TaxExemptionReason;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TaxRuleSetTest {

    private static final Currency CURRENCY = Currency.getInstance("SEK");
    private static final TaxTimeBand TAX_TIME_BAND =
            new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), new TaxAmount(new BigDecimal("8.00"), CURRENCY));
    private static final VehicleType OTHER = new VehicleType("OTHER", "Other");

    @Test
    void exposesOnlyCurrentCityTaxRuleFields() {
        assertThat(Arrays.stream(TaxRuleSet.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("cityCode", "currency", "taxTimeBands", "taxExemptions", "taxRuleOptions");
    }

    @Test
    void reportsEveryApplicableReasonInDeclarationOrder() {
        var date = LocalDate.of(2013, Month.JULY, 1);
        var taxExemptions = new TaxExemptions(List.of(
                new PublicHolidayTaxExemption(date.plusDays(1)),
                new MonthTaxExemption(Month.JULY),
                new VehicleTypeTaxExemption("BUS"),
                new PublicHolidayTaxExemption(date),
                new WeekdayTaxExemption(DayOfWeek.MONDAY)));
        var ruleSet = ruleSet(taxExemptions, new TaxRuleOptions(List.of(new PublicHolidayPrecedingDateOption(1))));

        var reasons = ruleSet.taxExemptionReasonsFor(new VehicleType("BUS", "Bus"), date);

        assertThat(reasons)
                .containsExactly(
                        TaxExemptionReason.VEHICLE_TYPE,
                        TaxExemptionReason.WEEKDAY,
                        TaxExemptionReason.MONTH,
                        TaxExemptionReason.PUBLIC_HOLIDAY,
                        TaxExemptionReason.DATE_BEFORE_PUBLIC_HOLIDAY);
        assertThatThrownBy(() -> reasons.add(TaxExemptionReason.WEEKDAY))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("publicHolidayPrecedingDateOptionBoundaries")
    void appliesPublicHolidayPrecedingDateOptionOnlyBeforeThePublicHoliday(
            String scenario, LocalDate date, Set<TaxExemptionReason> expectedReasons) {
        var publicHoliday = LocalDate.of(2013, Month.MAY, 3);
        var ruleSet = ruleSet(
                new TaxExemptions(List.of(new PublicHolidayTaxExemption(publicHoliday))),
                new TaxRuleOptions(List.of(new PublicHolidayPrecedingDateOption(2))));

        assertThat(ruleSet.taxExemptionReasonsFor(OTHER, date)).containsExactlyElementsOf(expectedReasons);
    }

    @Test
    void doesNotApplyPublicHolidayPrecedingDateOptionWhenOptionIsAbsent() {
        var publicHoliday = LocalDate.of(2013, Month.MAY, 3);
        var ruleSet = ruleSet(
                new TaxExemptions(List.of(new PublicHolidayTaxExemption(publicHoliday))), TaxRuleOptions.empty());

        assertThat(ruleSet.taxExemptionReasonsFor(OTHER, publicHoliday.minusDays(1)))
                .isEmpty();
    }

    @Test
    void doesNotApplyPublicHolidayPrecedingDateOptionWithoutPublicHolidayTaxExemptions() {
        var ruleSet =
                ruleSet(TaxExemptions.empty(), new TaxRuleOptions(List.of(new PublicHolidayPrecedingDateOption(1))));

        assertThat(ruleSet.taxExemptionReasonsFor(OTHER, LocalDate.of(2013, Month.MAY, 2)))
                .isEmpty();
    }

    @Test
    void reportsPublicHolidayAndPrecedingDateReasonsForConsecutiveHolidays() {
        var date = LocalDate.of(2013, Month.MAY, 1);
        var ruleSet = ruleSet(
                new TaxExemptions(
                        List.of(new PublicHolidayTaxExemption(date), new PublicHolidayTaxExemption(date.plusDays(1)))),
                new TaxRuleOptions(List.of(new PublicHolidayPrecedingDateOption(1))));

        assertThat(ruleSet.taxExemptionReasonsFor(OTHER, date))
                .containsExactly(TaxExemptionReason.PUBLIC_HOLIDAY, TaxExemptionReason.DATE_BEFORE_PUBLIC_HOLIDAY);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("individualTaxExemptions")
    void reportsReasonForMatchingTaxExemption(
            String scenario,
            TaxExemption taxExemption,
            VehicleType vehicleType,
            LocalDate date,
            TaxExemptionReason reason) {
        var ruleSet = ruleSet(new TaxExemptions(List.of(taxExemption)), TaxRuleOptions.empty());

        assertThat(ruleSet.taxExemptionReasonsFor(vehicleType, date)).containsExactly(reason);
    }

    @Test
    void returnsEmptySetWhenTaxExemptionsDoNotMatch() {
        var ruleSet = ruleSet(
                new TaxExemptions(List.of(
                        new VehicleTypeTaxExemption("BUS"),
                        new WeekdayTaxExemption(DayOfWeek.SATURDAY),
                        new MonthTaxExemption(Month.JULY),
                        new PublicHolidayTaxExemption(LocalDate.of(2013, Month.DECEMBER, 25)))),
                TaxRuleOptions.empty());

        assertThat(ruleSet.taxExemptionReasonsFor(OTHER, LocalDate.of(2013, Month.MAY, 6)))
                .isEmpty();
    }

    @Test
    void rejectsNullTaxExemptionReasonInputs() {
        var ruleSet = ruleSet(TaxExemptions.empty(), TaxRuleOptions.empty());

        assertThatNullPointerException()
                .isThrownBy(() -> ruleSet.taxExemptionReasonsFor(null, LocalDate.of(2013, Month.MAY, 6)));
        assertThatNullPointerException().isThrownBy(() -> ruleSet.taxExemptionReasonsFor(OTHER, null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullValues")
    void rejectsNullValues(
            String scenario,
            String cityCode,
            Currency currency,
            List<TaxTimeBand> taxTimeBands,
            TaxExemptions taxExemptions,
            TaxRuleOptions taxRuleOptions) {
        assertThatNullPointerException()
                .isThrownBy(() -> new TaxRuleSet(cityCode, currency, taxTimeBands, taxExemptions, taxRuleOptions));
    }

    @Test
    void rejectsEmptyTaxTimeBands() {
        assertThatThrownBy(() -> new TaxRuleSet("gothenburg", CURRENCY, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> nullValues() {
        return Stream.of(
                Arguments.of(
                        "null City code",
                        null,
                        CURRENCY,
                        List.of(TAX_TIME_BAND),
                        TaxExemptions.empty(),
                        TaxRuleOptions.empty()),
                Arguments.of(
                        "null currency",
                        "gothenburg",
                        null,
                        List.of(TAX_TIME_BAND),
                        TaxExemptions.empty(),
                        TaxRuleOptions.empty()),
                Arguments.of(
                        "null Tax Time Bands",
                        "gothenburg",
                        CURRENCY,
                        null,
                        TaxExemptions.empty(),
                        TaxRuleOptions.empty()),
                Arguments.of(
                        "null Tax Exemptions",
                        "gothenburg",
                        CURRENCY,
                        List.of(TAX_TIME_BAND),
                        null,
                        TaxRuleOptions.empty()),
                Arguments.of(
                        "null Tax Rule Options",
                        "gothenburg",
                        CURRENCY,
                        List.of(TAX_TIME_BAND),
                        TaxExemptions.empty(),
                        null));
    }

    private static Stream<Arguments> publicHolidayPrecedingDateOptionBoundaries() {
        return Stream.of(
                Arguments.of("before range", LocalDate.of(2013, Month.APRIL, 30), Set.<TaxExemptionReason>of()),
                Arguments.of(
                        "first date",
                        LocalDate.of(2013, Month.MAY, 1),
                        Set.of(TaxExemptionReason.DATE_BEFORE_PUBLIC_HOLIDAY)),
                Arguments.of(
                        "last date",
                        LocalDate.of(2013, Month.MAY, 2),
                        Set.of(TaxExemptionReason.DATE_BEFORE_PUBLIC_HOLIDAY)),
                Arguments.of(
                        "public holiday", LocalDate.of(2013, Month.MAY, 3), Set.of(TaxExemptionReason.PUBLIC_HOLIDAY)),
                Arguments.of("after range", LocalDate.of(2013, Month.MAY, 4), Set.<TaxExemptionReason>of()));
    }

    private static Stream<Arguments> individualTaxExemptions() {
        return Stream.of(
                Arguments.of(
                        "Vehicle Type",
                        new VehicleTypeTaxExemption("BUS"),
                        new VehicleType("BUS", "Bus"),
                        LocalDate.of(2013, Month.MAY, 6),
                        TaxExemptionReason.VEHICLE_TYPE),
                Arguments.of(
                        "weekday",
                        new WeekdayTaxExemption(DayOfWeek.MONDAY),
                        OTHER,
                        LocalDate.of(2013, Month.MAY, 6),
                        TaxExemptionReason.WEEKDAY),
                Arguments.of(
                        "month",
                        new MonthTaxExemption(Month.MAY),
                        OTHER,
                        LocalDate.of(2013, Month.MAY, 6),
                        TaxExemptionReason.MONTH),
                Arguments.of(
                        "public holiday",
                        new PublicHolidayTaxExemption(LocalDate.of(2013, Month.MAY, 6)),
                        OTHER,
                        LocalDate.of(2013, Month.MAY, 6),
                        TaxExemptionReason.PUBLIC_HOLIDAY));
    }

    private static TaxRuleSet ruleSet(TaxExemptions taxExemptions, TaxRuleOptions taxRuleOptions) {
        return new TaxRuleSet("gothenburg", CURRENCY, List.of(TAX_TIME_BAND), taxExemptions, taxRuleOptions);
    }
}
