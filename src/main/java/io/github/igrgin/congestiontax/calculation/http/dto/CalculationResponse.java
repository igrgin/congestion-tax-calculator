package io.github.igrgin.congestiontax.calculation.http.dto;

import io.github.igrgin.congestiontax.calculation.model.CalculatedTax;
import java.math.BigDecimal;
import java.util.List;

public record CalculationResponse(
        String cityCode,
        String vehicleType,
        String currency,
        BigDecimal totalAmount,
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
