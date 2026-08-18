package io.github.igrgin.congestiontax.domain.calculation;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.calculation.exception.InvalidCalculationInputException;
import io.github.igrgin.congestiontax.domain.calculation.exception.NoMatchingTaxTimeBandException;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.domain.rule.TaxTimeBand;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.NonNull;

public final class TaxCalculator {

    public CalculationResult calculate(
            @NonNull VehicleType vehicleType, @NonNull List<Passage> passages, @NonNull TaxRuleSet taxRuleSet) {

        validatePassages(passages);

        var orderedPassages = passages.stream()
                .sorted(Comparator.comparing(Passage::occurredAt))
                .toList();
        var assignedAmounts = initializeDailyAmounts(orderedPassages, taxRuleSet);
        var taxExemptionReasons = initializeTaxExemptionReasons(orderedPassages, vehicleType, taxRuleSet);
        var passageCharges = orderedPassages.stream()
                .map(passage ->
                        new PassageCharge(passage, calculatePassageAmount(passage, taxRuleSet, taxExemptionReasons)))
                .toList();

        taxRuleSet
                .taxRuleOptions()
                .chargeWindow()
                .ifPresentOrElse(
                        chargeWindow -> assignChargeWindows(passageCharges, chargeWindow.duration(), assignedAmounts),
                        () -> assignPassageAmounts(passageCharges, assignedAmounts));

        var dailyTaxes = assignedAmounts.entrySet().stream()
                .map(entry -> {
                    var amount = taxRuleSet
                            .taxRuleOptions()
                            .dailyMaximum()
                            .map(dailyMaximum -> entry.getValue().min(dailyMaximum.amount()))
                            .orElse(entry.getValue());
                    return new DailyTax(entry.getKey(), taxExemptionReasons.get(entry.getKey()), amount);
                })
                .toList();

        var totalAmount =
                dailyTaxes.stream().map(DailyTax::amount).reduce(TaxAmount.zero(taxRuleSet.currency()), TaxAmount::add);

        return new CalculationResult(vehicleType, dailyTaxes, totalAmount);
    }

    private static Map<LocalDate, TaxAmount> initializeDailyAmounts(
            List<Passage> orderedPassages, TaxRuleSet taxRuleSet) {

        Map<LocalDate, TaxAmount> assignedAmounts = new TreeMap<>();
        for (var passage : orderedPassages) {
            assignedAmounts.putIfAbsent(passage.cityDateTime().toLocalDate(), TaxAmount.zero(taxRuleSet.currency()));
        }

        return assignedAmounts;
    }

    private static Map<LocalDate, Set<TaxExemptionReason>> initializeTaxExemptionReasons(
            List<Passage> orderedPassages, VehicleType vehicleType, TaxRuleSet taxRuleSet) {

        Map<LocalDate, Set<TaxExemptionReason>> taxExemptionReasons = new TreeMap<>();
        for (var passage : orderedPassages) {
            var date = passage.cityDateTime().toLocalDate();
            taxExemptionReasons.computeIfAbsent(date, ignored -> taxRuleSet.taxExemptionReasonsFor(vehicleType, date));
        }

        return taxExemptionReasons;
    }

    private static void assignPassageAmounts(
            List<PassageCharge> passageCharges, Map<LocalDate, TaxAmount> assignedAmounts) {

        for (var passageCharge : passageCharges) {
            assign(passageCharge, assignedAmounts);
        }
    }

    private static void assignChargeWindows(
            List<PassageCharge> passageCharges, Duration duration, Map<LocalDate, TaxAmount> assignedAmounts) {

        Instant windowStart = null;
        PassageCharge winner = null;

        for (var passageCharge : passageCharges) {
            if (windowStart == null || passageCharge.passage().occurredAt().isAfter(windowStart.plus(duration))) {
                assign(winner, assignedAmounts);
                windowStart = passageCharge.passage().occurredAt();
                winner = passageCharge;
            } else if (passageCharge.amount().isGreaterThan(winner.amount())) {
                winner = passageCharge;
            }
        }

        assign(winner, assignedAmounts);
    }

    private static void assign(PassageCharge passageCharge, Map<LocalDate, TaxAmount> assignedAmounts) {
        if (passageCharge == null) {
            return;
        }

        var date = passageCharge.passage().cityDateTime().toLocalDate();
        assignedAmounts.merge(date, passageCharge.amount(), TaxAmount::add);
    }

    private static TaxAmount calculatePassageAmount(
            Passage passage, TaxRuleSet taxRuleSet, Map<LocalDate, Set<TaxExemptionReason>> taxExemptionReasons) {

        if (!taxExemptionReasons.get(passage.cityDateTime().toLocalDate()).isEmpty()) {
            return TaxAmount.zero(taxRuleSet.currency());
        }

        return taxRuleSet.taxTimeBands().stream()
                .filter(taxTimeBand ->
                        taxTimeBand.includes(passage.cityDateTime().toLocalTime()))
                .findFirst()
                .map(TaxTimeBand::amount)
                .orElseThrow(NoMatchingTaxTimeBandException::new);
    }

    private static void validatePassages(List<Passage> passages) {
        if (passages.isEmpty()) {
            throw new InvalidCalculationInputException("Passages must not be empty.");
        }
    }

    private record PassageCharge(Passage passage, TaxAmount amount) {}
}
