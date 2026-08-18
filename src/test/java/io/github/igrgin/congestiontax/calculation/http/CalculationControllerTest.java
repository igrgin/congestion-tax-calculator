package io.github.igrgin.congestiontax.calculation.http;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.igrgin.congestiontax.calculation.CalculationService;
import io.github.igrgin.congestiontax.calculation.exception.CityNotFoundException;
import io.github.igrgin.congestiontax.calculation.exception.InvalidStoredTaxRuleOptionException;
import io.github.igrgin.congestiontax.calculation.exception.MissingStoredTaxRuleSetException;
import io.github.igrgin.congestiontax.calculation.exception.UnsupportedPassageYearException;
import io.github.igrgin.congestiontax.calculation.exception.VehicleTypeNotFoundException;
import io.github.igrgin.congestiontax.calculation.model.CalculatedTax;
import io.github.igrgin.congestiontax.calculation.model.CalculationCommand;
import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.calculation.CalculationResult;
import io.github.igrgin.congestiontax.domain.calculation.DailyTax;
import io.github.igrgin.congestiontax.taxrule.exception.OverlappingTaxTimeBandsException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Currency;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@WebMvcTest(CalculationController.class)
class CalculationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalculationService calculationService;

    @Test
    void calculatesTaxForOnePassage() throws Exception {
        var cityCode = "gothenburg";
        var vehicleType = new VehicleType("OTHER", "Other vehicle");
        var cityDateTime = LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27);
        var calculationDate = LocalDate.of(2013, Month.FEBRUARY, 8);
        var currency = Currency.getInstance("SEK");
        var taxAmount = new TaxAmount(new BigDecimal("8.00"), currency);
        var calculationResult =
                new CalculationResult(vehicleType, List.of(new DailyTax(calculationDate, taxAmount)), taxAmount);

        given(calculationService.calculate(new CalculationCommand(cityCode, vehicleType.code(), List.of(cityDateTime))))
                .willReturn(new CalculatedTax(cityCode, calculationResult));

        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", cityCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "OTHER",
                                  "passages": [
                                    "2013-02-08 06:20:27"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "cityCode": "gothenburg",
                          "vehicleType": "OTHER",
                          "currency": "SEK",
                          "totalAmount": 8.00,
                          "dailyTaxes": [
                            {
                              "date": "2013-02-08",
                              "taxExemptionReasons": [],
                              "amount": 8.00
                            }
                          ]
                        }
                        """));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void rejectsInvalidRequest(String scenario, String requestBody) throws Exception {
        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", "gothenburg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title", not(emptyString())))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail", not(emptyString())))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errors").doesNotExist());

        verifyNoInteractions(calculationService);
    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("empty Passage list", """
                        {
                          "vehicleType": "OTHER",
                          "passages": []
                        }
                        """),
                Arguments.of("null Vehicle Type", """
                        {
                          "vehicleType": null,
                          "passages": [
                            "2013-02-08 06:20:27"
                          ]
                        }
                        """),
                Arguments.of("blank Vehicle Type", """
                        {
                          "vehicleType": " ",
                          "passages": [
                            "2013-02-08 06:20:27"
                          ]
                        }
                        """),
                Arguments.of("null Passage list", """
                        {
                          "vehicleType": "OTHER",
                          "passages": null
                        }
                        """),
                Arguments.of("null Passage", """
                        {
                          "vehicleType": "OTHER",
                          "passages": [
                            null
                          ]
                        }
                        """));
    }

    @Test
    void calculatesTaxForSeveralPassages() throws Exception {
        var cityCode = "gothenburg";
        var vehicleType = new VehicleType("OTHER", "Other vehicle");
        var calculationDate = LocalDate.of(2013, Month.FEBRUARY, 8);
        var currency = Currency.getInstance("SEK");
        var taxAmount = new TaxAmount(new BigDecimal("16.00"), currency);
        var calculationResult =
                new CalculationResult(vehicleType, List.of(new DailyTax(calculationDate, taxAmount)), taxAmount);

        given(calculationService.calculate(new CalculationCommand(
                        cityCode,
                        vehicleType.code(),
                        List.of(
                                LocalDateTime.of(2013, Month.FEBRUARY, 8, 5, 20, 27),
                                LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27)))))
                .willReturn(new CalculatedTax(cityCode, calculationResult));

        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", cityCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "OTHER",
                                  "passages": [
                                    "2013-02-08 05:20:27",
                                    "2013-02-08 06:20:27"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "cityCode": "gothenburg",
                          "vehicleType": "OTHER",
                          "currency": "SEK",
                          "totalAmount": 16.00,
                          "dailyTaxes": [
                            {
                              "date": "2013-02-08",
                              "taxExemptionReasons": [],
                              "amount": 16.00
                            }
                          ]
                        }
                        """));
    }

    @ParameterizedTest
    @MethodSource("supportedYearBoundaries")
    void acceptsPassageAtSupportedYearBoundary(String timestamp, LocalDateTime cityDateTime) throws Exception {
        var cityCode = "gothenburg";
        var vehicleType = new VehicleType("OTHER", "Other vehicle");
        var currency = Currency.getInstance("SEK");
        var taxAmount = TaxAmount.zero(currency);
        var calculationResult = new CalculationResult(
                vehicleType, List.of(new DailyTax(cityDateTime.toLocalDate(), taxAmount)), taxAmount);

        given(calculationService.calculate(new CalculationCommand(cityCode, vehicleType.code(), List.of(cityDateTime))))
                .willReturn(new CalculatedTax(cityCode, calculationResult));

        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", cityCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "OTHER",
                                  "passages": [
                                    "%s"
                                  ]
                                }
                                """.formatted(timestamp)))
                .andExpect(status().isOk());
    }

    private static Stream<Arguments> supportedYearBoundaries() {
        return Stream.of(
                Arguments.of("2013-01-01 00:00:00", LocalDateTime.of(2013, Month.JANUARY, 1, 0, 0, 0)),
                Arguments.of("2013-12-31 23:59:59", LocalDateTime.of(2013, Month.DECEMBER, 31, 23, 59, 59)));
    }

    @Test
    void rejectsEveryPassageOutsideSupportedYear() throws Exception {
        var cityCode = "gothenburg";
        var passageCityDateTimes = List.of(
                LocalDateTime.of(2012, Month.DECEMBER, 31, 23, 59, 59),
                LocalDateTime.of(2013, Month.JUNE, 15, 12, 0),
                LocalDateTime.of(2014, Month.JANUARY, 1, 0, 0),
                LocalDateTime.of(2020, Month.FEBRUARY, 29, 18, 30));
        var command = new CalculationCommand(cityCode, "OTHER", passageCityDateTimes);

        given(calculationService.calculate(command)).willThrow(new UnsupportedPassageYearException(List.of(0, 2, 3)));

        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", cityCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "OTHER",
                                  "passages": [
                                    "2012-12-31 23:59:59",
                                    "2013-06-15 12:00:00",
                                    "2014-01-01 00:00:00",
                                    "2020-02-29 18:30:00"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid calculation request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("The request contains invalid Passages."))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[0].field").value("passages[0]"))
                .andExpect(jsonPath("$.errors[0].code").value("UNSUPPORTED_PASSAGE_YEAR"))
                .andExpect(jsonPath("$.errors[0].message").value("A Passage City Local Time date must be in 2013."))
                .andExpect(jsonPath("$.errors[1].field").value("passages[2]"))
                .andExpect(jsonPath("$.errors[1].code").value("UNSUPPORTED_PASSAGE_YEAR"))
                .andExpect(jsonPath("$.errors[1].message").value("A Passage City Local Time date must be in 2013."))
                .andExpect(jsonPath("$.errors[2].field").value("passages[3]"))
                .andExpect(jsonPath("$.errors[2].code").value("UNSUPPORTED_PASSAGE_YEAR"))
                .andExpect(jsonPath("$.errors[2].message").value("A Passage City Local Time date must be in 2013."));

        verify(calculationService).calculate(command);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPassageValues")
    void rejectsInvalidPassageValue(String scenario, String passagesJson) throws Exception {
        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", "gothenburg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "OTHER",
                                  "passages": %s
                                }
                                """.formatted(passagesJson)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(calculationService);
    }

    private static Stream<Arguments> invalidPassageValues() {
        return Stream.of(
                Arguments.of("UTC offset", "[\"2013-02-08 06:20:27Z\"]"),
                Arguments.of("numeric offset", "[\"2013-02-08 06:20:27+01:00\"]"),
                Arguments.of("T separator", "[\"2013-02-08T06:20:27\"]"),
                Arguments.of("missing seconds", "[\"2013-02-08 06:20\"]"),
                Arguments.of("invalid calendar date", "[\"2013-02-30 06:20:27\"]"),
                Arguments.of("leading space", "[\" 2013-02-08 06:20:27\"]"),
                Arguments.of("trailing space", "[\"2013-02-08 06:20:27 \"]"),
                Arguments.of("number", "[20130208062027]"),
                Arguments.of("Boolean", "[true]"),
                Arguments.of("array form", "[[2013, 2, 8, 6, 20, 27]]"));
    }

    @Test
    void reportsFirstInvalidPassageTimestamp() throws Exception {
        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", "gothenburg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "OTHER",
                                  "passages": [
                                    "2013-02-08 06:20:27",
                                    "invalid",
                                    "also invalid"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid calculation request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("The request contains invalid Passages."))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("passages[1]"))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_PASSAGE_TIMESTAMP"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("A Passage must use the City Local Time format uuuu-MM-dd HH:mm:ss."));

        verifyNoInteractions(calculationService);
    }

    @Test
    void rejectsRemovedTimeZoneProperty() throws Exception {
        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", "gothenburg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "OTHER",
                                  "timeZone": "Europe/Stockholm",
                                  "passages": [
                                    "2013-02-08 06:20:27"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title", not(emptyString())))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail", not(emptyString())))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errors").doesNotExist());

        verifyNoInteractions(calculationService);
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", "gothenburg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid JSON"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("The request body is not valid JSON."))
                .andExpect(jsonPath("$.code").value("INVALID_JSON"))
                .andExpect(jsonPath("$.errors").doesNotExist());

        verifyNoInteractions(calculationService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("handledClientAndAvailabilityFailures")
    void returnsProblemDetailsForHandledClientAndAvailabilityFailure(
            String scenario, RuntimeException failure, int expectedStatus, String expectedCode) throws Exception {
        given(calculationService.calculate(any(CalculationCommand.class))).willThrow(failure);

        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", "gothenburg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "OTHER",
                                  "passages": [
                                    "2013-02-08 06:20:27"
                                  ]
                                }
                                """))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title", not(emptyString())))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail", not(emptyString())))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    private static Stream<Arguments> handledClientAndAvailabilityFailures() {
        return Stream.of(
                Arguments.of(
                        "unknown City",
                        new CityNotFoundException("unknown", new IllegalArgumentException("Unknown City.")),
                        404,
                        "CITY_NOT_FOUND"),
                Arguments.of(
                        "unknown Vehicle Type",
                        new VehicleTypeNotFoundException(
                                "UNKNOWN", new IllegalArgumentException("Unknown Vehicle Type.")),
                        400,
                        "INVALID_REQUEST"),
                Arguments.of(
                        "PostgreSQL unavailable",
                        new DataAccessResourceFailureException("PostgreSQL unavailable."),
                        503,
                        "SERVICE_UNAVAILABLE"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("internalCalculationFailures")
    void returnsSafeProblemDetailsForInternalCalculationFailure(String scenario, RuntimeException failure)
            throws Exception {
        given(calculationService.calculate(any(CalculationCommand.class))).willThrow(failure);

        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", "gothenburg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "OTHER",
                                  "passages": [
                                    "2013-02-08 06:20:27"
                                  ]
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Calculation failed"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("The calculation could not be completed."))
                .andExpect(jsonPath("$.code").value("CALCULATION_FAILED"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    private static Stream<Arguments> internalCalculationFailures() {
        var storedFailure = new IllegalStateException("Stored content failure.");
        return Stream.of(
                Arguments.of(
                        "invalid stored Tax Rule Option",
                        new InvalidStoredTaxRuleOptionException("CHARGE_WINDOW", storedFailure)),
                Arguments.of("missing Tax Rule Set", new MissingStoredTaxRuleSetException("gothenburg", storedFailure)),
                Arguments.of(
                        "overlapping stored Tax Time Bands",
                        new OverlappingTaxTimeBandsException(
                                "gothenburg",
                                LocalTime.of(6, 0),
                                LocalTime.of(9, 0),
                                LocalTime.of(7, 0),
                                LocalTime.of(8, 0))),
                Arguments.of("unexpected failure", new IllegalStateException("Unexpected failure.")));
    }
}
