package io.github.igrgin.congestiontax.domain.rule;

import java.time.DayOfWeek;
import lombok.NonNull;

public record WeekdayTaxExemption(@NonNull DayOfWeek dayOfWeek) implements TaxExemption {}
