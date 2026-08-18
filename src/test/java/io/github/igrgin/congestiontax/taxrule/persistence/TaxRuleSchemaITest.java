package io.github.igrgin.congestiontax.taxrule.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.postgresql.util.PSQLException;
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
    void acceptsValidTaxExemptionRowsAndDefinesClosedTypeVocabulary() {
        long cityId = insertCity("valid-tax-exemptions");
        insertVehicleType("VALID_EXEMPT_VEHICLE");
        long ruleSetId = insertTaxRuleSet(cityId);

        insertTaxExemption(ruleSetId, "WEEKDAY", (short) 1, null, null, null);
        insertTaxExemption(ruleSetId, "MONTH", null, (short) 12, null, null);
        insertTaxExemption(ruleSetId, "PUBLIC_HOLIDAY", null, null, LocalDate.of(2020, Month.MAY, 1), null);
        insertTaxExemption(ruleSetId, "VEHICLE_TYPE", null, null, null, "VALID_EXEMPT_VEHICLE");

        List<String> typeCodes =
                jdbcTemplate.queryForList("SELECT code FROM tax_exemption_type ORDER BY code", String.class);
        Integer exemptionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tax_exemption WHERE rule_set_id = ?", Integer.class, ruleSetId);

        assertThat(typeCodes).containsExactly("MONTH", "PUBLIC_HOLIDAY", "VEHICLE_TYPE", "WEEKDAY");
        assertThat(exemptionCount).isEqualTo(4);
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(InvalidTaxExemptionShapes.class)
    void rejectsInvalidTaxExemptionValueShape(
            String scenario,
            String typeCode,
            Short dayOfWeek,
            Short monthNumber,
            LocalDate holidayDate,
            String vehicleTypeCode) {
        long cityId = insertCity("invalid-exemption-shape-" + scenario);
        insertVehicleType("SHAPE_VEHICLE");
        long ruleSetId = insertTaxRuleSet(cityId);

        assertThatThrownBy(() ->
                        insertTaxExemption(ruleSetId, typeCode, dayOfWeek, monthNumber, holidayDate, vehicleTypeCode))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(InvalidTaxExemptionRanges.class)
    void rejectsTaxExemptionValueOutsideAcceptedRange(
            String scenario, String typeCode, Short dayOfWeek, Short monthNumber) {
        long cityId = insertCity("invalid-exemption-range-" + scenario);
        long ruleSetId = insertTaxRuleSet(cityId);

        assertThatThrownBy(() -> insertTaxExemption(ruleSetId, typeCode, dayOfWeek, monthNumber, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsTaxExemptionForUnknownTaxRuleSet() {
        assertThatThrownBy(() -> insertTaxExemption(Long.MAX_VALUE, "WEEKDAY", (short) 1, null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsUnknownTaxExemptionType() {
        long cityId = insertCity("unknown-exemption-type");
        long ruleSetId = insertTaxRuleSet(cityId);

        assertThatThrownBy(() -> insertTaxExemption(ruleSetId, "UNKNOWN", null, null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsVehicleTypeTaxExemptionForUnknownVehicleType() {
        long cityId = insertCity("unknown-exempt-vehicle");
        long ruleSetId = insertTaxRuleSet(cityId);

        assertThatThrownBy(() -> insertTaxExemption(ruleSetId, "VEHICLE_TYPE", null, null, null, "UNKNOWN_VEHICLE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(DuplicateTaxExemptions.class)
    void rejectsDuplicateTypedTaxExemptionForOneTaxRuleSet(
            String scenario,
            String typeCode,
            Short dayOfWeek,
            Short monthNumber,
            LocalDate holidayDate,
            String vehicleTypeCode) {
        long cityId = insertCity("duplicate-exemption-" + scenario);
        insertVehicleType("DUPLICATE_EXEMPT_VEHICLE");
        long ruleSetId = insertTaxRuleSet(cityId);

        insertTaxExemption(ruleSetId, typeCode, dayOfWeek, monthNumber, holidayDate, vehicleTypeCode);

        assertThatThrownBy(() ->
                        insertTaxExemption(ruleSetId, typeCode, dayOfWeek, monthNumber, holidayDate, vehicleTypeCode))
                .isInstanceOf(DataIntegrityViolationException.class);
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
    void acceptsFullDayTaxTimeBandWithEqualBoundaries() {
        long cityId = insertCity("equal-band-boundaries");
        long ruleSetId = insertTaxRuleSet(cityId);
        LocalTime boundary = LocalTime.of(6, 0);

        insertTaxTimeBand(ruleSetId, boundary, boundary, new BigDecimal("8.00"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tax_time_band WHERE rule_set_id = ?", Integer.class, ruleSetId);

        assertThat(count).isEqualTo(1);
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("overlappingTaxTimeBands")
    void rejectsEveryTaxTimeBandOverlapShape(
            String scenario, LocalTime firstStart, LocalTime firstEnd, LocalTime secondStart, LocalTime secondEnd) {
        long cityId = insertCity("overlap-" + scenario);
        long ruleSetId = insertTaxRuleSet(cityId);

        assertThatThrownBy(() -> {
                    insertTaxTimeBand(ruleSetId, firstStart, firstEnd, new BigDecimal("8.00"));
                    insertTaxTimeBand(ruleSetId, secondStart, secondEnd, new BigDecimal("13.00"));
                })
                .isInstanceOfSatisfying(
                        DataIntegrityViolationException.class, exception -> assertThat(exception.getRootCause())
                                .isInstanceOfSatisfying(PSQLException.class, cause -> assertThat(cause.getSQLState())
                                        .isEqualTo("23P01")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonOverlappingTaxTimeBands")
    void acceptsNonOverlappingTaxTimeBands(
            String scenario, LocalTime firstStart, LocalTime firstEnd, LocalTime secondStart, LocalTime secondEnd) {
        long cityId = insertCity("non-overlap-" + scenario);
        long ruleSetId = insertTaxRuleSet(cityId);

        insertTaxTimeBand(ruleSetId, firstStart, firstEnd, new BigDecimal("8.00"));
        insertTaxTimeBand(ruleSetId, secondStart, secondEnd, new BigDecimal("13.00"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tax_time_band WHERE rule_set_id = ?", Integer.class, ruleSetId);

        assertThat(count).isEqualTo(2);
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

    private void insertTaxExemption(
            long ruleSetId,
            String typeCode,
            Short dayOfWeek,
            Short monthNumber,
            LocalDate holidayDate,
            String vehicleTypeCode) {
        jdbcTemplate.update("""
                INSERT INTO tax_exemption (
                    rule_set_id,
                    type_code,
                    day_of_week,
                    month_number,
                    holiday_date,
                    vehicle_type_code
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """, ruleSetId, typeCode, dayOfWeek, monthNumber, holidayDate, vehicleTypeCode);
    }

    private static Stream<Arguments> overlappingTaxTimeBands() {
        return Stream.of(
                Arguments.of(
                        "partial-same-date",
                        LocalTime.of(6, 0),
                        LocalTime.of(8, 0),
                        LocalTime.of(7, 0),
                        LocalTime.of(9, 0)),
                Arguments.of("nested", LocalTime.of(6, 0), LocalTime.of(9, 0), LocalTime.of(7, 0), LocalTime.of(8, 0)),
                Arguments.of(
                        "cross-midnight",
                        LocalTime.of(18, 30),
                        LocalTime.of(6, 0),
                        LocalTime.of(5, 0),
                        LocalTime.of(7, 0)),
                Arguments.of(
                        "full-day-and-another-band",
                        LocalTime.of(7, 0),
                        LocalTime.of(8, 0),
                        LocalTime.of(6, 0),
                        LocalTime.of(6, 0)),
                Arguments.of(
                        "two-full-day-bands",
                        LocalTime.of(6, 0),
                        LocalTime.of(6, 0),
                        LocalTime.of(12, 0),
                        LocalTime.of(12, 0)));
    }

    private static Stream<Arguments> nonOverlappingTaxTimeBands() {
        return Stream.of(
                Arguments.of(
                        "adjacent", LocalTime.of(6, 0), LocalTime.of(7, 0), LocalTime.of(7, 0), LocalTime.of(8, 0)),
                Arguments.of("gap", LocalTime.of(6, 0), LocalTime.of(7, 0), LocalTime.of(8, 0), LocalTime.of(9, 0)));
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

    private static final class InvalidTaxExemptionShapes implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(
                ParameterDeclarations parameters, ExtensionContext context) {
            return Stream.of(
                    Arguments.of("weekday-without-day", "WEEKDAY", null, null, null, null),
                    Arguments.of("month-with-weekday", "MONTH", (short) 1, null, null, null),
                    Arguments.of(
                            "public-holiday-with-month",
                            "PUBLIC_HOLIDAY",
                            null,
                            (short) 5,
                            LocalDate.of(2020, Month.MAY, 1),
                            null),
                    Arguments.of("vehicle-type-without-code", "VEHICLE_TYPE", null, null, null, null));
        }
    }

    private static final class InvalidTaxExemptionRanges implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(
                ParameterDeclarations parameters, ExtensionContext context) {
            return Stream.of(
                    Arguments.of("weekday-zero", "WEEKDAY", (short) 0, null),
                    Arguments.of("weekday-eight", "WEEKDAY", (short) 8, null),
                    Arguments.of("month-zero", "MONTH", null, (short) 0),
                    Arguments.of("month-thirteen", "MONTH", null, (short) 13));
        }
    }

    private static final class DuplicateTaxExemptions implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(
                ParameterDeclarations parameters, ExtensionContext context) {
            return Stream.of(
                    Arguments.of("weekday", "WEEKDAY", (short) 5, null, null, null),
                    Arguments.of("month", "MONTH", null, (short) 6, null, null),
                    Arguments.of(
                            "public-holiday", "PUBLIC_HOLIDAY", null, null, LocalDate.of(2020, Month.JUNE, 19), null),
                    Arguments.of("vehicle-type", "VEHICLE_TYPE", null, null, null, "DUPLICATE_EXEMPT_VEHICLE"));
        }
    }
}
