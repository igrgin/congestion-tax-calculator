package io.github.igrgin.congestiontax.domain.calculation;

import java.time.Instant;
import java.time.LocalDateTime;

public record Passage(Instant occurredAt, LocalDateTime cityDateTime) {}
