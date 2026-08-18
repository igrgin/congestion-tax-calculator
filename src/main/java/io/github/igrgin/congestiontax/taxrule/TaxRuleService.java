package io.github.igrgin.congestiontax.taxrule;

import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.taxrule.model.CityTaxRuleSet;

public interface TaxRuleService {

    VehicleType getVehicleType(String vehicleTypeCode);

    CityTaxRuleSet getCityTaxRuleSet(String cityCode);
}
