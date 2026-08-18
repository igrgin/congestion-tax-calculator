package io.github.igrgin.congestiontax.domain.calculation;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.calculation.exception.InvalidCalculationInputException;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.domain.rule.TaxTimeBand;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;

public final class TaxCalculator {

    public CalculationResult calculate(
            @NonNull VehicleType vehicleType,
            @NonNull List<Passage> passages,
            @NonNull Map<LocalDate, TaxRuleSet> applicableTaxRuleSets) {

        validateInput(passages, applicableTaxRuleSets);

        var passagesByDate = passages.stream()
                .collect(Collectors.groupingBy(passage -> passage.cityDateTime().toLocalDate()));

        var dailyTaxes = passagesByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    var taxRuleSet = applicableTaxRuleSets.get(entry.getKey());
                    var amount = calculateDailyAmount(entry.getValue(), taxRuleSet);

                    return new DailyTax(entry.getKey(), amount);
                })
                .toList();

        var totalAmount = dailyTaxes.stream()
                .map(DailyTax::amount)
                .reduce(TaxAmount.zero(dailyTaxes.get(0).amount().currency()), TaxAmount::add);

        return new CalculationResult(vehicleType, dailyTaxes, totalAmount);
    }

    private static TaxAmount calculateDailyAmount(List<Passage> passages, TaxRuleSet taxRuleSet) {
        var orderedPassages = passages.stream()
                .sorted(Comparator.comparing(Passage::occurredAt))
                .toList();

        var chargeWindowAmount = taxRuleSet
                .taxRuleOptions()
                .chargeWindow()
                .map(chargeWindow -> calculateChargeWindows(orderedPassages, taxRuleSet, chargeWindow.duration()))
                .orElseGet(() -> orderedPassages.stream()
                        .map(passage -> calculatePassageAmount(passage, taxRuleSet))
                        .reduce(TaxAmount.zero(taxRuleSet.currency()), TaxAmount::add));

        return taxRuleSet
                .taxRuleOptions()
                .dailyMaximum()
                .map(dailyMaximum -> chargeWindowAmount.min(dailyMaximum.amount()))
                .orElse(chargeWindowAmount);
    }

    private static TaxAmount calculateChargeWindows(
            List<Passage> orderedPassages, TaxRuleSet taxRuleSet, Duration duration) {

        var totalAmount = TaxAmount.zero(taxRuleSet.currency());
        Instant windowStart = null;
        var windowAmount = TaxAmount.zero(taxRuleSet.currency());

        for (var passage : orderedPassages) {
            var passageAmount = calculatePassageAmount(passage, taxRuleSet);

            if (windowStart == null || passage.occurredAt().isAfter(windowStart.plus(duration))) {
                totalAmount = totalAmount.add(windowAmount);
                windowStart = passage.occurredAt();
                windowAmount = passageAmount;
            } else {
                windowAmount = windowAmount.max(passageAmount);
            }
        }

        return totalAmount.add(windowAmount);
    }

    private static TaxAmount calculatePassageAmount(Passage passage, TaxRuleSet taxRuleSet) {
        return taxRuleSet.taxTimeBands().stream()
                .filter(taxTimeBand ->
                        taxTimeBand.includes(passage.cityDateTime().toLocalTime()))
                .findFirst()
                .map(TaxTimeBand::amount)
                .orElseGet(() -> TaxAmount.zero(taxRuleSet.currency()));
    }

    private static void validateInput(List<Passage> passages, Map<LocalDate, TaxRuleSet> applicableTaxRuleSets) {

        if (passages.isEmpty()) {
            throw new InvalidCalculationInputException("Passages must not be empty.");
        }

        if (applicableTaxRuleSets.isEmpty()) {
            throw new InvalidCalculationInputException("Applicable Tax Rule Sets must not be empty.");
        }

        for (var passage : passages) {
            var calculationDate = passage.cityDateTime().toLocalDate();

            if (applicableTaxRuleSets.get(calculationDate) == null) {
                throw new InvalidCalculationInputException(
                        "Applicable Tax Rule Set is missing" + " for calculation date: " + calculationDate + ".");
            }
        }
    }
}
