package io.github.igrgin.congestiontax.taxrule.exception;

public final class UnknownVehicleTypeException extends IllegalArgumentException {

    public UnknownVehicleTypeException(String vehicleTypeCode) {
        super("Vehicle Type does not exist: " + vehicleTypeCode + ".");
    }
}
