package io.github.igrgin.congestiontax.calculation.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CalculationProblemResponse(
        @Schema(description = "Short problem summary.") String title,

        @Schema(description = "HTTP status code.", example = "400")
        int status,

        @Schema(description = "Safe problem explanation.") String detail,

        @Schema(description = "Stable application problem code.", example = "INVALID_REQUEST")
        String code,

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @ArraySchema(
                arraySchema = @Schema(description = "Specific request errors when they are available."),
                schema = @Schema(implementation = RequestError.class))
        List<RequestError> errors) {

    public static CalculationProblemResponse forInvalidRequest() {
        return new CalculationProblemResponse(
                "Invalid calculation request",
                400,
                "The calculation request is invalid.",
                "INVALID_REQUEST",
                List.of());
    }

    public static CalculationProblemResponse forUnknownCity() {
        return new CalculationProblemResponse(
                "City not found", 404, "The selected City does not exist.", "CITY_NOT_FOUND", List.of());
    }

    public static CalculationProblemResponse forUnsupportedPassageYears(List<Integer> passageIndexes) {
        var errors = passageIndexes.stream().map(RequestError::unsupportedYear).toList();

        return invalidPassages(errors);
    }

    public static CalculationProblemResponse forInvalidPassageTimestamp(int passageIndex) {
        return invalidPassages(List.of(RequestError.invalidTimestamp(passageIndex)));
    }

    public static CalculationProblemResponse forInvalidJson() {
        return new CalculationProblemResponse(
                "Invalid JSON", 400, "The request body is not valid JSON.", "INVALID_JSON", List.of());
    }

    public static CalculationProblemResponse forCalculationFailure() {
        return new CalculationProblemResponse(
                "Calculation failed", 500, "The calculation could not be completed.", "CALCULATION_FAILED", List.of());
    }

    public static CalculationProblemResponse forServiceUnavailable() {
        return new CalculationProblemResponse(
                "Service unavailable",
                503,
                "The calculation service is temporarily unavailable.",
                "SERVICE_UNAVAILABLE",
                List.of());
    }

    private static CalculationProblemResponse invalidPassages(List<RequestError> errors) {
        return new CalculationProblemResponse(
                "Invalid calculation request",
                400,
                "The request contains invalid Passages.",
                "INVALID_REQUEST",
                errors);
    }

    public record RequestError(
            @Schema(description = "Request field or Passage index.", example = "passages[1]")
            String field,

            @Schema(description = "Stable request error code.", example = "INVALID_PASSAGE_TIMESTAMP")
            String code,

            @Schema(description = "Request error explanation.")
            String message) {

        private static RequestError unsupportedYear(int passageIndex) {
            return new RequestError(
                    field(passageIndex), "UNSUPPORTED_PASSAGE_YEAR", "A Passage City Local Time date must be in 2013.");
        }

        private static RequestError invalidTimestamp(int passageIndex) {
            return new RequestError(
                    field(passageIndex),
                    "INVALID_PASSAGE_TIMESTAMP",
                    "A Passage must use the City Local Time format uuuu-MM-dd HH:mm:ss.");
        }

        private static String field(int passageIndex) {
            return "passages[" + passageIndex + "]";
        }
    }
}
