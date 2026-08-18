package io.github.igrgin.congestiontax.domain.rule;

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import lombok.NonNull;

public record TaxRuleSet(
        @NonNull String cityCode,
        @NonNull LocalDate effectiveFrom,
        @NonNull Currency currency,
        @NonNull List<TaxTimeBand> taxTimeBands,
        @NonNull TaxExemptions taxExemptions,
        @NonNull TaxRuleOptions taxRuleOptions) {

    public TaxRuleSet {
        if (taxTimeBands.isEmpty()) {
            throw new IllegalArgumentException("Tax Rule Set must contain at least one Tax Time Band.");
        }
    }

    public TaxRuleSet(String cityCode, LocalDate effectiveFrom, Currency currency, List<TaxTimeBand> taxTimeBands) {
        this(cityCode, effectiveFrom, currency, taxTimeBands, TaxExemptions.empty(), TaxRuleOptions.empty());
    }

    public TaxRuleSet(
            String cityCode,
            LocalDate effectiveFrom,
            Currency currency,
            List<TaxTimeBand> taxTimeBands,
            TaxRuleOptions taxRuleOptions) {
        this(cityCode, effectiveFrom, currency, taxTimeBands, TaxExemptions.empty(), taxRuleOptions);
    }
}
