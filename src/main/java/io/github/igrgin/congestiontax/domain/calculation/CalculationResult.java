package io.github.igrgin.congestiontax.domain.calculation;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import java.util.List;

public record CalculationResult(VehicleType vehicleType, List<DailyTax> dailyTaxes, TaxAmount totalAmount) {}
