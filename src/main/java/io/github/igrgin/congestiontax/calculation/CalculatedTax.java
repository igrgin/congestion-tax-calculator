package io.github.igrgin.congestiontax.calculation;

import io.github.igrgin.congestiontax.domain.calculation.CalculationResult;

public record CalculatedTax(String cityCode, CalculationResult calculationResult) {}
