package io.github.igrgin.congestiontax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

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
    void calculatesSuppliedPassagesFromStoredGothenburgTaxRules() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var request = new HttpEntity<>("""
                {
                  "vehicleType": "OTHER",
                  "passages": [
                    "2013-01-14 21:00:00",
                    "2013-01-15 21:00:00",
                    "2013-02-07 06:23:27",
                    "2013-02-07 15:27:00",
                    "2013-02-08 06:27:00",
                    "2013-02-08 06:20:27",
                    "2013-02-08 14:35:00",
                    "2013-02-08 15:29:00",
                    "2013-02-08 15:47:00",
                    "2013-02-08 16:01:00",
                    "2013-02-08 16:48:00",
                    "2013-02-08 17:49:00",
                    "2013-02-08 18:29:00",
                    "2013-02-08 18:35:00",
                    "2013-03-26 14:25:00",
                    "2013-03-28 14:07:27"
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
                  "totalAmount": 89.00,
                  "dailyTaxes": [
                    {
                      "date": "2013-01-14",
                      "taxExemptionReasons": [],
                      "amount": 0.00
                    },
                    {
                      "date": "2013-01-15",
                      "taxExemptionReasons": [],
                      "amount": 0.00
                    },
                    {
                      "date": "2013-02-07",
                      "taxExemptionReasons": [],
                      "amount": 21.00
                    },
                    {
                      "date": "2013-02-08",
                      "taxExemptionReasons": [],
                      "amount": 60.00
                    },
                    {
                      "date": "2013-03-26",
                      "taxExemptionReasons": [],
                      "amount": 8.00
                    },
                    {
                      "date": "2013-03-28",
                      "taxExemptionReasons": [
                        "DATE_BEFORE_PUBLIC_HOLIDAY"
                      ],
                      "amount": 0.00
                    }
                  ]
                }
                """));

        var prometheusResponse = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(prometheusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        var prometheusBody = prometheusResponse.getBody();
        assertThat(prometheusBody).isNotNull();
        assertThat(prometheusBody.lines())
                .anyMatch(line -> line.startsWith("http_server_requests_seconds_count{")
                        && line.contains("outcome=\"SUCCESS\"")
                        && line.contains("status=\"200\"")
                        && line.contains("uri=\"/api/v1/cities/{cityCode}/congestion-tax/calculations\""))
                .anyMatch(line -> line.startsWith("hikaricp_connections{"));
        assertThat(prometheusBody)
                .contains("congestion_tax_calculation_seconds_count{outcome=\"success\"}")
                .contains(
                        "congestion_tax_calculation_passages_bucket{le=\"1.0\"}",
                        "congestion_tax_calculation_passages_bucket{le=\"10.0\"}",
                        "congestion_tax_calculation_passages_bucket{le=\"100.0\"}",
                        "congestion_tax_calculation_passages_bucket{le=\"1000.0\"}",
                        "congestion_tax_calculation_passages_bucket{le=\"10000.0\"}");
    }

    @Test
    void calculatesPassagesFromStoredLondonTaxRules() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var request = new HttpEntity<>("""
                {
                  "vehicleType": "OTHER",
                  "passages": [
                    "2013-02-04 12:15:00",
                    "2013-02-05 11:45:00",
                    "2013-02-05 12:15:00"
                  ]
                }
                """, headers);

        var response = restTemplate.postForEntity(
                "/api/v1/cities/london-test" + "/congestion-tax/calculations", request, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(objectMapper.readTree("""
                {
                  "cityCode": "london-test",
                  "vehicleType": "OTHER",
                  "currency": "GBP",
                  "totalAmount": 10.00,
                  "dailyTaxes": [
                    {
                      "date": "2013-02-04",
                      "taxExemptionReasons": [
                        "WEEKDAY"
                      ],
                      "amount": 0.00
                    },
                    {
                      "date": "2013-02-05",
                      "taxExemptionReasons": [],
                      "amount": 10.00
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
                Arguments.of("empty Passage list", """
                        {
                          "vehicleType": "OTHER",
                          "passages": []
                        }
                        """),
                Arguments.of("null Passage", """
                        {
                          "vehicleType": "OTHER",
                          "passages": [
                            null
                          ]
                        }
                        """),
                Arguments.of("Passage with offset", """
                        {
                          "vehicleType": "OTHER",
                          "passages": [
                            "2013-02-08T05:20:27Z"
                          ]
                        }
                        """),
                Arguments.of("removed time zone property", """
                        {
                          "vehicleType": "OTHER",
                          "timeZone": "Europe/Stockholm",
                          "passages": [
                            "2013-02-08 06:20:27"
                          ]
                        }
                        """));
    }
}
