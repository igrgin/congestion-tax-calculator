package io.github.igrgin.congestiontax.taxrule;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.rule.ChargeWindow;
import io.github.igrgin.congestiontax.domain.rule.DailyMaximum;
import io.github.igrgin.congestiontax.domain.rule.MonthTaxExemption;
import io.github.igrgin.congestiontax.domain.rule.PublicHolidayPrecedingDateOption;
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
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxRuleSetException;
import io.github.igrgin.congestiontax.taxrule.exception.MissingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.OverlappingTaxTimeBandsException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import io.github.igrgin.congestiontax.taxrule.model.CityTaxRuleSet;
import io.github.igrgin.congestiontax.taxrule.persistence.CityEntity;
import io.github.igrgin.congestiontax.taxrule.persistence.CityRepository;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxExemptionEntity;
import io.github.igrgin.congestiontax.taxrule.persistence.TaxExemptionRepository;
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
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
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
    public CityTaxRuleSet getCityTaxRuleSet(String cityCode) {
        var cityTimeZone = cityRepository
                .findByCode(cityCode)
                .map(CityEntity::getTimeZone)
                .map(timeZone -> parseCityTimeZone(cityCode, timeZone))
                .orElseThrow(() -> new UnknownCityException(cityCode));

        var taxRuleSetEntity = taxRuleSetRepository
                .findByCityCode(cityCode)
                .orElseThrow(() -> new MissingTaxRuleSetException(cityCode));
        var taxTimeBands = taxTimeBandRepository.findByRuleSetId(taxRuleSetEntity.getId());
        var taxRuleOptions = taxRuleOptionRepository.findByRuleSetId(taxRuleSetEntity.getId());
        var taxExemptions = taxExemptionRepository.findByRuleSetId(taxRuleSetEntity.getId());
        var taxRuleSet = mapCompleteTaxRuleSet(cityCode, taxRuleSetEntity, taxTimeBands, taxRuleOptions, taxExemptions);

        log.debug(
                "Loaded City Tax Rule Set. cityCode={} chargeWindowEnabled={} dailyMaximumEnabled={}",
                cityCode,
                taxRuleSet.taxRuleOptions().chargeWindow().isPresent(),
                taxRuleSet.taxRuleOptions().dailyMaximum().isPresent());

        return new CityTaxRuleSet(cityTimeZone, taxRuleSet);
    }

    private static ZoneId parseCityTimeZone(String cityCode, String timeZone) {
        if (!ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw new InvalidCityTimeZoneException(cityCode);
        }

        return ZoneId.of(timeZone);
    }

    private static TaxRuleSet mapCompleteTaxRuleSet(
            String cityCode,
            TaxRuleSetEntity taxRuleSetEntity,
            List<TaxTimeBandEntity> taxTimeBandEntities,
            List<TaxRuleOptionEntity> taxRuleOptionEntities,
            List<TaxExemptionEntity> taxExemptionEntities) {
        if (taxTimeBandEntities.isEmpty()) {
            throw new MissingTaxTimeBandsException(cityCode);
        }

        validateTaxTimeBands(cityCode, taxTimeBandEntities);

        var taxRuleOptions =
                mapTaxRuleOptions(taxRuleOptionEntities, Currency.getInstance(taxRuleSetEntity.getCurrencyCode()));
        var taxExemptions = mapTaxExemptions(taxExemptionEntities);

        return TaxRuleSetEntity.toTaxRuleSet(
                taxRuleSetEntity, cityCode, taxTimeBandEntities, taxExemptions, taxRuleOptions);
    }

    private static TaxExemptions mapTaxExemptions(List<TaxExemptionEntity> taxExemptionEntities) {
        var taxExemptions = taxExemptionEntities.stream()
                .map(TaxRuleServiceImpl::mapTaxExemption)
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

    private static TaxExemption mapTaxExemption(TaxExemptionEntity entity) {
        if (entity.getType() == null) {
            throw new InvalidTaxExemptionException("UNKNOWN");
        }

        return switch (entity.getType()) {
            case WEEKDAY -> {
                if (entity.getDayOfWeek() == null
                        || entity.getDayOfWeek() < 1
                        || entity.getDayOfWeek() > 7
                        || entity.getMonthNumber() != null
                        || entity.getHolidayDate() != null
                        || entity.getVehicleTypeCode() != null) {
                    throw new InvalidTaxExemptionException(entity.getType().name());
                }
                yield new WeekdayTaxExemption(DayOfWeek.of(entity.getDayOfWeek()));
            }
            case MONTH -> {
                if (entity.getDayOfWeek() != null
                        || entity.getMonthNumber() == null
                        || entity.getMonthNumber() < 1
                        || entity.getMonthNumber() > 12
                        || entity.getHolidayDate() != null
                        || entity.getVehicleTypeCode() != null) {
                    throw new InvalidTaxExemptionException(entity.getType().name());
                }
                yield new MonthTaxExemption(Month.of(entity.getMonthNumber()));
            }
            case PUBLIC_HOLIDAY -> {
                if (entity.getDayOfWeek() != null
                        || entity.getMonthNumber() != null
                        || entity.getHolidayDate() == null
                        || entity.getVehicleTypeCode() != null) {
                    throw new InvalidTaxExemptionException(entity.getType().name());
                }
                yield new PublicHolidayTaxExemption(entity.getHolidayDate());
            }
            case VEHICLE_TYPE -> {
                if (entity.getDayOfWeek() != null
                        || entity.getMonthNumber() != null
                        || entity.getHolidayDate() != null
                        || entity.getVehicleTypeCode() == null
                        || entity.getVehicleTypeCode().isBlank()) {
                    throw new InvalidTaxExemptionException(entity.getType().name());
                }
                yield new VehicleTypeTaxExemption(entity.getVehicleTypeCode());
            }
        };
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
                        validatePublicHolidayPrecedingDateOption(entity);
                        consumer.accept(new PublicHolidayPrecedingDateOption(entity.getPrecedingDays()));
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

    private static void validatePublicHolidayPrecedingDateOption(TaxRuleOptionEntity entity) {
        if (entity.getPrecedingDays() == null
                || entity.getPrecedingDays() <= 0
                || entity.getAmount() != null
                || entity.getDurationMinutes() != null) {
            throw new InvalidTaxRuleOptionException(entity.getType().name());
        }
    }

    private static void validateTaxTimeBands(String cityCode, List<TaxTimeBandEntity> taxTimeBands) {
        for (var firstIndex = 0; firstIndex < taxTimeBands.size(); firstIndex++) {
            for (var secondIndex = firstIndex + 1; secondIndex < taxTimeBands.size(); secondIndex++) {
                var first = taxTimeBands.get(firstIndex);
                var second = taxTimeBands.get(secondIndex);
                if (!overlaps(first, second)) {
                    continue;
                }

                throw new OverlappingTaxTimeBandsException(
                        cityCode, first.getStartTime(), first.getEndTime(), second.getStartTime(), second.getEndTime());
            }
        }
    }

    private static boolean overlaps(TaxTimeBandEntity first, TaxTimeBandEntity second) {
        return includes(first, second.getStartTime()) || includes(second, first.getStartTime());
    }

    private static boolean includes(TaxTimeBandEntity taxTimeBand, LocalTime time) {
        if (taxTimeBand.getEndTime().isAfter(taxTimeBand.getStartTime())) {
            return !time.isBefore(taxTimeBand.getStartTime()) && time.isBefore(taxTimeBand.getEndTime());
        }

        return !time.isBefore(taxTimeBand.getStartTime()) || time.isBefore(taxTimeBand.getEndTime());
    }
}
