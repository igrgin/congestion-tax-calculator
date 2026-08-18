package io.github.igrgin.congestiontax.domain.rule;

public sealed interface TaxRuleOption permits ChargeWindow, DailyMaximum, PublicHolidayPrecedingDateOption {}
