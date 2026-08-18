package io.github.igrgin.congestiontax.domain.rule;

import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.calculation.TaxExemptionReason;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode
public final class TaxExemptions {

    private final List<TaxExemption> exemptions;

    public TaxExemptions(@NonNull List<TaxExemption> exemptions) {
        this.exemptions = List.copyOf(exemptions);
        if (new HashSet<>(this.exemptions).size() != this.exemptions.size()) {
            throw new IllegalArgumentException("Tax Exemptions must not contain duplicate values.");
        }
    }

    public static TaxExemptions empty() {
        return new TaxExemptions(List.of());
    }

    Set<TaxExemptionReason> reasonsFor(
            VehicleType vehicleType, LocalDate date, Optional<HolidayPreceding> holidayPreceding) {
        var reasons = EnumSet.noneOf(TaxExemptionReason.class);

        for (var exemption : exemptions) {
            if (exemption instanceof VehicleTypeTaxExemption vehicleTypeExemption
                    && vehicleTypeExemption.vehicleTypeCode().equals(vehicleType.code())) {
                reasons.add(TaxExemptionReason.VEHICLE_TYPE);
            } else if (exemption instanceof WeekdayTaxExemption weekdayExemption
                    && weekdayExemption.dayOfWeek() == date.getDayOfWeek()) {
                reasons.add(TaxExemptionReason.WEEKDAY);
            } else if (exemption instanceof MonthTaxExemption monthExemption
                    && monthExemption.month() == date.getMonth()) {
                reasons.add(TaxExemptionReason.MONTH);
            } else if (exemption instanceof PublicHolidayTaxExemption publicHolidayExemption) {
                addPublicHolidayReasons(reasons, date, publicHolidayExemption.date(), holidayPreceding);
            }
        }

        return Collections.unmodifiableSet(reasons);
    }

    private static void addPublicHolidayReasons(
            EnumSet<TaxExemptionReason> reasons,
            LocalDate date,
            LocalDate publicHoliday,
            Optional<HolidayPreceding> holidayPreceding) {
        if (date.equals(publicHoliday)) {
            reasons.add(TaxExemptionReason.PUBLIC_HOLIDAY);
        }

        holidayPreceding.ifPresent(option -> {
            var firstPrecedingDate = publicHoliday.minusDays(option.calendarDateCount());
            if (!date.isBefore(firstPrecedingDate) && date.isBefore(publicHoliday)) {
                reasons.add(TaxExemptionReason.DATE_BEFORE_PUBLIC_HOLIDAY);
            }
        });
    }
}
