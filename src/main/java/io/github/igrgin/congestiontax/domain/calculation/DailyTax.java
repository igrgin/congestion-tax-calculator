package io.github.igrgin.congestiontax.domain.calculation;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import java.time.LocalDate;
import java.util.Set;

public record DailyTax(LocalDate date, Set<TaxExemptionReason> taxExemptionReasons, TaxAmount amount) {

    public DailyTax(LocalDate date, TaxAmount amount) {
        this(date, Set.of(), amount);
    }
}
