package io.github.igrgin.congestiontax.domain.rule.exception;

import java.time.LocalTime;

public final class InvalidTaxTimeBandException extends IllegalArgumentException {

    public InvalidTaxTimeBandException(LocalTime startTime, LocalTime endTime) {
        super("Tax Time Band end time must be after its start time: %s to %s.".formatted(startTime, endTime));
    }

    public InvalidTaxTimeBandException() {
        super("Tax Time Band Tax Amount must be positive.");
    }
}
