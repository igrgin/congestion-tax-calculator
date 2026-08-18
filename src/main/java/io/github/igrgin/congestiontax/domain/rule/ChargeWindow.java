package io.github.igrgin.congestiontax.domain.rule;

import java.time.Duration;
import lombok.NonNull;

public record ChargeWindow(@NonNull Duration duration) implements TaxRuleOption {

    public ChargeWindow {
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Charge Window duration must be positive.");
        }
    }
}
