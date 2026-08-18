package io.github.igrgin.congestiontax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("itest")
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ApiDocumentationITest {

    private static final String CALCULATION_PATH =
            "/paths/~1api~1v1~1cities~1{cityCode}~1congestion-tax~1calculations/post";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int serverPort;

    @Test
    void publishesCalculationOpenApiContract() {
        var response = restTemplate.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON))
                .isTrue();

        var document = response.getBody();
        assertThat(document).isNotNull();
        assertThat(document.at("/openapi").asString()).startsWith("3.");
        assertThat(document.at("/info/title").asString()).isEqualTo("Congestion Tax Calculator API");
        assertThat(document.at("/info/version").asString()).isEqualTo("v1");

        var operation = requiredNode(document, CALCULATION_PATH);
        var cityCode = requiredNode(operation, "/parameters/0");
        assertThat(cityCode.at("/name").asString()).isEqualTo("cityCode");
        assertThat(cityCode.at("/in").asString()).isEqualTo("path");
        assertThat(cityCode.at("/required").asBoolean()).isTrue();
        assertThat(cityCode.at("/schema/type").asString()).isEqualTo("string");

        var requestSchema =
                resolvedSchema(document, requiredNode(operation, "/requestBody/content/application~1json/schema"));
        assertThat(requestSchema.at("/required"))
                .containsExactlyInAnyOrder(
                        objectMapper.valueToTree("vehicleType"), objectMapper.valueToTree("passages"));
        assertThat(requestSchema.at("/additionalProperties").asBoolean(true)).isFalse();
        assertThat(requestSchema.at("/properties/timeZone").isMissingNode()).isTrue();

        var vehicleTypeSchema = requiredNode(requestSchema, "/properties/vehicleType");
        assertThat(vehicleTypeSchema.at("/type").asString()).isEqualTo("string");
        assertThat(vehicleTypeSchema.at("/minLength").asInt()).isEqualTo(1);
        var vehicleTypePattern =
                Pattern.compile(vehicleTypeSchema.at("/pattern").asString());
        assertThat(vehicleTypePattern.matcher("OTHER").matches()).isTrue();
        assertThat(vehicleTypePattern.matcher(" ").matches()).isFalse();

        var passagesSchema = requiredNode(requestSchema, "/properties/passages");
        assertThat(passagesSchema.at("/type").asString()).isEqualTo("array");
        assertThat(passagesSchema.at("/minItems").asInt()).isEqualTo(1);
        var passageSchema = requiredNode(passagesSchema, "/items");
        assertThat(passageSchema.at("/type").asString()).isEqualTo("string");
        assertThat(passageSchema.at("/example").asString()).isEqualTo("2013-02-08 06:20:27");
        assertThat(passageSchema.at("/description").asString())
                .contains("City Local Time", "2013", "uuuu-MM-dd HH:mm:ss");
        var passagePattern = Pattern.compile(passageSchema.at("/pattern").asString());
        assertThat(passagePattern.matcher("2013-02-08 06:20:27").matches()).isTrue();
        assertThat(passagePattern.matcher("2013-02-08T06:20:27").matches()).isFalse();
        assertThat(passagePattern.matcher("2014-02-08 06:20:27").matches()).isFalse();

        var successSchema =
                resolvedSchema(document, requiredNode(operation, "/responses/200/content/application~1json/schema"));
        assertThat(successSchema.at("/properties/cityCode/type").asString()).isEqualTo("string");
        assertThat(successSchema.at("/properties/vehicleType/type").asString()).isEqualTo("string");
        assertThat(successSchema.at("/properties/currency/type").asString()).isEqualTo("string");
        assertThat(successSchema.at("/properties/totalAmount/type").asString()).isEqualTo("number");
        assertThat(successSchema.at("/properties/dailyTaxes/type").asString()).isEqualTo("array");
        assertThat(successSchema.at("/properties/dailyTaxes/description").asString())
                .contains("date order");

        var dailyTaxSchema = resolvedSchema(document, requiredNode(successSchema, "/properties/dailyTaxes/items"));
        assertThat(dailyTaxSchema.at("/properties/date/type").asString()).isEqualTo("string");
        assertThat(dailyTaxSchema.at("/properties/taxExemptionReasons/type").asString())
                .isEqualTo("array");
        assertThat(dailyTaxSchema.at("/properties/amount/type").asString()).isEqualTo("number");

        assertProblemResponse(document, operation, "404", "unknownCity", "CITY_NOT_FOUND");
        assertProblemResponse(document, operation, "500", "calculationFailed", "CALCULATION_FAILED");
        assertProblemResponse(document, operation, "503", "serviceUnavailable", "SERVICE_UNAVAILABLE");
        assertBadRequestExamples(document, operation);
    }

    @Test
    void swaggerEntryRedirectsToSwaggerUi() throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + serverPort + "/swagger-ui.html"))
                .GET()
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        assertThat(response.headers().firstValue("location")).contains("/swagger-ui/index.html");
    }

    @Test
    void loadsSwaggerUiPage() {
        var response = restTemplate.getForEntity("/swagger-ui/index.html", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML))
                .isTrue();
        assertThat(response.getBody()).contains("Swagger UI", "swagger-ui");
    }

    private void assertProblemResponse(
            JsonNode document, JsonNode operation, String status, String exampleName, String expectedCode) {
        var content = requiredNode(operation, "/responses/" + status + "/content/application~1problem+json");
        assertProblemSchema(resolvedSchema(document, requiredNode(content, "/schema")));
        var example = requiredNode(content, "/examples/" + exampleName + "/value");
        assertThat(example.at("/code").asString()).isEqualTo(expectedCode);
        assertThat(example.at("/type").isMissingNode()).isTrue();
    }

    private void assertBadRequestExamples(JsonNode document, JsonNode operation) {
        var content = requiredNode(operation, "/responses/400/content/application~1problem+json");
        assertProblemSchema(resolvedSchema(document, requiredNode(content, "/schema")));

        var examples = requiredNode(content, "/examples");
        assertThat(examples.at("/invalidRequest/value")).isEqualTo(objectMapper.readTree("""
                {
                  "title": "Invalid calculation request",
                  "status": 400,
                  "detail": "The calculation request is invalid.",
                  "code": "INVALID_REQUEST"
                }
                """));
        assertThat(examples.at("/invalidJson/value")).isEqualTo(objectMapper.readTree("""
                {
                  "title": "Invalid JSON",
                  "status": 400,
                  "detail": "The request body is not valid JSON.",
                  "code": "INVALID_JSON"
                }
                """));
        assertThat(examples.at("/invalidPassageTimestamp/value")).isEqualTo(objectMapper.readTree("""
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
                """));
        assertThat(examples.at("/unsupportedPassageYear/value")).isEqualTo(objectMapper.readTree("""
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
                """));
    }

    private static void assertProblemSchema(JsonNode problemSchema) {
        assertThat(problemSchema.at("/properties/title/type").asString()).isEqualTo("string");
        assertThat(problemSchema.at("/properties/status/type").asString()).isEqualTo("integer");
        assertThat(problemSchema.at("/properties/detail/type").asString()).isEqualTo("string");
        assertThat(problemSchema.at("/properties/code/type").asString()).isEqualTo("string");
        assertThat(problemSchema.at("/properties/errors/type").asString()).isEqualTo("array");
        assertThat(problemSchema.at("/properties/type").isMissingNode()).isTrue();
    }

    private static JsonNode resolvedSchema(JsonNode document, JsonNode schema) {
        var reference = schema.at("/$ref");
        if (!reference.isMissingNode()) {
            assertThat(reference.asString()).startsWith("#/components/schemas/");
            return requiredNode(document, reference.asString().substring(1));
        }

        assertThat(schema.isObject()).isTrue();
        assertThat(schema.isEmpty()).isFalse();
        return schema;
    }

    private static JsonNode requiredNode(JsonNode parent, String pointer) {
        var node = parent.at(pointer);
        assertThat(node.isMissingNode()).as("OpenAPI node at %s", pointer).isFalse();
        return node;
    }
}
