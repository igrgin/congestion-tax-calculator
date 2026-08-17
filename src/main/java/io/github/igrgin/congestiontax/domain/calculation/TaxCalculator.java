package io.github.igrgin.congestiontax.domain.calculation;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.calculation.exception.InvalidCalculationInputException;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.domain.rule.TaxTimeBand;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

public final class TaxCalculator {

    public CalculationResult calculate(
            @NonNull VehicleType vehicleType,
            @NonNull List<Passage> passages,
            @NonNull Map<LocalDate, TaxRuleSet> applicableTaxRuleSets) {

        validateInput(passages, applicableTaxRuleSets);

        var passage = passages.get(0);
        var date = passage.cityDateTime().toLocalDate();
        var taxRuleSet = applicableTaxRuleSets.get(date);

        var amount = taxRuleSet.taxTimeBands().stream()
                .filter(taxTimeBand ->
                        taxTimeBand.includes(passage.cityDateTime().toLocalTime()))
                .findFirst()
                .map(TaxTimeBand::amount)
                .orElseGet(() -> TaxAmount.zero(taxRuleSet.currency()));

        var dailyTax = new DailyTax(date, amount);

        return new CalculationResult(vehicleType, List.of(dailyTax), amount);
    }

    private static void validateInput(List<Passage> passages, Map<LocalDate, TaxRuleSet> applicableTaxRuleSets) {

        if (passages.isEmpty()) {
            throw new InvalidCalculationInputException("Passages must not be empty.");
        }

        if (applicableTaxRuleSets.isEmpty()) {
            throw new InvalidCalculationInputException("Applicable Tax Rule Sets must not be empty.");
        }

        var calculationDate = passages.get(0).cityDateTime().toLocalDate();

        if (applicableTaxRuleSets.get(calculationDate) == null) {
            throw new InvalidCalculationInputException(
                    "Applicable Tax Rule Set is missing" + " for calculation date: " + calculationDate + ".");
        }
    }
}
