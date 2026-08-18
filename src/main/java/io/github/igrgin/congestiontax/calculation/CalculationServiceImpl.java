package io.github.igrgin.congestiontax.calculation;

import io.github.igrgin.congestiontax.calculation.exception.CityNotFoundException;
import io.github.igrgin.congestiontax.calculation.exception.InvalidPassageTimestampException;
import io.github.igrgin.congestiontax.calculation.exception.InvalidStoredTaxRuleOptionException;
import io.github.igrgin.congestiontax.calculation.exception.UnsupportedPassageYearException;
import io.github.igrgin.congestiontax.calculation.exception.VehicleTypeNotFoundException;
import io.github.igrgin.congestiontax.calculation.model.CalculatedTax;
import io.github.igrgin.congestiontax.calculation.model.CalculationCommand;
import io.github.igrgin.congestiontax.domain.calculation.Passage;
import io.github.igrgin.congestiontax.domain.calculation.TaxCalculator;
import io.github.igrgin.congestiontax.metrics.CalculationMetrics;
import io.github.igrgin.congestiontax.taxrule.TaxRuleService;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidTaxRuleOptionException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalculationServiceImpl implements CalculationService {

    private static final DateTimeFormatter PASSAGE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

    private final TaxRuleService taxRuleService;
    private final TaxCalculator taxCalculator;
    private final CalculationMetrics calculationMetrics;

    @Override
    public CalculatedTax calculate(CalculationCommand command) {
        var passageCityDateTimes = parsePassageCityDateTimes(command.passageTimestamps());
        rejectUnsupportedPassageYears(passageCityDateTimes);

        log.debug(
                "Started Congestion Tax Calculation. cityCode={} vehicleTypeCode={} passageCount={}",
                command.cityCode(),
                command.vehicleTypeCode(),
                command.passageTimestamps().size());

        try {
            var calculatedTax = calculationMetrics.recordCalculation(
                    command.passageTimestamps().size(), () -> calculateTax(command, passageCityDateTimes));

            log.info(
                    "Completed Congestion Tax Calculation."
                            + " cityCode={} vehicleTypeCode={} passageCount={} dailyTaxCount={}",
                    command.cityCode(),
                    command.vehicleTypeCode(),
                    command.passageTimestamps().size(),
                    calculatedTax.calculationResult().dailyTaxes().size());

            return calculatedTax;
        } catch (UnknownCityException exception) {
            throw new CityNotFoundException(command.cityCode(), exception);
        } catch (UnknownVehicleTypeException exception) {
            throw new VehicleTypeNotFoundException(command.vehicleTypeCode(), exception);
        } catch (InvalidTaxRuleOptionException exception) {
            throw new InvalidStoredTaxRuleOptionException(exception.optionTypeCode(), exception);
        }
    }

    private CalculatedTax calculateTax(CalculationCommand command, List<LocalDateTime> passageCityDateTimes) {
        var calculationDates =
                passageCityDateTimes.stream().map(LocalDateTime::toLocalDate).collect(Collectors.toUnmodifiableSet());

        var applicableTaxRuleSets = taxRuleService.getApplicableTaxRuleSets(command.cityCode(), calculationDates);

        var passages = passageCityDateTimes.stream()
                .map(cityDateTime -> new Passage(
                        cityDateTime
                                .atZone(applicableTaxRuleSets.cityTimeZone())
                                .toInstant(),
                        cityDateTime))
                .toList();

        var vehicleType = taxRuleService.getVehicleType(command.vehicleTypeCode());

        var calculationResult =
                taxCalculator.calculate(vehicleType, passages, applicableTaxRuleSets.taxRuleSetsByCalculationDate());

        return new CalculatedTax(command.cityCode(), calculationResult);
    }

    private static List<LocalDateTime> parsePassageCityDateTimes(List<String> passageTimestamps) {
        return IntStream.range(0, passageTimestamps.size())
                .mapToObj(index -> parsePassageCityDateTime(passageTimestamps.get(index), index))
                .toList();
    }

    private static LocalDateTime parsePassageCityDateTime(String timestamp, int passageIndex) {
        try {
            return LocalDateTime.parse(timestamp, PASSAGE_TIMESTAMP_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new InvalidPassageTimestampException(passageIndex, exception);
        }
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
