package io.github.igrgin.congestiontax.calculation;

import java.time.Instant;
import java.util.List;

public record CalculationCommand(String cityCode, String vehicleTypeCode, List<Instant> passageInstants) {}
