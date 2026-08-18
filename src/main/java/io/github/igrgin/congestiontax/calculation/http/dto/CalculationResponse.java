package io.github.igrgin.congestiontax.calculation.http.dto;

import io.github.igrgin.congestiontax.calculation.model.CalculatedTax;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record CalculationResponse(
        @Schema(description = "City code that selected the Tax Rule Set.", example = "gothenburg")
        String cityCode,

        @Schema(description = "Database-defined Vehicle Type code.", example = "OTHER")
        String vehicleType,

        @Schema(description = "ISO 4217 currency code for all Tax Amounts.", example = "SEK")
        String currency,

        @Schema(description = "Total Tax Amount for all Passages.", example = "16.00")
        BigDecimal totalAmount,

        @ArraySchema(
                arraySchema = @Schema(description = "Daily Taxes in City Local Time date order."),
                schema = @Schema(implementation = DailyTaxResponse.class))
        List<DailyTaxResponse> dailyTaxes) {

    public static CalculationResponse from(CalculatedTax calculatedTax) {
        var calculationResult = calculatedTax.calculationResult();

        var dailyTaxes = calculationResult.dailyTaxes().stream()
                .map(DailyTaxResponse::from)
                .toList();

        return new CalculationResponse(
                calculatedTax.cityCode(),
                calculationResult.vehicleType().code(),
                calculationResult.totalAmount().currency().getCurrencyCode(),
                calculationResult.totalAmount().amount(),
                dailyTaxes);
    }
}
