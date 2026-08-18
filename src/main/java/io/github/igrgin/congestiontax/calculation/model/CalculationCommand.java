package io.github.igrgin.congestiontax.calculation.model;

import java.util.List;

public record CalculationCommand(String cityCode, String vehicleTypeCode, List<String> passageTimestamps) {

    public CalculationCommand {
        passageTimestamps = List.copyOf(passageTimestamps);
    }
}
