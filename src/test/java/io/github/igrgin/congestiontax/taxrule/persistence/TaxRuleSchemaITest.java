package io.github.igrgin.congestiontax.taxrule.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("itest")
@SpringBootTest(webEnvironment = NONE)
@Transactional
class TaxRuleSchemaITest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void acceptsCityWithTimeZone() {
        long cityId = insertCity("city-with-time-zone", "Europe/Stockholm");

        assertThat(cityId).isPositive();
    }

    @Test
    void rejectsCityWithoutTimeZone() {
        assertThatThrownBy(() -> insertCityWithoutTimeZone("city-without-time-zone"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsBlankCityTimeZone() {
        assertThatThrownBy(() -> insertCity("city-with-blank-time-zone", " "))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void acceptsValidTaxRuleRows() {
        long cityId = insertCity("schema-city");
        insertVehicleType("SCHEMA_VEHICLE");

        long ruleSetId = insertTaxRuleSet(cityId);

        insertTaxTimeBand(ruleSetId, LocalTime.of(6, 0), LocalTime.of(6, 30), new BigDecimal("8.00"));

        insertTaxRuleOption(ruleSetId, "DAILY_MAXIMUM", new BigDecimal("60.00"), null, null);

        Integer timeBandCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tax_time_band WHERE rule_set_id = ?", Integer.class, ruleSetId);

        Integer optionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tax_rule_option WHERE rule_set_id = ?", Integer.class, ruleSetId);

        assertThat(timeBandCount).isEqualTo(1);
        assertThat(optionCount).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateCityCode() {
        insertCity("duplicate-city");

        assertThatThrownBy(() -> insertCity("duplicate-city")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsTaxRuleSetForUnknownCity() {
        assertThatThrownBy(() -> insertTaxRuleSet(Long.MAX_VALUE)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsSecondTaxRuleSetForSameCity() {
        long cityId = insertCity("duplicate-city-rule-set");

        insertTaxRuleSet(cityId);

        assertThatThrownBy(() -> insertTaxRuleSet(cityId)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsTaxTimeBandForUnknownTaxRuleSet() {
        LocalTime startTime = LocalTime.of(6, 0);
        LocalTime endTime = LocalTime.of(6, 30);
        BigDecimal amount = new BigDecimal("8.00");

        assertThatThrownBy(() -> insertTaxTimeBand(Long.MAX_VALUE, startTime, endTime, amount))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNegativeTaxTimeBandAmount() {
        long cityId = insertCity("negative-band-amount");
        long ruleSetId = insertTaxRuleSet(cityId);
        LocalTime startTime = LocalTime.of(6, 0);
        LocalTime endTime = LocalTime.of(6, 30);

        assertThatThrownBy(() -> insertTaxTimeBand(ruleSetId, startTime, endTime, new BigDecimal("-1.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void acceptsZeroTaxTimeBandAmount() {
        long cityId = insertCity("zero-band-amount");
        long ruleSetId = insertTaxRuleSet(cityId);

        insertTaxTimeBand(ruleSetId, LocalTime.of(6, 0), LocalTime.of(6, 30), BigDecimal.ZERO);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tax_time_band WHERE rule_set_id = ?", Integer.class, ruleSetId);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void rejectsTaxTimeBandWithEqualBoundaries() {
        long cityId = insertCity("equal-band-boundaries");
        long ruleSetId = insertTaxRuleSet(cityId);
        LocalTime boundary = LocalTime.of(6, 0);

        assertThatThrownBy(() -> insertTaxTimeBand(ruleSetId, boundary, boundary, new BigDecimal("8.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void acceptsCrossMidnightTaxTimeBand() {
        long cityId = insertCity("cross-midnight-band");
        long ruleSetId = insertTaxRuleSet(cityId);

        insertTaxTimeBand(ruleSetId, LocalTime.of(18, 30), LocalTime.of(6, 0), new BigDecimal("8.00"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tax_time_band WHERE rule_set_id = ?", Integer.class, ruleSetId);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateTaxTimeBandForOneTaxRuleSet() {
        long cityId = insertCity("duplicate-time-band");
        long ruleSetId = insertTaxRuleSet(cityId);
        LocalTime startTime = LocalTime.of(6, 0);
        LocalTime endTime = LocalTime.of(6, 30);
        BigDecimal duplicateAmount = new BigDecimal("13.00");

        insertTaxTimeBand(ruleSetId, startTime, endTime, new BigDecimal("8.00"));

        assertThatThrownBy(() -> insertTaxTimeBand(ruleSetId, startTime, endTime, duplicateAmount))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsTaxRuleOptionForUnknownTaxRuleSet() {
        BigDecimal amount = new BigDecimal("60.00");

        assertThatThrownBy(() -> insertTaxRuleOption(Long.MAX_VALUE, "DAILY_MAXIMUM", amount, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateTaxRuleOptionTypeForOneTaxRuleSet() {
        long cityId = insertCity("duplicate-option");
        long ruleSetId = insertTaxRuleSet(cityId);
        BigDecimal duplicateAmount = new BigDecimal("70.00");

        insertTaxRuleOption(ruleSetId, "DAILY_MAXIMUM", new BigDecimal("60.00"), null, null);

        assertThatThrownBy(() -> insertTaxRuleOption(ruleSetId, "DAILY_MAXIMUM", duplicateAmount, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(InvalidTaxRuleOptions.class)
    void rejectsInvalidTaxRuleOptionValue(
            String scenario, String typeCode, BigDecimal amount, Integer durationMinutes, Short precedingDays) {
        long cityId = insertCity("invalid-option-" + scenario);
        long ruleSetId = insertTaxRuleSet(cityId);

        assertThatThrownBy(() -> insertTaxRuleOption(ruleSetId, typeCode, amount, durationMinutes, precedingDays))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long insertCity(String code) {
        return insertCity(code, "Europe/Stockholm");
    }

    private long insertCityWithoutTimeZone(String code) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO city (code, name)
                VALUES (?, ?)
                RETURNING id
                """, Long.class, code, "Schema Test City");
    }

    private long insertCity(String code, String timeZone) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO city (code, name, time_zone)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, code, "Schema Test City", timeZone);
    }

    private void insertVehicleType(String code) {
        jdbcTemplate.update("""
                INSERT INTO vehicle_type (code, description)
                VALUES (?, ?)
                """, code, "Schema test Vehicle Type");
    }

    private long insertTaxRuleSet(long cityId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO tax_rule_set (city_id, currency_code)
                VALUES (?, ?)
                RETURNING id
                """, Long.class, cityId, "EUR");
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

    private void insertTaxRuleOption(
            long ruleSetId, String typeCode, BigDecimal amount, Integer durationMinutes, Short precedingDays) {
        jdbcTemplate.update("""
                INSERT INTO tax_rule_option (
                    rule_set_id,
                    type_code,
                    amount,
                    duration_minutes,
                    preceding_days
                )
                VALUES (?, ?, ?, ?, ?)
                """, ruleSetId, typeCode, amount, durationMinutes, precedingDays);
    }

    private static final class InvalidTaxRuleOptions implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(
                ParameterDeclarations parameters, ExtensionContext context) {
            return Stream.of(
                    Arguments.of("daily-maximum-without-amount", "DAILY_MAXIMUM", null, null, null),
                    Arguments.of("daily-maximum-with-duration", "DAILY_MAXIMUM", new BigDecimal("60.00"), 60, null),
                    Arguments.of("non-positive-daily-maximum", "DAILY_MAXIMUM", new BigDecimal("0.00"), null, null),
                    Arguments.of("non-positive-charge-window", "CHARGE_WINDOW", null, 0, null),
                    Arguments.of("non-positive-holiday-preceding", "HOLIDAY_PRECEDING", null, null, (short) 0),
                    Arguments.of("unknown-option-type", "UNKNOWN", new BigDecimal("60.00"), null, null));
        }
    }
}
