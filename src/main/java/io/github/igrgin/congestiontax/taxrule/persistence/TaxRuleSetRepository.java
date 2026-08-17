package io.github.igrgin.congestiontax.taxrule.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface TaxRuleSetRepository extends Repository<TaxRuleSetEntity, Long> {

    @Query(value = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM city
                        WHERE city.code = :cityCode
                    )
                    """, nativeQuery = true)
    public boolean cityExists(@Param("cityCode") String cityCode);

    @Query(value = """
                    SELECT tax_rule_set.*
                    FROM tax_rule_set
                    JOIN city ON city.id = tax_rule_set.city_id
                    WHERE city.code = :cityCode
                      AND tax_rule_set.effective_from <= :latestCalculationDate
                    ORDER BY tax_rule_set.effective_from DESC
                    """, nativeQuery = true)
    public List<TaxRuleSetEntity> findApplicableCandidates(
            @Param("cityCode") String cityCode, @Param("latestCalculationDate") LocalDate latestCalculationDate);
}
