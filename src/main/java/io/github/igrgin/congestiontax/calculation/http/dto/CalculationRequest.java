package io.github.igrgin.congestiontax.calculation.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import tools.jackson.databind.annotation.JsonDeserialize;

public record CalculationRequest(
        @NotBlank String vehicleType,

        @NotEmpty @JsonDeserialize(contentUsing = PassageCityDateTimeDeserializer.class)
        List<@NotNull LocalDateTime> passages) {}
