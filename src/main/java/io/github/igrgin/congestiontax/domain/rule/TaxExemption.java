package io.github.igrgin.congestiontax.domain.rule;

public sealed interface TaxExemption
        permits MonthTaxExemption, PublicHolidayTaxExemption, VehicleTypeTaxExemption, WeekdayTaxExemption {}
