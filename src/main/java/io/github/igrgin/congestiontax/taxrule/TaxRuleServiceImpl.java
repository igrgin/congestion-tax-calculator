package io.github.igrgin.congestiontax.taxrule;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.rule.ChargeWindow;
import io.github.igrgin.congestiontax.domain.rule.DailyMaximum;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleOption;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleOptions;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidCityTimeZoneException;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidTaxRuleOptionException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingApplicableTaxRuleSetException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.OverlappingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import io.github.igrgin.congestiontax.taxrule.model.ApplicableTaxRuleSets;
import io.github.igrgin.congestiontax.taxrule.persistence.CityEntity;
import io.github.igrgin.congestiontax.taxrule.persistence.CityRepository;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxRuleOptionEntity;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxRuleOptionRepository;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxRuleOptionType;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxRuleSetEntity;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxRuleSetRepository;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxTimeBandEntity;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxTimeBandRepository;
import io.github.igrgin.congestiontax.taxrule.persistence.VehicleTypeEntity;
import io.github.igrgin.congestiontax.taxrule.persistence.VehicleTypeRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxRuleServiceImpl implements TaxRuleService {

    private final CityRepository cityRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final TaxRuleSetRepository taxRuleSetRepository;
    private final TaxTimeBandRepository taxTimeBandRepository;
    private final TaxRuleOptionRepository taxRuleOptionRepository;

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
    public ApplicableTaxRuleSets getApplicableTaxRuleSets(String cityCode, Set<LocalDate> calculationDates) {
        var cityTimeZone = cityRepository
                .findByCode(cityCode)
                .map(CityEntity::getTimeZone)
                .map(timeZone -> parseCityTimeZone(cityCode, timeZone))
                .orElseThrow(() -> new UnknownCityException(cityCode));

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

        var taxRuleOptionsByRuleSetId = taxRuleOptionRepository.findByRuleSetIdIn(selectedRuleSetIds).stream()
                .collect(Collectors.groupingBy(TaxRuleOptionEntity::getRuleSetId));

        var taxRuleSetsByCalculationDate = ruleSetEntitiesByDate.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> {
                    var taxRuleSet = mapCompleteTaxRuleSet(
                            cityCode, entry.getValue(), taxTimeBandsByRuleSetId, taxRuleOptionsByRuleSetId);

                    log.debug(
                            "Selected Applicable Tax Rule Set."
                                    + " cityCode={} calculationDate={} effectiveFrom={}"
                                    + " chargeWindowEnabled={} dailyMaximumEnabled={}",
                            cityCode,
                            entry.getKey(),
                            taxRuleSet.effectiveFrom(),
                            taxRuleSet.taxRuleOptions().chargeWindow().isPresent(),
                            taxRuleSet.taxRuleOptions().dailyMaximum().isPresent());

                    return taxRuleSet;
                }));

        return new ApplicableTaxRuleSets(cityTimeZone, taxRuleSetsByCalculationDate);
    }

    private static ZoneId parseCityTimeZone(String cityCode, String timeZone) {
        if (!ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw new InvalidCityTimeZoneException(cityCode);
        }

        return ZoneId.of(timeZone);
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
            Map<Long, List<TaxTimeBandEntity>> taxTimeBandsByRuleSetId,
            Map<Long, List<TaxRuleOptionEntity>> taxRuleOptionsByRuleSetId) {
        var taxTimeBandEntities = taxTimeBandsByRuleSetId.getOrDefault(taxRuleSetEntity.getId(), List.of());

        if (taxTimeBandEntities.isEmpty()) {
            throw new MissingTaxTimeBandsException(cityCode, taxRuleSetEntity.getEffectiveFrom());
        }

        validateTaxTimeBands(cityCode, taxRuleSetEntity.getEffectiveFrom(), taxTimeBandEntities);

        var taxRuleOptions = mapTaxRuleOptions(
                taxRuleOptionsByRuleSetId.getOrDefault(taxRuleSetEntity.getId(), List.of()),
                Currency.getInstance(taxRuleSetEntity.getCurrencyCode()));

        return taxRuleSetEntity.toTaxRuleSet(cityCode, taxTimeBandEntities, taxRuleOptions);
    }

    private static TaxRuleOptions mapTaxRuleOptions(
            List<TaxRuleOptionEntity> taxRuleOptionEntities, Currency currency) {
        validateUniqueTaxRuleOptionTypes(taxRuleOptionEntities);

        var taxRuleOptions = taxRuleOptionEntities.stream()
                .<TaxRuleOption>mapMulti((entity, consumer) -> {
                    if (entity.getType() == TaxRuleOptionType.CHARGE_WINDOW) {
                        validateChargeWindow(entity);
                        consumer.accept(new ChargeWindow(Duration.ofMinutes(entity.getDurationMinutes())));
                    } else if (entity.getType() == TaxRuleOptionType.DAILY_MAXIMUM) {
                        validateDailyMaximum(entity);
                        consumer.accept(new DailyMaximum(new TaxAmount(entity.getAmount(), currency)));
                    }
                })
                .toList();

        return new TaxRuleOptions(taxRuleOptions);
    }

    private static void validateUniqueTaxRuleOptionTypes(List<TaxRuleOptionEntity> entities) {
        var optionTypes = new HashSet<TaxRuleOptionType>();

        for (var entity : entities) {
            if (entity.getType() == null) {
                throw new InvalidTaxRuleOptionException("UNKNOWN");
            }

            if (!optionTypes.add(entity.getType())) {
                throw new InvalidTaxRuleOptionException(entity.getType().name());
            }
        }
    }

    private static void validateChargeWindow(TaxRuleOptionEntity entity) {
        if (entity.getDurationMinutes() == null
                || entity.getDurationMinutes() <= 0
                || entity.getAmount() != null
                || entity.getPrecedingDays() != null) {
            throw new InvalidTaxRuleOptionException(entity.getType().name());
        }
    }

    private static void validateDailyMaximum(TaxRuleOptionEntity entity) {
        if (entity.getAmount() == null
                || entity.getAmount().signum() <= 0
                || entity.getDurationMinutes() != null
                || entity.getPrecedingDays() != null) {
            throw new InvalidTaxRuleOptionException(entity.getType().name());
        }
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
