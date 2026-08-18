package io.github.igrgin.congestiontax.domain.rule;

import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.calculation.TaxExemptionReason;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.NonNull;

public record TaxRuleSet(
        @NonNull String cityCode,
        @NonNull Currency currency,
        @NonNull List<TaxTimeBand> taxTimeBands,
        @NonNull TaxExemptions taxExemptions,
        @NonNull TaxRuleOptions taxRuleOptions) {

    public TaxRuleSet {
        if (taxTimeBands.isEmpty()) {
            throw new IllegalArgumentException("Tax Rule Set must contain at least one Tax Time Band.");
        }
    }

    public TaxRuleSet(String cityCode, Currency currency, List<TaxTimeBand> taxTimeBands) {
        this(cityCode, currency, taxTimeBands, TaxExemptions.empty(), TaxRuleOptions.empty());
    }

    public TaxRuleSet(
            String cityCode, Currency currency, List<TaxTimeBand> taxTimeBands, TaxRuleOptions taxRuleOptions) {
        this(cityCode, currency, taxTimeBands, TaxExemptions.empty(), taxRuleOptions);
    }

    public Set<TaxExemptionReason> taxExemptionReasonsFor(VehicleType vehicleType, LocalDate date) {
        Objects.requireNonNull(vehicleType);
        Objects.requireNonNull(date);
        return taxExemptions.reasonsFor(vehicleType, date, taxRuleOptions.publicHolidayPrecedingDateOption());
    }
}
