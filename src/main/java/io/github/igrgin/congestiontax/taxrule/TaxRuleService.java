package io.github.igrgin.congestiontax.taxrule;

import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public interface TaxRuleService {

    public VehicleType getVehicleType(String vehicleTypeCode);

    public Map<LocalDate, TaxRuleSet> getApplicableTaxRuleSets(String cityCode, Set<LocalDate> calculationDates);
}
