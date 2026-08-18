package io.github.igrgin.congestiontax.domain.rule.exception;

import java.time.LocalTime;

public final class InvalidTaxTimeBandException extends IllegalArgumentException {

    public InvalidTaxTimeBandException(LocalTime startTime, LocalTime endTime) {
        super("Tax Time Band start time and end time must be different: %s to %s.".formatted(startTime, endTime));
    }

    public InvalidTaxTimeBandException() {
        super("Tax Time Band Tax Amount must not be negative.");
    }
}
