package io.github.igrgin.congestiontax.calculation;

import io.github.igrgin.congestiontax.domain.calculation.Passage;
import java.util.List;

public record CalculationCommand(String cityCode, String vehicleTypeCode, List<Passage> passages) {}
