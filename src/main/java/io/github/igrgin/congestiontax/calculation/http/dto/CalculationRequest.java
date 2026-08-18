package io.github.igrgin.congestiontax.calculation.http.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import tools.jackson.databind.annotation.JsonDeserialize;

@Schema(
        description = "One Congestion Tax Calculation request.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CalculationRequest(
        @NotBlank
        @Schema(
                description = "Database-defined Vehicle Type code.",
                example = "OTHER",
                minLength = 1,
                pattern = "^.*\\S.*$")
        String vehicleType,

        @NotEmpty
        @JsonDeserialize(contentUsing = PassageCityDateTimeDeserializer.class)
        @ArraySchema(
                arraySchema = @Schema(description = "One or more Passages for one vehicle."),
                schema =
                        @Schema(
                                type = "string",
                                description =
                                        "Passage timestamp in exact uuuu-MM-dd HH:mm:ss City Local Time format. The date must be in 2013.",
                                example = "2013-02-08 06:20:27",
                                pattern =
                                        "^2013-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12][0-9]|3[01]) (?:[01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$"),
                minItems = 1)
        List<@NotNull LocalDateTime> passages) {}
