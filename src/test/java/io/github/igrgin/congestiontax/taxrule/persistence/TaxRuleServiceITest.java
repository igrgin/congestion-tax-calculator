package io.github.igrgin.congestiontax.taxrule.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.domain.rule.TaxTimeBand;
import io.github.igrgin.congestiontax.taxrule.TaxRuleService;
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.OverlappingTaxTimeBandsException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
        var cityId = insertCity(cityCode);
        var otherCityId = insertCity("tax-rule-service-other-city");

        var firstRuleSetId = insertTaxRuleSet(cityId, LocalDate.of(2013, 1, 1));
        var secondRuleSetId = insertTaxRuleSet(cityId, LocalDate.of(2013, 2, 15));
        var futureRuleSetId = insertTaxRuleSet(cityId, LocalDate.of(2013, 4, 1));
        var otherCityRuleSetId = insertTaxRuleSet(otherCityId, LocalDate.of(2013, 2, 20));

        insertTaxTimeBand(firstRuleSetId, new BigDecimal("8.00"));
        insertTaxTimeBand(secondRuleSetId, new BigDecimal("13.00"));
        insertTaxTimeBand(futureRuleSetId, new BigDecimal("18.00"));
        insertTaxTimeBand(otherCityRuleSetId, new BigDecimal("99.00"));

        var firstCalculationDate = LocalDate.of(2013, 2, 8);
        var secondCalculationDate = LocalDate.of(2013, 3, 1);

        var result =
                taxRuleService.getApplicableTaxRuleSets(cityCode, Set.of(firstCalculationDate, secondCalculationDate));

        assertThat(result)
                .isEqualTo(Map.of(
                        firstCalculationDate,
                        taxRuleSet(cityCode, LocalDate.of(2013, 1, 1), new BigDecimal("8.00")),
                        secondCalculationDate,
                        taxRuleSet(cityCode, LocalDate.of(2013, 2, 15), new BigDecimal("13.00"))));
    }

    @Test
    void rejectsStoredTaxRuleSetWithoutTaxTimeBands() {
        var cityCode = "tax-rule-service-missing-bands";
        var cityId = insertCity(cityCode);

        insertTaxRuleSet(cityId, LocalDate.of(2013, 1, 1));

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(cityCode, Set.of(LocalDate.of(2013, 2, 8))))
                .isInstanceOf(MissingTaxTimeBandsException.class);
    }

    @Test
    void rejectsStoredOverlappingTaxTimeBands() {
        var cityCode = "tax-rule-service-overlap";
        var cityId = insertCity(cityCode);
        var ruleSetId = insertTaxRuleSet(cityId, LocalDate.of(2013, 1, 1));

        insertTaxTimeBand(ruleSetId, LocalTime.of(6, 0), LocalTime.of(6, 30), new BigDecimal("8.00"));
        insertTaxTimeBand(ruleSetId, LocalTime.of(6, 15), LocalTime.of(7, 0), new BigDecimal("13.00"));

        assertThatThrownBy(() -> taxRuleService.getApplicableTaxRuleSets(cityCode, Set.of(LocalDate.of(2013, 2, 8))))
                .isInstanceOf(OverlappingTaxTimeBandsException.class);
    }

    private long insertCity(String code) {
        var cityId =
                jdbcTemplate.queryForObject("""
                INSERT INTO city (
                    code,
                    name,
                    time_zone
                )
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, code, "Tax Rule Service Test City", "Europe/Stockholm");

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

    private static TaxRuleSet taxRuleSet(String cityCode, LocalDate effectiveFrom, BigDecimal amount) {
        return new TaxRuleSet(
                cityCode,
                effectiveFrom,
                SEK,
                List.of(new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), new TaxAmount(amount, SEK))));
    }
}
