package io.github.igrgin.congestiontax.taxrule;

import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.taxrule.model.ApplicableTaxRuleSets;
import java.time.LocalDate;
import java.util.Set;

public interface TaxRuleService {

    VehicleType getVehicleType(String vehicleTypeCode);

    ApplicableTaxRuleSets getApplicableTaxRuleSets(String cityCode, Set<LocalDate> calculationDates);
}
