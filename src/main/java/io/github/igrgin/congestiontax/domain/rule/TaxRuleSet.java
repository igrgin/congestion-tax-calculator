package io.github.igrgin.congestiontax.domain.rule;

import java.util.Currency;
import java.util.List;
import lombok.NonNull;

public record TaxRuleSet(
        @NonNull String cityCode,
        @NonNull Currency currency,
        @NonNull List<TaxTimeBand> taxTimeBands,
        @NonNull TaxRuleOptions taxRuleOptions) {

    public TaxRuleSet {
        if (taxTimeBands.isEmpty()) {
            throw new IllegalArgumentException("Tax Rule Set must contain at least one Tax Time Band.");
        }
    }

    public TaxRuleSet(String cityCode, Currency currency, List<TaxTimeBand> taxTimeBands) {
        this(cityCode, currency, taxTimeBands, TaxRuleOptions.empty());
    }
}
