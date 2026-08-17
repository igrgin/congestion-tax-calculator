package io.github.igrgin.congestiontax.domain.calculation;

import java.time.Instant;
import java.time.LocalDateTime;

public record LocalizedPassage(Instant occurredAt, LocalDateTime cityDateTime) {}
