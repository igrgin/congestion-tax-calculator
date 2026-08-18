package io.github.igrgin.congestiontax.taxrule.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.rule.ChargeWindow;
import io.github.igrgin.congestiontax.domain.rule.DailyMaximum;
import io.github.igrgin.congestiontax.domain.rule.MonthTaxExemption;
import io.github.igrgin.congestiontax.domain.rule.PublicHolidayPrecedingDateOption;
import io.github.igrgin.congestiontax.domain.rule.PublicHolidayTaxExemption;
import io.github.igrgin.congestiontax.domain.rule.TaxExemptions;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleOptions;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.domain.rule.TaxTimeBand;
import io.github.igrgin.congestiontax.domain.rule.VehicleTypeTaxExemption;
import io.github.igrgin.congestiontax.domain.rule.WeekdayTaxExemption;
import io.github.igrgin.congestiontax.taxrule.TaxRuleService;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidCityTimeZoneException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.OverlappingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.model.ApplicableTaxRuleSets;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("itest")
@SpringBootTest(webEnvironment = NONE)
class TaxRuleServiceITest {

    private static final Currency SEK = Currency.getInstance("SEK");

    private final List<Long> insertedCityIds = new ArrayList<>();

    @Autowired
    private TaxRuleService taxRuleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        for (var cityId : insertedCityIds) {
            jdbcTemplate.update("""
                    DELETE FROM tax_exemption
                    WHERE rule_set_id IN (
                        SELECT id
                        FROM tax_rule_set
                        WHERE city_id = ?
                    )
                    """, cityId);

            jdbcTemplate.update("""
                    DELETE FROM tax_rule_option
                    WHERE rule_set_id IN (
                        SELECT id
                        FROM tax_rule_set
                        WHERE city_id = ?
                    )
                    """, cityId);

            jdbcTemplate.update("""
                    DELETE FROM tax_time_band
                    WHERE rule_set_id IN (
                        SELECT id
                        FROM tax_rule_set
                        WHERE city_id = ?
                    )
                    """, cityId);

            jdbcTemplate.update("""
                    DELETE FROM tax_rule_set
                    WHERE city_id = ?
                    """, cityId);

            jdbcTemplate.update("""
                    DELETE FROM city
                    WHERE id = ?
                    """, cityId);
        }
    }

    @Test
    void loadsStoredVehicleType() {
        assertThat(taxRuleService.getVehicleType("OTHER")).isEqualTo(new VehicleType("OTHER", "Other vehicle"));
    }

    @Test
    void loadsApplicableTaxRuleSetsForSelectedCityAndDates() {
        var cityCode = "tax-rule-service-city";
        var cityId = insertCity(cityCode, "Europe/Stockholm");
        var otherCityId = insertCity("tax-rule-service-other-city", "Europe/Zagreb");

        var firstRuleSetId = insertTaxRuleSet(cityId, LocalDate.of(2013, Month.JANUARY, 1));
        var secondRuleSetId = insertTaxRuleSet(cityId, LocalDate.of(2013, Month.FEBRUARY, 15));
        var futureRuleSetId = insertTaxRuleSet(cityId, LocalDate.of(2013, Month.APRIL, 1));
        var otherCityRuleSetId = insertTaxRuleSet(otherCityId, LocalDate.of(2013, Month.FEBRUARY, 20));

        insertTaxTimeBand(firstRuleSetId, new BigDecimal("8.00"));
        insertTaxTimeBand(secondRuleSetId, new BigDecimal("13.00"));
        insertTaxTimeBand(futureRuleSetId, new BigDecimal("18.00"));
        insertTaxTimeBand(otherCityRuleSetId, new BigDecimal("99.00"));

        var firstCalculationDate = LocalDate.of(2013, Month.FEBRUARY, 8);
        var secondCalculationDate = LocalDate.of(2013, Month.MARCH, 1);

        var result =
                taxRuleService.getApplicableTaxRuleSets(cityCode, Set.of(firstCalculationDate, secondCalculationDate));

        assertThat(result)
                .isEqualTo(new ApplicableTaxRuleSets(
                        ZoneId.of("Europe/Stockholm"),
                        Map.of(
                                firstCalculationDate,
                                taxRuleSet(cityCode, LocalDate.of(2013, Month.JANUARY, 1), new BigDecimal("8.00")),
                                secondCalculationDate,
                                taxRuleSet(
                                        cityCode, LocalDate.of(2013, Month.FEBRUARY, 15), new BigDecimal("13.00")))));
    }

    @Test
    void loadsStoredChargeWindow() {
        var cityCode = "tax-rule-service-charge-window";
        var cityId = insertCity(cityCode, "Europe/Stockholm");
        var effectiveFrom = LocalDate.of(2013, Month.JANUARY, 1);
        var calculationDate = LocalDate.of(2013, Month.FEBRUARY, 8);
        var ruleSetId = insertTaxRuleSet(cityId, effectiveFrom);

        insertTaxTimeBand(ruleSetId, new BigDecimal("8.00"));
        insertChargeWindow(ruleSetId, 60);

        var result = taxRuleService.getApplicableTaxRuleSets(cityCode, Set.of(calculationDate));

        assertThat(result.taxRuleSetsByCalculationDate()
                        .get(calculationDate)
                        .taxRuleOptions()
                        .chargeWindow())
                .contains(new ChargeWindow(Duration.ofMinutes(60)));
    }

    @Test
    void loadsStoredDailyMaximum() {
        var cityCode = "tax-rule-service-daily-maximum";
        var cityId = insertCity(cityCode, "Europe/Stockholm");
        var effectiveFrom = LocalDate.of(2013, Month.JANUARY, 1);
        var calculationDate = LocalDate.of(2013, Month.FEBRUARY, 8);
        var ruleSetId = insertTaxRuleSet(cityId, effectiveFrom);

        insertTaxTimeBand(ruleSetId, new BigDecimal("8.00"));
        insertDailyMaximum(ruleSetId, new BigDecimal("60.00"));

        var result = taxRuleService.getApplicableTaxRuleSets(cityCode, Set.of(calculationDate));

        assertThat(result.taxRuleSetsByCalculationDate()
                        .get(calculationDate)
                        .taxRuleOptions()
                        .dailyMaximum())
                .contains(new DailyMaximum(new TaxAmount(new BigDecimal("60.00"), SEK)));
    }

    @Test
    void loadsCompleteTypedTaxExemptionsAndPublicHolidayPrecedingDateOption() {
        var cityCode = "tax-rule-service-exemptions";
        var cityId = insertCity(cityCode, "Europe/Stockholm");
        var effectiveFrom = LocalDate.of(2013, Month.JANUARY, 1);
        var calculationDate = LocalDate.of(2013, Month.FEBRUARY, 8);
        var publicHoliday = LocalDate.of(2013, Month.DECEMBER, 25);
        var ruleSetId = insertTaxRuleSet(cityId, effectiveFrom);

        insertTaxTimeBand(ruleSetId, new BigDecimal("8.00"));
        insertWeekdayTaxExemption(ruleSetId, 6);
        insertMonthTaxExemption(ruleSetId, Month.JULY.getValue());
        insertPublicHolidayTaxExemption(ruleSetId, publicHoliday);
        insertVehicleTypeTaxExemption(ruleSetId, "OTHER");
        insertPublicHolidayPrecedingDateOption(ruleSetId, 1);

        var result = taxRuleService.getApplicableTaxRuleSets(cityCode, Set.of(calculationDate));
        var expectedRuleSet = new TaxRuleSet(
                cityCode,
                effectiveFrom,
                SEK,
                List.of(new TaxTimeBand(
                        LocalTime.of(6, 0), LocalTime.of(6, 30), new TaxAmount(new BigDecimal("8.00"), SEK))),
                new TaxExemptions(List.of(
                        new WeekdayTaxExemption(DayOfWeek.SATURDAY),
                        new MonthTaxExemption(Month.JULY),
                        new PublicHolidayTaxExemption(publicHoliday),
                        new VehicleTypeTaxExemption("OTHER"))),
                new TaxRuleOptions(List.of(new PublicHolidayPrecedingDateOption(1))));

        assertThat(result)
                .isEqualTo(new ApplicableTaxRuleSets(
                        ZoneId.of("Europe/Stockholm"), Map.of(calculationDate, expectedRuleSet)));
    }

    @Test
    void rejectsStoredTaxRuleSetWithoutTaxTimeBands() {
        var cityCode = "tax-rule-service-missing-bands";
        var cityId = insertCity(cityCode, "Europe/Stockholm");
        var calculationDates = Set.of(LocalDate.of(2013, Month.FEBRUARY, 8));

        insertTaxRuleSet(cityId, LocalDate.of(2013, Month.JANUARY, 1));

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .isInstanceOf(MissingTaxTimeBandsException.class);
    }

    @Test
    void rejectsStoredOverlappingTaxTimeBands() {
        var cityCode = "tax-rule-service-overlap";
        var cityId = insertCity(cityCode, "Europe/Stockholm");
        var ruleSetId = insertTaxRuleSet(cityId, LocalDate.of(2013, Month.JANUARY, 1));
        var calculationDates = Set.of(LocalDate.of(2013, Month.FEBRUARY, 8));

        insertTaxTimeBand(ruleSetId, LocalTime.of(6, 0), LocalTime.of(6, 30), new BigDecimal("8.00"));
        insertTaxTimeBand(ruleSetId, LocalTime.of(6, 15), LocalTime.of(7, 0), new BigDecimal("13.00"));

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .isInstanceOf(OverlappingTaxTimeBandsException.class);
    }

    @Test
    void rejectsInvalidStoredCityTimeZone() {
        var cityCode = "tax-rule-service-invalid-time-zone";
        var calculationDates = Set.of(LocalDate.of(2013, Month.FEBRUARY, 8));
        insertCity(cityCode, "Mars/Olympus");

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(cityCode, calculationDates))
                .isInstanceOf(InvalidCityTimeZoneException.class);
    }

    private long insertCity(String code, String timeZone) {
        var cityId = jdbcTemplate.queryForObject("""
                INSERT INTO city (
                    code,
                    name,
                    time_zone
                )
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, code, "Tax Rule Service Test City", timeZone);

        insertedCityIds.add(cityId);

        return cityId;
    }

    private long insertTaxRuleSet(long cityId, LocalDate effectiveFrom) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO tax_rule_set (
                    city_id,
                    effective_from,
                    currency_code
                )
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, cityId, effectiveFrom, SEK.getCurrencyCode());
    }

    private void insertTaxTimeBand(long ruleSetId, BigDecimal amount) {
        insertTaxTimeBand(ruleSetId, LocalTime.of(6, 0), LocalTime.of(6, 30), amount);
    }

    private void insertTaxTimeBand(long ruleSetId, LocalTime startTime, LocalTime endTime, BigDecimal amount) {
        jdbcTemplate.update("""
                INSERT INTO tax_time_band (
                    rule_set_id,
                    start_time,
                    end_time,
                    amount
                )
                VALUES (?, ?, ?, ?)
                """, ruleSetId, startTime, endTime, amount);
    }

    private void insertChargeWindow(long ruleSetId, int durationMinutes) {
        jdbcTemplate.update("""
                INSERT INTO tax_rule_option (
                    rule_set_id,
                    type_code,
                    duration_minutes
                )
                VALUES (?, 'CHARGE_WINDOW', ?)
                """, ruleSetId, durationMinutes);
    }

    private void insertDailyMaximum(long ruleSetId, BigDecimal amount) {
        jdbcTemplate.update("""
                INSERT INTO tax_rule_option (
                    rule_set_id,
                    type_code,
                    amount
                )
                VALUES (?, 'DAILY_MAXIMUM', ?)
                """, ruleSetId, amount);
    }

    private void insertWeekdayTaxExemption(long ruleSetId, int dayOfWeek) {
        jdbcTemplate.update("""
                INSERT INTO tax_exemption (
                    rule_set_id,
                    type_code,
                    day_of_week
                )
                VALUES (?, 'WEEKDAY', ?)
                """, ruleSetId, dayOfWeek);
    }

    private void insertMonthTaxExemption(long ruleSetId, int monthNumber) {
        jdbcTemplate.update("""
                INSERT INTO tax_exemption (
                    rule_set_id,
                    type_code,
                    month_number
                )
                VALUES (?, 'MONTH', ?)
                """, ruleSetId, monthNumber);
    }

    private void insertPublicHolidayTaxExemption(long ruleSetId, LocalDate holidayDate) {
        jdbcTemplate.update("""
                INSERT INTO tax_exemption (
                    rule_set_id,
                    type_code,
                    holiday_date
                )
                VALUES (?, 'PUBLIC_HOLIDAY', ?)
                """, ruleSetId, holidayDate);
    }

    private void insertVehicleTypeTaxExemption(long ruleSetId, String vehicleTypeCode) {
        jdbcTemplate.update("""
                INSERT INTO tax_exemption (
                    rule_set_id,
                    type_code,
                    vehicle_type_code
                )
                VALUES (?, 'VEHICLE_TYPE', ?)
                """, ruleSetId, vehicleTypeCode);
    }

    private void insertPublicHolidayPrecedingDateOption(long ruleSetId, int precedingDays) {
        jdbcTemplate.update("""
                INSERT INTO tax_rule_option (
                    rule_set_id,
                    type_code,
                    preceding_days
                )
                VALUES (?, 'HOLIDAY_PRECEDING', ?)
                """, ruleSetId, precedingDays);
    }

    private static TaxRuleSet taxRuleSet(String cityCode, LocalDate effectiveFrom, BigDecimal amount) {
        return new TaxRuleSet(
                cityCode,
                effectiveFrom,
                SEK,
                List.of(new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), new TaxAmount(amount, SEK))));
    }
}
