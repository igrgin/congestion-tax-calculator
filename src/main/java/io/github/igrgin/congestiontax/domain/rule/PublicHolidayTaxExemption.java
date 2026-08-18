package io.github.igrgin.congestiontax.domain.rule;

import java.time.LocalDate;
import lombok.NonNull;

public record PublicHolidayTaxExemption(@NonNull LocalDate date) implements TaxExemption {}
