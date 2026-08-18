package io.github.igrgin.congestiontax.taxrule.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface TaxRuleSetRepository extends Repository<TaxRuleSetEntity, Long> {

    @Query(value = """
                    SELECT tax_rule_set.*
                    FROM tax_rule_set
                    JOIN city ON city.id = tax_rule_set.city_id
                    WHERE city.code = :cityCode
                    """, nativeQuery = true)
    Optional<TaxRuleSetEntity> findByCityCode(@Param("cityCode") String cityCode);
}
