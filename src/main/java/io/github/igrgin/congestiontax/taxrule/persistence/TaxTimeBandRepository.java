package io.github.igrgin.congestiontax.taxrule.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.repository.Repository;

interface TaxTimeBandRepository extends Repository<TaxTimeBandEntity, Long> {

    public List<TaxTimeBandEntity> findByRuleSetIdIn(Collection<Long> ruleSetIds);
}
