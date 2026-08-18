package io.github.igrgin.congestiontax.calculation.http;

import io.github.igrgin.congestiontax.calculation.CalculationService;
import io.github.igrgin.congestiontax.calculation.http.dto.CalculationProblemResponse;
import io.github.igrgin.congestiontax.calculation.http.dto.CalculationRequest;
import io.github.igrgin.congestiontax.calculation.http.dto.CalculationResponse;
import io.github.igrgin.congestiontax.calculation.model.CalculationCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CalculationController {

    private static final String INVALID_REQUEST_EXAMPLE = """
            {
              "title": "Invalid calculation request",
              "status": 400,
              "detail": "The calculation request is invalid.",
              "code": "INVALID_REQUEST"
            }
            """;
    private static final String INVALID_JSON_EXAMPLE = """
            {
              "title": "Invalid JSON",
              "status": 400,
              "detail": "The request body is not valid JSON.",
              "code": "INVALID_JSON"
            }
            """;
    private static final String INVALID_PASSAGE_TIMESTAMP_EXAMPLE = """
            {
              "title": "Invalid calculation request",
              "status": 400,
              "detail": "The request contains invalid Passages.",
              "code": "INVALID_REQUEST",
              "errors": [
                {
                  "field": "passages[1]",
                  "code": "INVALID_PASSAGE_TIMESTAMP",
                  "message": "A Passage must use the City Local Time format uuuu-MM-dd HH:mm:ss."
                }
              ]
            }
            """;
    private static final String UNSUPPORTED_PASSAGE_YEAR_EXAMPLE = """
            {
              "title": "Invalid calculation request",
              "status": 400,
              "detail": "The request contains invalid Passages.",
              "code": "INVALID_REQUEST",
              "errors": [
                {
                  "field": "passages[0]",
                  "code": "UNSUPPORTED_PASSAGE_YEAR",
                  "message": "A Passage City Local Time date must be in 2013."
                }
              ]
            }
            """;
    private static final String UNKNOWN_CITY_EXAMPLE = """
            {
              "title": "City not found",
              "status": 404,
              "detail": "The selected City does not exist.",
              "code": "CITY_NOT_FOUND"
            }
            """;
    private static final String CALCULATION_FAILED_EXAMPLE = """
            {
              "title": "Calculation failed",
              "status": 500,
              "detail": "The calculation could not be completed.",
              "code": "CALCULATION_FAILED"
            }
            """;
    private static final String SERVICE_UNAVAILABLE_EXAMPLE = """
            {
              "title": "Service unavailable",
              "status": 503,
              "detail": "The calculation service is temporarily unavailable.",
              "code": "SERVICE_UNAVAILABLE"
            }
            """;

    private final CalculationService calculationService;

    @Operation(
            summary = "Calculate congestion tax",
            description = "Calculates the Congestion Tax for one Vehicle Type and one or more Passages in one City.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Congestion Tax Calculation completed.",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = CalculationResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "The request is invalid or the Vehicle Type does not exist.",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                schema = @Schema(implementation = CalculationProblemResponse.class),
                                examples = {
                                    @ExampleObject(
                                            name = "invalidRequest",
                                            summary = "Invalid request data",
                                            value = INVALID_REQUEST_EXAMPLE),
                                    @ExampleObject(
                                            name = "invalidJson",
                                            summary = "Malformed JSON",
                                            value = INVALID_JSON_EXAMPLE),
                                    @ExampleObject(
                                            name = "invalidPassageTimestamp",
                                            summary = "Invalid Passage timestamp",
                                            value = INVALID_PASSAGE_TIMESTAMP_EXAMPLE),
                                    @ExampleObject(
                                            name = "unsupportedPassageYear",
                                            summary = "Unsupported Passage year",
                                            value = UNSUPPORTED_PASSAGE_YEAR_EXAMPLE)
                                })),
        @ApiResponse(
                responseCode = "404",
                description = "The City does not exist.",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                schema = @Schema(implementation = CalculationProblemResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "unknownCity",
                                                summary = "Unknown City",
                                                value = UNKNOWN_CITY_EXAMPLE))),
        @ApiResponse(
                responseCode = "500",
                description = "The calculation failed.",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                schema = @Schema(implementation = CalculationProblemResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "calculationFailed",
                                                summary = "Calculation failure",
                                                value = CALCULATION_FAILED_EXAMPLE))),
        @ApiResponse(
                responseCode = "503",
                description = "The calculation service is temporarily unavailable.",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                schema = @Schema(implementation = CalculationProblemResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "serviceUnavailable",
                                                summary = "PostgreSQL unavailable",
                                                value = SERVICE_UNAVAILABLE_EXAMPLE)))
    })
    @PostMapping("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations")
    public CalculationResponse calculate(
            @Parameter(description = "City code that selects the stored Tax Rule Set.", example = "gothenburg")
                    @PathVariable
                    String cityCode,
            @Valid @RequestBody CalculationRequest request) {
        var command = new CalculationCommand(cityCode, request.vehicleType(), request.passages());
        var calculatedTax = calculationService.calculate(command);

        return CalculationResponse.from(calculatedTax);
    }
}
