package io.github.igrgin.congestiontax.calculation;

import io.github.igrgin.congestiontax.calculation.model.CalculatedTax;
import io.github.igrgin.congestiontax.calculation.model.CalculationCommand;

public interface CalculationService {

    public CalculatedTax calculate(CalculationCommand command);
}
