package io.github.igrgin.congestiontax.domain.rule;

import lombok.NonNull;

public record VehicleTypeTaxExemption(@NonNull String vehicleTypeCode) implements TaxExemption {

    public VehicleTypeTaxExemption {
        if (vehicleTypeCode.isBlank()) {
            throw new IllegalArgumentException("Vehicle Type code must not be blank.");
        }
    }
}
