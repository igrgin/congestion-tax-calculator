package io.github.igrgin.congestiontax.taxrule;

import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public interface TaxRuleService {

    VehicleType getVehicleType(String vehicleTypeCode);

    Map<LocalDate, TaxRuleSet> getApplicableTaxRuleSets(String cityCode, Set<LocalDate> calculationDates);
}
