package io.github.igrgin.congestiontax.calculation.http.exception;

public final class InvalidTimeZoneException extends IllegalArgumentException {

    public InvalidTimeZoneException() {
        super("The calculation time zone must be a valid IANA time-zone identifier.");
    }
}
