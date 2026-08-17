package io.github.igrgin.congestiontax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("itest")
@AutoConfigureMetrics
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class CalculationITest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void calculatesOnePassageFromStoredTaxRules() throws Exception {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var request = new HttpEntity<>("""
                {
                  "vehicleType": "OTHER",
                  "timeZone": "Europe/Stockholm",
                  "passages": [
                    "2013-02-08 06:20:27"
                  ]
                }
                """, headers);

        var response = restTemplate.postForEntity(
                "/api/v1/cities/gothenburg" + "/congestion-tax/calculations", request, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(objectMapper.readTree("""
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

        var metricsResponse = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(metricsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String metricsBody = metricsResponse.getBody();
        assertThat(metricsBody).isNotNull();

        List<String> metricLines = metricsBody.lines().toList();

        assertThat(metricLines)
                .anyMatch(line -> line.startsWith("congestion_tax_calculation_seconds_bucket{")
                        && line.contains("outcome=\"success\""));

        assertThat(metricLines)
                .anyMatch(line -> line.startsWith("congestion_tax_calculation_seconds_count{")
                        && line.contains("outcome=\"success\""));
    }

    @Test
    void returnsZeroTaxOutsideStoredTaxTimeBands() throws Exception {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var request = new HttpEntity<>("""
                {
                  "vehicleType": "OTHER",
                  "timeZone": "Europe/Stockholm",
                  "passages": [
                    "2013-02-08 06:40:27"
                  ]
                }
                """, headers);

        var response = restTemplate.postForEntity(
                "/api/v1/cities/gothenburg" + "/congestion-tax/calculations", request, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(response.getBody()).isEqualTo(objectMapper.readTree("""
                {
                  "cityCode": "gothenburg",
                  "vehicleType": "OTHER",
                  "currency": "SEK",
                  "totalAmount": 0.00,
                  "dailyTaxes": [
                    {
                      "date": "2013-02-08",
                      "taxExemptionReasons": [],
                      "amount": 0.00
                    }
                  ]
                }
                """));
    }

    @Test
    void rejectsUnknownCity() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var request = new HttpEntity<>("""
                {
                  "vehicleType": "OTHER",
                  "timeZone": "Europe/Stockholm",
                  "passages": [
                    "2013-02-08 06:20:27"
                  ]
                }
                """, headers);

        var response = restTemplate.postForEntity(
                "/api/v1/cities/unknown" + "/congestion-tax/calculations", request, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsUnknownVehicleType() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var request = new HttpEntity<>("""
                {
                  "vehicleType": "UNKNOWN",
                  "timeZone": "Europe/Stockholm",
                  "passages": [
                    "2013-02-08 06:20:27"
                  ]
                }
                """, headers);

        var response = restTemplate.postForEntity(
                "/api/v1/cities/gothenburg" + "/congestion-tax/calculations", request, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void rejectsInvalidRequest(String scenario, String requestBody) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var request = new HttpEntity<>(requestBody, headers);

        var response = restTemplate.postForEntity(
                "/api/v1/cities/gothenburg" + "/congestion-tax/calculations", request, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
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
                Arguments.of("empty Passage list", """
                        {
                          "vehicleType": "OTHER",
                          "timeZone": "Europe/Stockholm",
                          "passages": []
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
                Arguments.of("multiple Passages", """
                        {
                          "vehicleType": "OTHER",
                          "timeZone": "Europe/Stockholm",
                          "passages": [
                            "2013-02-08 05:20:27",
                            "2013-02-08 06:20:27"
                          ]
                        }
                        """),
                Arguments.of("Passage with offset", """
                        {
                          "vehicleType": "OTHER",
                          "timeZone": "Europe/Stockholm",
                          "passages": [
                            "2013-02-08T05:20:27Z"
                          ]
                        }
                        """),
                Arguments.of("unknown time zone", """
                        {
                          "vehicleType": "OTHER",
                          "timeZone": "Mars/Olympus",
                          "passages": [
                            "2013-02-08 06:20:27"
                          ]
                        }
                        """),
                Arguments.of("fixed offset", """
                        {
                          "vehicleType": "OTHER",
                          "timeZone": "+01:00",
                          "passages": [
                            "2013-02-08 06:20:27"
                          ]
                        }
                        """));
    }
}
