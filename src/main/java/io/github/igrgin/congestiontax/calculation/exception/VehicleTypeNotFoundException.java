package io.github.igrgin.congestiontax.calculation.exception;

import java.util.NoSuchElementException;

public final class VehicleTypeNotFoundException extends NoSuchElementException {

    private final String vehicleTypeCode;

    public VehicleTypeNotFoundException(String vehicleTypeCode, Throwable cause) {
        super("Vehicle Type does not exist: " + vehicleTypeCode + ".", cause);
        this.vehicleTypeCode = vehicleTypeCode;
    }

    public String vehicleTypeCode() {
        return vehicleTypeCode;
    }
}
