package io.github.igrgin.congestiontax.calculation.http.dto;

import io.github.igrgin.congestiontax.domain.calculation.DailyTax;
import io.github.igrgin.congestiontax.domain.calculation.TaxExemptionReason;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record DailyTaxResponse(
        @Schema(description = "City Local Time date.", example = "2013-02-08")
        LocalDate date,

        @ArraySchema(
                arraySchema = @Schema(description = "Tax Exemption Reasons that made the Daily Tax zero."),
                schema = @Schema(implementation = TaxExemptionReason.class))
        Set<TaxExemptionReason> taxExemptionReasons,

        @Schema(description = "Daily Tax Amount.", example = "16.00")
        BigDecimal amount) {

    public static DailyTaxResponse from(DailyTax dailyTax) {
        return new DailyTaxResponse(
                dailyTax.date(),
                dailyTax.taxExemptionReasons(),
                dailyTax.amount().amount());
    }
}
