package io.github.igrgin.congestiontax;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfiguration {

    @Bean
    OpenAPI congestionTaxCalculatorOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Congestion Tax Calculator API").version("v1"));
    }
}
