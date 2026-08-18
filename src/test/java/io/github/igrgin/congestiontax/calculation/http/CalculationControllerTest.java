package io.github.igrgin.congestiontax.calculation.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.igrgin.congestiontax.calculation.CalculationService;
import io.github.igrgin.congestiontax.calculation.exception.InvalidStoredTaxExemptionException;
import io.github.igrgin.congestiontax.calculation.model.CalculatedTax;
import io.github.igrgin.congestiontax.calculation.model.CalculationCommand;
import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.calculation.CalculationResult;
import io.github.igrgin.congestiontax.domain.calculation.DailyTax;
import io.github.igrgin.congestiontax.domain.calculation.TaxExemptionReason;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.Currency;
import java.util.EnumSet;
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
import org.springframework.test.web.servlet.ResultActions;

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

        performOnePassageOtherRequest()
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

    @Test
    void reportsAllTaxExemptionReasonsForExemptDailyTax() throws Exception {
        var cityCode = "gothenburg";
        var vehicleType = new VehicleType("OTHER", "Other vehicle");
        var cityDateTime = LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27);
        var calculationDate = cityDateTime.toLocalDate();
        var zero = TaxAmount.zero(Currency.getInstance("SEK"));
        var taxExemptionReasons = Collections.unmodifiableSet(EnumSet.allOf(TaxExemptionReason.class));
        var calculationResult = new CalculationResult(
                vehicleType, List.of(new DailyTax(calculationDate, taxExemptionReasons, zero)), zero);

        given(calculationService.calculate(new CalculationCommand(cityCode, vehicleType.code(), List.of(cityDateTime))))
                .willReturn(new CalculatedTax(cityCode, calculationResult));

        performOnePassageOtherRequest()
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "cityCode": "gothenburg",
                          "vehicleType": "OTHER",
                          "currency": "SEK",
                          "totalAmount": 0.00,
                          "dailyTaxes": [
                            {
                              "date": "2013-02-08",
                              "taxExemptionReasons": [
                                "VEHICLE_TYPE",
                                "WEEKDAY",
                                "MONTH",
                                "PUBLIC_HOLIDAY",
                                "DATE_BEFORE_PUBLIC_HOLIDAY"
                              ],
                              "amount": 0.00
                            }
                          ]
                        }
                        """))
                .andExpect(
                        jsonPath("$.dailyTaxes[0].taxExemptionReasons.length()").value(5))
                .andExpect(jsonPath("$.dailyTaxes[0].taxExemptionReasons[0]").value("VEHICLE_TYPE"))
                .andExpect(jsonPath("$.dailyTaxes[0].taxExemptionReasons[1]").value("WEEKDAY"))
                .andExpect(jsonPath("$.dailyTaxes[0].taxExemptionReasons[2]").value("MONTH"))
                .andExpect(jsonPath("$.dailyTaxes[0].taxExemptionReasons[3]").value("PUBLIC_HOLIDAY"))
                .andExpect(jsonPath("$.dailyTaxes[0].taxExemptionReasons[4]").value("DATE_BEFORE_PUBLIC_HOLIDAY"));
    }

    private ResultActions performOnePassageOtherRequest() throws Exception {
        return mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", "gothenburg")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "vehicleType": "OTHER",
                          "passages": [
                            "2013-02-08 06:20:27"
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
        var firstPassage = LocalDateTime.of(2013, Month.FEBRUARY, 8, 5, 20, 27);
        var secondPassage = LocalDateTime.of(2013, Month.FEBRUARY, 8, 6, 20, 27);
        var calculationDate = LocalDate.of(2013, Month.FEBRUARY, 8);
        var currency = Currency.getInstance("SEK");
        var taxAmount = new TaxAmount(new BigDecimal("16.00"), currency);
        var calculationResult =
                new CalculationResult(vehicleType, List.of(new DailyTax(calculationDate, taxAmount)), taxAmount);

        given(calculationService.calculate(
                        new CalculationCommand(cityCode, vehicleType.code(), List.of(firstPassage, secondPassage))))
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
    @ValueSource(strings = {"2013-02-08T05:20:27Z", "2013-02-08T06:20:27", "2013-02-08 06:20", "2013-02-30 06:20:27"})
    void rejectsInvalidPassageTimestamp(String timestamp) throws Exception {
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

    @Test
    void returnsSafeInternalServerErrorForInvalidStoredTaxExemption() throws Exception {
        var cause = new IllegalStateException("Sensitive stored value.");
        given(calculationService.calculate(any(CalculationCommand.class)))
                .willThrow(new InvalidStoredTaxExemptionException("PUBLIC_HOLIDAY", cause));

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
