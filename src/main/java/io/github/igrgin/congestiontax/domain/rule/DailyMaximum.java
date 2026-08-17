package io.github.igrgin.congestiontax.domain.rule;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import lombok.NonNull;

public record DailyMaximum(@NonNull TaxAmount amount) implements TaxRuleOption {

    public DailyMaximum {
        if (amount.amount().signum() == 0) {
            throw new IllegalArgumentException("Daily Maximum Tax Amount must be positive.");
        }
    }
}
