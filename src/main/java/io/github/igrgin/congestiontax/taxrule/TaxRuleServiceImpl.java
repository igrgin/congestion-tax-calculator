package io.github.igrgin.congestiontax.taxrule;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.rule.ChargeWindow;
import io.github.igrgin.congestiontax.domain.rule.DailyMaximum;
import io.github.igrgin.congestiontax.domain.rule.HolidayPreceding;
import io.github.igrgin.congestiontax.domain.rule.MonthTaxExemption;
import io.github.igrgin.congestiontax.domain.rule.PublicHolidayTaxExemption;
import io.github.igrgin.congestiontax.domain.rule.TaxExemption;
import io.github.igrgin.congestiontax.domain.rule.TaxExemptions;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleOption;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleOptions;
import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import io.github.igrgin.congestiontax.domain.rule.VehicleTypeTaxExemption;
import io.github.igrgin.congestiontax.domain.rule.WeekdayTaxExemption;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidCityTimeZoneException;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidTaxExemptionException;
import io.github.igrgin.congestiontax.taxrule.exception.InvalidTaxRuleOptionException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingApplicableTaxRuleSetException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.OverlappingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import io.github.igrgin.congestiontax.taxrule.model.ApplicableTaxRuleSets;
import io.github.igrgin.congestiontax.taxrule.persistence.CityEntity;
import io.github.igrgin.congestiontax.taxrule.persistence.CityRepository;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxExemptionEntity;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxExemptionRepository;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxExemptionType;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxRuleOptionEntity;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxRuleOptionRepository;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxRuleOptionType;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxRuleSetEntity;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxRuleSetRepository;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxTimeBandEntity;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxTimeBandRepository;
import io.github.igrgin.congestiontax.taxrule.persistence.VehicleTypeEntity;
import io.github.igrgin.congestiontax.taxrule.persistence.VehicleTypeRepository;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
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
    private final TaxExemptionRepository taxExemptionRepository;

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

        var taxExemptionsByRuleSetId = taxExemptionRepository.findByRuleSetIdIn(selectedRuleSetIds).stream()
                .collect(Collectors.groupingBy(TaxExemptionEntity::getRuleSetId));

        var taxRuleSetsByCalculationDate = ruleSetEntitiesByDate.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> {
                    var taxRuleSet = mapCompleteTaxRuleSet(
                            cityCode,
                            entry.getValue(),
                            taxTimeBandsByRuleSetId,
                            taxRuleOptionsByRuleSetId,
                            taxExemptionsByRuleSetId);

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
            Map<Long, List<TaxRuleOptionEntity>> taxRuleOptionsByRuleSetId,
            Map<Long, List<TaxExemptionEntity>> taxExemptionsByRuleSetId) {
        var taxTimeBandEntities = taxTimeBandsByRuleSetId.getOrDefault(taxRuleSetEntity.getId(), List.of());

        if (taxTimeBandEntities.isEmpty()) {
            throw new MissingTaxTimeBandsException(cityCode, taxRuleSetEntity.getEffectiveFrom());
        }

        validateTaxTimeBands(cityCode, taxRuleSetEntity.getEffectiveFrom(), taxTimeBandEntities);

        var taxRuleOptions = mapTaxRuleOptions(
                taxRuleOptionsByRuleSetId.getOrDefault(taxRuleSetEntity.getId(), List.of()),
                Currency.getInstance(taxRuleSetEntity.getCurrencyCode()));

        var taxExemptionEntities = taxExemptionsByRuleSetId.getOrDefault(taxRuleSetEntity.getId(), List.of());
        var taxExemptions = mapTaxExemptions(taxExemptionEntities);
        validateHolidayPrecedingRelation(taxRuleOptions, taxExemptionEntities);

        return TaxRuleSetEntity.toTaxRuleSet(
                taxRuleSetEntity, cityCode, taxTimeBandEntities, taxExemptions, taxRuleOptions);
    }

    private static TaxExemptions mapTaxExemptions(List<TaxExemptionEntity> taxExemptionEntities) {
        for (var entity : taxExemptionEntities) {
            validateTaxExemption(entity);
        }

        var taxExemptions = taxExemptionEntities.stream()
                .<TaxExemption>map(entity -> switch (entity.getType()) {
                    case WEEKDAY -> new WeekdayTaxExemption(DayOfWeek.of(entity.getDayOfWeek()));
                    case MONTH -> new MonthTaxExemption(Month.of(entity.getMonthNumber()));
                    case PUBLIC_HOLIDAY -> new PublicHolidayTaxExemption(entity.getHolidayDate());
                    case VEHICLE_TYPE -> new VehicleTypeTaxExemption(entity.getVehicleTypeCode());
                })
                .toList();

        var uniqueTaxExemptions = new HashSet<TaxExemption>();
        for (var index = 0; index < taxExemptions.size(); index++) {
            if (!uniqueTaxExemptions.add(taxExemptions.get(index))) {
                throw new InvalidTaxExemptionException(
                        taxExemptionEntities.get(index).getType().name());
            }
        }

        return new TaxExemptions(taxExemptions);
    }

    private static void validateTaxExemption(TaxExemptionEntity entity) {
        if (entity.getType() == null) {
            throw new InvalidTaxExemptionException("UNKNOWN");
        }

        var valid =
                switch (entity.getType()) {
                    case WEEKDAY ->
                        entity.getDayOfWeek() != null
                                && entity.getDayOfWeek() >= 1
                                && entity.getDayOfWeek() <= 7
                                && entity.getMonthNumber() == null
                                && entity.getHolidayDate() == null
                                && entity.getVehicleTypeCode() == null;
                    case MONTH ->
                        entity.getDayOfWeek() == null
                                && entity.getMonthNumber() != null
                                && entity.getMonthNumber() >= 1
                                && entity.getMonthNumber() <= 12
                                && entity.getHolidayDate() == null
                                && entity.getVehicleTypeCode() == null;
                    case PUBLIC_HOLIDAY ->
                        entity.getDayOfWeek() == null
                                && entity.getMonthNumber() == null
                                && entity.getHolidayDate() != null
                                && entity.getVehicleTypeCode() == null;
                    case VEHICLE_TYPE ->
                        entity.getDayOfWeek() == null
                                && entity.getMonthNumber() == null
                                && entity.getHolidayDate() == null
                                && entity.getVehicleTypeCode() != null
                                && !entity.getVehicleTypeCode().isBlank();
                };

        if (!valid) {
            throw new InvalidTaxExemptionException(entity.getType().name());
        }
    }

    private static void validateHolidayPrecedingRelation(
            TaxRuleOptions taxRuleOptions, List<TaxExemptionEntity> taxExemptionEntities) {
        if (taxRuleOptions.holidayPreceding().isPresent()
                && taxExemptionEntities.stream()
                        .noneMatch(entity -> entity.getType() == TaxExemptionType.PUBLIC_HOLIDAY)) {
            throw new InvalidTaxExemptionException(TaxExemptionType.PUBLIC_HOLIDAY.name());
        }
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
                    } else if (entity.getType() == TaxRuleOptionType.HOLIDAY_PRECEDING) {
                        validateHolidayPreceding(entity);
                        consumer.accept(new HolidayPreceding(entity.getPrecedingDays()));
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

    private static void validateHolidayPreceding(TaxRuleOptionEntity entity) {
        if (entity.getPrecedingDays() == null
                || entity.getPrecedingDays() <= 0
                || entity.getAmount() != null
                || entity.getDurationMinutes() != null) {
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
