package io.github.igrgin.congestiontax.calculation.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.igrgin.congestiontax.calculation.CalculationService;
import io.github.igrgin.congestiontax.calculation.exception.InvalidPassageTimestampException;
import io.github.igrgin.congestiontax.calculation.exception.UnsupportedPassageYearException;
import io.github.igrgin.congestiontax.calculation.model.CalculatedTax;
import io.github.igrgin.congestiontax.calculation.model.CalculationCommand;
import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.calculation.CalculationResult;
import io.github.igrgin.congestiontax.domain.calculation.DailyTax;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Currency;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

        given(calculationService.calculate(
                        new CalculationCommand(cityCode, vehicleType.code(), List.of("2013-02-08 06:20:27"))))
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
                .andExpect(status().isBadRequest());
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
                        cityCode, vehicleType.code(), List.of("2013-02-08 05:20:27", "2013-02-08 06:20:27"))))
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

        given(calculationService.calculate(new CalculationCommand(cityCode, vehicleType.code(), List.of(timestamp))))
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
        var passageTimestamps =
                List.of("2012-12-31 23:59:59", "2013-06-15 12:00:00", "2014-01-01 00:00:00", "2020-02-29 18:30:00");
        var command = new CalculationCommand(cityCode, "OTHER", passageTimestamps);

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

    @ParameterizedTest
    @ValueSource(strings = {"2013-02-08T05:20:27Z", "2013-02-08T06:20:27", "2013-02-08 06:20", "2013-02-30 06:20:27"})
    void rejectsInvalidPassageTimestamp(String timestamp) throws Exception {
        var command = new CalculationCommand("gothenburg", "OTHER", List.of(timestamp));

        given(calculationService.calculate(command))
                .willThrow(new InvalidPassageTimestampException(0, new IllegalArgumentException()));

        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", "gothenburg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "OTHER",
                                  "passages": [
                                    "%s"
                                  ]
                                }
                                """.formatted(timestamp)))
                .andExpect(status().isBadRequest());

        verify(calculationService).calculate(command);
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
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", "gothenburg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsInternalServerErrorForUnexpectedFailure() throws Exception {
        given(calculationService.calculate(any(CalculationCommand.class)))
                .willThrow(new IllegalStateException("Unexpected failure."));

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
                .andExpect(content().string(""));
    }
}
