package io.github.igrgin.congestiontax.taxrule.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.rule.ChargeWindow;
import io.github.igrgin.congestiontax.domain.rule.DailyMaximum;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.domain.rule.TaxTimeBand;
import io.github.igrgin.congestiontax.taxrule.TaxRuleService;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidCityTimeZoneException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.OverlappingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.model.CityTaxRuleSet;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
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
    void loadsTaxRuleSetForSelectedCity() {
        var cityCode = "tax-rule-service-city";
        var cityId = insertCity(cityCode, "Europe/Stockholm");
        var otherCityId = insertCity("tax-rule-service-other-city", "Europe/Zagreb");

        var ruleSetId = insertTaxRuleSet(cityId);
        var otherCityRuleSetId = insertTaxRuleSet(otherCityId);

        insertTaxTimeBand(ruleSetId, new BigDecimal("8.00"));
        insertTaxTimeBand(otherCityRuleSetId, new BigDecimal("99.00"));

        var result = taxRuleService.getCityTaxRuleSet(cityCode);

        assertThat(result)
                .isEqualTo(new CityTaxRuleSet(
                        ZoneId.of("Europe/Stockholm"), taxRuleSet(cityCode, new BigDecimal("8.00"))));
    }

    @Test
    void loadsStoredChargeWindow() {
        var cityCode = "tax-rule-service-charge-window";
        var cityId = insertCity(cityCode, "Europe/Stockholm");
        var ruleSetId = insertTaxRuleSet(cityId);

        insertTaxTimeBand(ruleSetId, new BigDecimal("8.00"));
        insertChargeWindow(ruleSetId, 60);

        var result = taxRuleService.getCityTaxRuleSet(cityCode);

        assertThat(result.taxRuleSet().taxRuleOptions().chargeWindow())
                .contains(new ChargeWindow(Duration.ofMinutes(60)));
    }

    @Test
    void loadsStoredDailyMaximum() {
        var cityCode = "tax-rule-service-daily-maximum";
        var cityId = insertCity(cityCode, "Europe/Stockholm");
        var ruleSetId = insertTaxRuleSet(cityId);

        insertTaxTimeBand(ruleSetId, new BigDecimal("8.00"));
        insertDailyMaximum(ruleSetId, new BigDecimal("60.00"));

        var result = taxRuleService.getCityTaxRuleSet(cityCode);

        assertThat(result.taxRuleSet().taxRuleOptions().dailyMaximum())
                .contains(new DailyMaximum(new TaxAmount(new BigDecimal("60.00"), SEK)));
    }

    @Test
    void rejectsStoredTaxRuleSetWithoutTaxTimeBands() {
        var cityCode = "tax-rule-service-missing-bands";
        var cityId = insertCity(cityCode, "Europe/Stockholm");
        insertTaxRuleSet(cityId);

        assertThatThrownBy(() -> taxRuleService.getCityTaxRuleSet(cityCode))
                .isInstanceOf(MissingTaxTimeBandsException.class);
    }

    @Test
    void rejectsStoredOverlappingTaxTimeBands() {
        var cityCode = "tax-rule-service-overlap";
        var cityId = insertCity(cityCode, "Europe/Stockholm");
        var ruleSetId = insertTaxRuleSet(cityId);

        insertTaxTimeBand(ruleSetId, LocalTime.of(6, 0), LocalTime.of(6, 30), new BigDecimal("8.00"));
        insertTaxTimeBand(ruleSetId, LocalTime.of(6, 15), LocalTime.of(7, 0), new BigDecimal("13.00"));

        assertThatThrownBy(() -> taxRuleService.getCityTaxRuleSet(cityCode))
                .isInstanceOf(OverlappingTaxTimeBandsException.class);
    }

    @Test
    void rejectsInvalidStoredCityTimeZone() {
        var cityCode = "tax-rule-service-invalid-time-zone";
        insertCity(cityCode, "Mars/Olympus");

        assertThatThrownBy(() -> taxRuleService.getCityTaxRuleSet(cityCode))
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

    private long insertTaxRuleSet(long cityId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO tax_rule_set (
                    city_id,
                    currency_code
                )
                VALUES (?, ?)
                RETURNING id
                """, Long.class, cityId, SEK.getCurrencyCode());
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

    private static TaxRuleSet taxRuleSet(String cityCode, BigDecimal amount) {
        return new TaxRuleSet(
                cityCode,
                SEK,
                List.of(new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), new TaxAmount(amount, SEK))));
    }
}
