package io.github.igrgin.congestiontax.taxrule.persistence;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface TaxRuleOptionRepository extends Repository<TaxRuleOptionEntity, Long> {

    List<TaxRuleOptionEntity> findByRuleSetId(Long ruleSetId);
}
