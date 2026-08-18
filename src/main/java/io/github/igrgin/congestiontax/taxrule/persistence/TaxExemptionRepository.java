package io.github.igrgin.congestiontax.taxrule.persistence;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface TaxExemptionRepository extends Repository<TaxExemptionEntity, Long> {

    List<TaxExemptionEntity> findByRuleSetId(Long ruleSetId);
}
