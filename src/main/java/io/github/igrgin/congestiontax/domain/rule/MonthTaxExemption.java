package io.github.igrgin.congestiontax.domain.rule;

import java.time.Month;
import lombok.NonNull;

public record MonthTaxExemption(@NonNull Month month) implements TaxExemption {}
