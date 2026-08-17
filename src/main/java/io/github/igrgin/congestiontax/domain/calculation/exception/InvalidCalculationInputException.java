package io.github.igrgin.congestiontax.domain.calculation.exception;

public final class InvalidCalculationInputException extends IllegalArgumentException {

    public InvalidCalculationInputException(String message) {
        super(message);
    }
}
