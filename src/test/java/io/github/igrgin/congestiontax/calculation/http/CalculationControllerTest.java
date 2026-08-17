package io.github.igrgin.congestiontax.calculation.http;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.igrgin.congestiontax.calculation.CalculatedTax;
import io.github.igrgin.congestiontax.calculation.CalculationCommand;
import io.github.igrgin.congestiontax.calculation.CalculationService;
import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.VehicleType;
import io.github.igrgin.congestiontax.domain.calculation.CalculationResult;
import io.github.igrgin.congestiontax.domain.calculation.DailyTax;
import io.github.igrgin.congestiontax.domain.calculation.Passage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
        var passage = new Passage(Instant.parse("2013-02-08T05:20:27Z"), LocalDateTime.of(2013, 2, 8, 6, 20, 27));
        var calculationDate = LocalDate.of(2013, 2, 8);
        var currency = Currency.getInstance("SEK");
        var taxAmount = new TaxAmount(new BigDecimal("8.00"), currency);
        var calculationResult =
                new CalculationResult(vehicleType, List.of(new DailyTax(calculationDate, taxAmount)), taxAmount);

        given(calculationService.calculate(new CalculationCommand(cityCode, vehicleType.code(), List.of(passage))))
                .willReturn(new CalculatedTax(cityCode, calculationResult));

        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", cityCode)
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
    void derivesSummerInstantFromRequestTimeZone() throws Exception {
        var cityCode = "gothenburg";
        var vehicleType = new VehicleType("OTHER", "Other vehicle");
        var passage = new Passage(Instant.parse("2013-07-08T04:20:27Z"), LocalDateTime.of(2013, 7, 8, 6, 20, 27));
        var calculationDate = LocalDate.of(2013, 7, 8);
        var currency = Currency.getInstance("SEK");
        var taxAmount = new TaxAmount(new BigDecimal("8.00"), currency);
        var calculationResult =
                new CalculationResult(vehicleType, List.of(new DailyTax(calculationDate, taxAmount)), taxAmount);

        given(calculationService.calculate(new CalculationCommand(cityCode, vehicleType.code(), List.of(passage))))
                .willReturn(new CalculatedTax(cityCode, calculationResult));

        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", cityCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "OTHER",
                                  "timeZone": "Europe/Stockholm",
                                  "passages": [
                                    "2013-07-08 06:20:27"
                                  ]
                                }
                                """))
                .andExpect(status().isOk());
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
                          "timeZone": "Europe/Stockholm",
                          "passages": []
                        }
                        """),
                Arguments.of("null Vehicle Type", """
                        {
                          "vehicleType": null,
                          "timeZone": "Europe/Stockholm",
                          "passages": [
                            "2013-02-08 06:20:27"
                          ]
                        }
                        """),
                Arguments.of("blank Vehicle Type", """
                        {
                          "vehicleType": " ",
                          "timeZone": "Europe/Stockholm",
                          "passages": [
                            "2013-02-08 06:20:27"
                          ]
                        }
                        """),
                Arguments.of("null Passage list", """
                        {
                          "vehicleType": "OTHER",
                          "timeZone": "Europe/Stockholm",
                          "passages": null
                        }
                        """),
                Arguments.of("null Passage", """
                        {
                          "vehicleType": "OTHER",
                          "timeZone": "Europe/Stockholm",
                          "passages": [
                            null
                          ]
                        }
                        """),
                Arguments.of("null time zone", """
                        {
                          "vehicleType": "OTHER",
                          "timeZone": null,
                          "passages": [
                            "2013-02-08 06:20:27"
                          ]
                        }
                        """),
                Arguments.of("blank time zone", """
                        {
                          "vehicleType": "OTHER",
                          "timeZone": " ",
                          "passages": [
                            "2013-02-08 06:20:27"
                          ]
                        }
                        """));
    }

    @Test
    void rejectsMultiplePassages() throws Exception {
        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", "gothenburg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "OTHER",
                                  "timeZone": "Europe/Stockholm",
                                  "passages": [
                                    "2013-02-08 05:20:27",
                                    "2013-02-08 06:20:27"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {"2013-02-08T05:20:27Z", "2013-02-08T06:20:27", "2013-02-08 06:20", "2013-02-30 06:20:27"})
    void rejectsInvalidPassageTimestamp(String timestamp) throws Exception {
        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", "gothenburg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "OTHER",
                                  "timeZone": "Europe/Stockholm",
                                  "passages": [
                                    "%s"
                                  ]
                                }
                                """.formatted(timestamp)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"Mars/Olympus", "+01:00"})
    void rejectsInvalidTimeZone(String timeZone) throws Exception {
        mockMvc.perform(post("/api/v1/cities/{cityCode}" + "/congestion-tax/calculations", "gothenburg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "OTHER",
                                  "timeZone": "%s",
                                  "passages": [
                                    "2013-02-08 06:20:27"
                                  ]
                                }
                                """.formatted(timeZone)))
                .andExpect(status().isBadRequest());
    }
}
