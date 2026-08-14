package io.github.igrgin.congestiontax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;

@ActiveProfiles("itest")
@AutoConfigureMetrics
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ActuatorITest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthReportsApplicationAndDatabaseAsUpWithoutDetails() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/actuator/health", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.at("/status").asString()).isEqualTo("UP");
        assertThat(body.at("/components/db/status").asString()).isEqualTo("UP");
        assertThat(body.at("/components/db/details").isMissingNode()).isTrue();
    }

    @Test
    void prometheusPublishesStandardApplicationMetrics() {
        restTemplate.getForEntity("/actuator/prometheus", String.class);

        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body)
                .isNotNull()
                .contains(
                        "jvm_memory_used_bytes",
                        "process_uptime_seconds",
                        "http_server_requests_seconds",
                        "hikaricp_connections");
    }
}
