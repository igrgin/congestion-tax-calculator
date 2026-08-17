package io.github.igrgin.congestiontax.calculation.model;

import io.github.igrgin.congestiontax.domain.calculation.Passage;
import java.util.List;

public record CalculationCommand(String cityCode, String vehicleTypeCode, List<Passage> passages) {}
