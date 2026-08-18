package io.github.igrgin.congestiontax.taxrule.model;

import io.github.igrgin.congestiontax.domain.rule.TaxRuleSet;
import java.time.ZoneId;
import lombok.NonNull;

public record CityTaxRuleSet(
        @NonNull ZoneId cityTimeZone, @NonNull TaxRuleSet taxRuleSet) {}
