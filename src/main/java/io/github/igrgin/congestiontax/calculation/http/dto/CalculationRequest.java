package io.github.igrgin.congestiontax.calculation.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CalculationRequest(
        @NotBlank String vehicleType, @NotEmpty List<@NotNull String> passages) {}
