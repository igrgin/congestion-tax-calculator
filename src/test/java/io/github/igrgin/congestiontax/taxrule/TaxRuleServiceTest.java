package io.github.igrgin.congestiontax.taxrule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TaxRuleServiceTest {

    @Test
    void doesNotRequireCalculationDatesToLoadCityTaxRuleSet() {
        assertThat(TaxRuleService.class.getDeclaredMethods())
                .noneMatch(method -> Arrays.asList(method.getParameterTypes()).contains(Set.class));
    }
}
