package io.github.igrgin.congestiontax.taxrule.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface TaxRuleOptionRepository extends Repository<TaxRuleOptionEntity, Long> {

    List<TaxRuleOptionEntity> findByRuleSetIdIn(Collection<Long> ruleSetIds);
}
