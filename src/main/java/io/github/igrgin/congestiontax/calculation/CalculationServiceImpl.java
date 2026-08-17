package io.github.igrgin.congestiontax.calculation;

import io.github.igrgin.congestiontax.calculation.exception.CityNotFoundException;
import io.github.igrgin.congestiontax.calculation.exception.VehicleTypeNotFoundException;
import io.github.igrgin.congestiontax.domain.calculation.TaxCalculator;
import io.github.igrgin.congestiontax.metrics.CalculationMetrics;
import io.github.igrgin.congestiontax.taxrule.TaxRuleService;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalculationServiceImpl implements CalculationService {

    private final TaxRuleService taxRuleService;
    private final TaxCalculator taxCalculator;
    private final CalculationMetrics calculationMetrics;

    @Override
    public CalculatedTax calculate(CalculationCommand command) {
        log.debug(
                "Started Congestion Tax Calculation. cityCode={} vehicleTypeCode={} passageCount={}",
                command.cityCode(),
                command.vehicleTypeCode(),
                command.passages().size());

        try {
            var calculatedTax = calculationMetrics.recordCalculation(() -> calculateTax(command));

            log.info(
                    "Completed Congestion Tax Calculation."
                            + " cityCode={} vehicleTypeCode={} passageCount={} dailyTaxCount={}",
                    command.cityCode(),
                    command.vehicleTypeCode(),
                    command.passages().size(),
                    calculatedTax.calculationResult().dailyTaxes().size());

            return calculatedTax;
        } catch (UnknownCityException exception) {
            throw new CityNotFoundException(command.cityCode(), exception);
        } catch (UnknownVehicleTypeException exception) {
            throw new VehicleTypeNotFoundException(command.vehicleTypeCode(), exception);
        }
    }

    private CalculatedTax calculateTax(CalculationCommand command) {
        var calculationDates = command.passages().stream()
                .map(passage -> passage.cityDateTime().toLocalDate())
                .collect(Collectors.toUnmodifiableSet());

        var applicableTaxRuleSets = taxRuleService.getApplicableTaxRuleSets(command.cityCode(), calculationDates);

        var vehicleType = taxRuleService.getVehicleType(command.vehicleTypeCode());

        var calculationResult = taxCalculator.calculate(vehicleType, command.passages(), applicableTaxRuleSets);

        return new CalculatedTax(command.cityCode(), calculationResult);
    }
}
