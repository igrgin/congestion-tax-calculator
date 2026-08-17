package io.github.igrgin.congestiontax.taxrule.persistence;

import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.taxrule.TaxRuleService;
import io.github.igrgin.congestiontax.taxrule.exception.MissingApplicableTaxRuleSetException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.OverlappingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaxRuleServiceImpl implements TaxRuleService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final TaxRuleSetRepository taxRuleSetRepository;
    private final TaxTimeBandRepository taxTimeBandRepository;

    @Override
    @Transactional(readOnly = true)
    public VehicleType getVehicleType(String vehicleTypeCode) {
        return vehicleTypeRepository
                .findByCode(vehicleTypeCode)
                .map(VehicleTypeEntity::toVehicleType)
                .orElseThrow(() -> new UnknownVehicleTypeException(vehicleTypeCode));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<LocalDate, TaxRuleSet> getApplicableTaxRuleSets(String cityCode, Set<LocalDate> calculationDates) {
        if (!taxRuleSetRepository.cityExists(cityCode)) {
            throw new UnknownCityException(cityCode);
        }

        var latestCalculationDate =
                calculationDates.stream().max(LocalDate::compareTo).orElseThrow();

        var candidates = taxRuleSetRepository.findApplicableCandidates(cityCode, latestCalculationDate);

        var ruleSetEntitiesByDate = calculationDates.stream()
                .collect(Collectors.toUnmodifiableMap(
                        Function.identity(),
                        calculationDate -> selectApplicableTaxRuleSet(cityCode, calculationDate, candidates)));

        var selectedRuleSetIds = ruleSetEntitiesByDate.values().stream()
                .map(TaxRuleSetEntity::getId)
                .collect(Collectors.toUnmodifiableSet());

        var taxTimeBandsByRuleSetId = taxTimeBandRepository.findByRuleSetIdIn(selectedRuleSetIds).stream()
                .collect(Collectors.groupingBy(TaxTimeBandEntity::getRuleSetId));

        return ruleSetEntitiesByDate.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> mapCompleteTaxRuleSet(cityCode, entry.getValue(), taxTimeBandsByRuleSetId)));
    }

    private static TaxRuleSetEntity selectApplicableTaxRuleSet(
            String cityCode, LocalDate calculationDate, List<TaxRuleSetEntity> candidates) {
        return candidates.stream()
                .filter(candidate -> !candidate.getEffectiveFrom().isAfter(calculationDate))
                .max(Comparator.comparing(TaxRuleSetEntity::getEffectiveFrom))
                .orElseThrow(() -> new MissingApplicableTaxRuleSetException(cityCode, calculationDate));
    }

    private static TaxRuleSet mapCompleteTaxRuleSet(
            String cityCode,
            TaxRuleSetEntity taxRuleSetEntity,
            Map<Long, List<TaxTimeBandEntity>> taxTimeBandsByRuleSetId) {
        var taxTimeBandEntities = taxTimeBandsByRuleSetId.getOrDefault(taxRuleSetEntity.getId(), List.of());

        if (taxTimeBandEntities.isEmpty()) {
            throw new MissingTaxTimeBandsException(cityCode, taxRuleSetEntity.getEffectiveFrom());
        }

        validateTaxTimeBands(cityCode, taxRuleSetEntity.getEffectiveFrom(), taxTimeBandEntities);

        return taxRuleSetEntity.toTaxRuleSet(cityCode, taxTimeBandEntities);
    }

    private static void validateTaxTimeBands(
            String cityCode, LocalDate effectiveFrom, List<TaxTimeBandEntity> taxTimeBands) {
        var orderedTaxTimeBands = taxTimeBands.stream()
                .sorted(Comparator.comparing(TaxTimeBandEntity::getStartTime))
                .toList();

        for (var index = 1; index < orderedTaxTimeBands.size(); index++) {
            var precedingTaxTimeBand = orderedTaxTimeBands.get(index - 1);
            var taxTimeBand = orderedTaxTimeBands.get(index);

            if (taxTimeBand.getStartTime().isBefore(precedingTaxTimeBand.getEndTime())) {
                throw new OverlappingTaxTimeBandsException(
                        cityCode,
                        effectiveFrom,
                        precedingTaxTimeBand.getStartTime(),
                        precedingTaxTimeBand.getEndTime(),
                        taxTimeBand.getStartTime(),
                        taxTimeBand.getEndTime());
            }
        }
    }
}
