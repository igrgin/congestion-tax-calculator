package io.github.igrgin.congestiontax.calculation;

import io.github.igrgin.congestiontax.domain.calculation.TaxCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CalculationConfiguration {

    @Bean
    public TaxCalculator taxCalculator() {
        return new TaxCalculator();
    }
}
