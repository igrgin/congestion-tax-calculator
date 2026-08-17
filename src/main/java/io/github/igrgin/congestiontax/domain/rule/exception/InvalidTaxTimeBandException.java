package io.github.igrgin.congestiontax.domain.rule.exception;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import java.time.LocalTime;

public final class InvalidTaxTimeBandException extends IllegalArgumentException {

    public InvalidTaxTimeBandException(LocalTime startTime, LocalTime endTime) {
        super("Tax Time Band end time must be after its start time: %s to %s.".formatted(startTime, endTime));
    }

    public InvalidTaxTimeBandException(TaxAmount amount) {
        super("Tax Time Band Tax Amount must be positive: %s %s."
                .formatted(amount.amount(), amount.currency().getCurrencyCode()));
    }
}
