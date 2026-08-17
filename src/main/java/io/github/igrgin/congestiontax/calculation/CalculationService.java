package io.github.igrgin.congestiontax.calculation;

public interface CalculationService {

    public CalculatedTax calculate(CalculationCommand command);
}
