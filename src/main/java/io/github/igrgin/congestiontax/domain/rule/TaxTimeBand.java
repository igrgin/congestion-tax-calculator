package io.github.igrgin.congestiontax.domain.rule;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import java.time.LocalTime;
import lombok.NonNull;

public record TaxTimeBand(
        @NonNull LocalTime startTime,
        @NonNull LocalTime endTime,
        @NonNull TaxAmount amount) {

    public boolean includes(@NonNull LocalTime localTime) {
        if (endTime.equals(startTime)) {
            return true;
        }

        if (endTime.isAfter(startTime)) {
            return !localTime.isBefore(startTime) && localTime.isBefore(endTime);
        }

        return !localTime.isBefore(startTime) || localTime.isBefore(endTime);
    }
}
