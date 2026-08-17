package io.github.igrgin.congestiontax.taxrule.model;

import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

public record ApplicableTaxRuleSets(ZoneId cityTimeZone, Map<LocalDate, TaxRuleSet> taxRuleSetsByCalculationDate) {}
