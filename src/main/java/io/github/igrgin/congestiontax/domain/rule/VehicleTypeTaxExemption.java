package io.github.igrgin.congestiontax.domain.rule;

import lombok.NonNull;

public record VehicleTypeTaxExemption(@NonNull String vehicleTypeCode) implements TaxExemption {}
