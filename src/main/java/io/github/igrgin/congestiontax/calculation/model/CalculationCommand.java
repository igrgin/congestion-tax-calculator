package io.github.igrgin.congestiontax.calculation.model;

import java.time.LocalDateTime;
import java.util.List;

public record CalculationCommand(String cityCode, String vehicleTypeCode, List<LocalDateTime> passageCityDateTimes) {}
