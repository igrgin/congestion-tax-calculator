package io.github.igrgin.congestiontax.calculation;

import io.github.igrgin.congestiontax.calculation.exception.CityNotFoundException;
import io.github.igrgin.congestiontax.calculation.exception.InvalidStoredTaxRuleOptionException;
import io.github.igrgin.congestiontax.calculation.exception.InvalidStoredTaxTimeBandsException;
import io.github.igrgin.congestiontax.calculation.exception.MissingStoredTaxRuleSetException;
import io.github.igrgin.congestiontax.calculation.exception.UnsupportedPassageYearException;
import io.github.igrgin.congestiontax.calculation.exception.VehicleTypeNotFoundException;
import io.github.igrgin.congestiontax.calculation.model.CalculatedTax;
import io.github.igrgin.congestiontax.calculation.model.CalculationCommand;
import io.github.igrgin.congestiontax.domain.calculation.Passage;
import io.github.igrgin.congestiontax.domain.calculation.TaxCalculator;
import io.github.igrgin.congestiontax.metrics.CalculationMetrics;
import io.github.igrgin.congestiontax.taxrule.TaxRuleService;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidTaxRuleOptionException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxRuleSetException;
import io.github.igrgin.congestiontax.taxrule.exception.OverlappingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
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
        rejectUnsupportedPassageYears(command.passageCityDateTimes());

        log.debug(
                "Started Congestion Tax Calculation. cityCode={} vehicleTypeCode={} passageCount={}",
                command.cityCode(),
                command.vehicleTypeCode(),
                command.passageCityDateTimes().size());

        try {
            var calculatedTax = calculationMetrics.recordCalculation(
                    command.passageCityDateTimes().size(), () -> calculateTax(command));

            log.info(
                    "Completed Congestion Tax Calculation."
                            + " cityCode={} vehicleTypeCode={} passageCount={} dailyTaxCount={}",
                    command.cityCode(),
                    command.vehicleTypeCode(),
                    command.passageCityDateTimes().size(),
                    calculatedTax.calculationResult().dailyTaxes().size());

            return calculatedTax;
        } catch (UnknownCityException exception) {
            throw new CityNotFoundException(command.cityCode(), exception);
        } catch (UnknownVehicleTypeException exception) {
            throw new VehicleTypeNotFoundException(command.vehicleTypeCode(), exception);
        } catch (InvalidTaxRuleOptionException exception) {
            throw new InvalidStoredTaxRuleOptionException(exception.optionTypeCode(), exception);
        } catch (MissingTaxRuleSetException exception) {
            throw new MissingStoredTaxRuleSetException(command.cityCode(), exception);
        } catch (OverlappingTaxTimeBandsException exception) {
            throw new InvalidStoredTaxTimeBandsException(command.cityCode(), exception);
        }
    }

    private CalculatedTax calculateTax(CalculationCommand command) {
        var cityTaxRuleSet = taxRuleService.getCityTaxRuleSet(command.cityCode());

        var passages = command.passageCityDateTimes().stream()
                .map(cityDateTime -> new Passage(
                        cityDateTime.atZone(cityTaxRuleSet.cityTimeZone()).toInstant(), cityDateTime))
                .toList();

        var vehicleType = taxRuleService.getVehicleType(command.vehicleTypeCode());

        var calculationResult = taxCalculator.calculate(vehicleType, passages, cityTaxRuleSet.taxRuleSet());

        return new CalculatedTax(command.cityCode(), calculationResult);
    }

    private static void rejectUnsupportedPassageYears(List<LocalDateTime> passageCityDateTimes) {
        var unsupportedPassageIndexes = IntStream.range(0, passageCityDateTimes.size())
                .filter(index -> passageCityDateTimes.get(index).getYear() != 2013)
                .boxed()
                .toList();

        if (!unsupportedPassageIndexes.isEmpty()) {
            throw new UnsupportedPassageYearException(unsupportedPassageIndexes);
        }
    }
}
