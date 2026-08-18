package io.github.igrgin.congestiontax.taxrule.persistence;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface TaxTimeBandRepository extends Repository<TaxTimeBandEntity, Long> {

    List<TaxTimeBandEntity> findByRuleSetId(Long ruleSetId);
}
