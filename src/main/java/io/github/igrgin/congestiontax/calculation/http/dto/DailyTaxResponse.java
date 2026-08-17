package io.github.igrgin.congestiontax.calculation.http.dto;

import io.github.igrgin.congestiontax.domain.calculation.DailyTax;
import io.github.igrgin.congestiontax.domain.calculation.TaxExemptionReason;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record DailyTaxResponse(LocalDate date, Set<TaxExemptionReason> taxExemptionReasons, BigDecimal amount) {

    public static DailyTaxResponse from(DailyTax dailyTax) {
        return new DailyTaxResponse(
                dailyTax.date(),
                dailyTax.taxExemptionReasons(),
                dailyTax.amount().amount());
    }
}
