package io.github.igrgin.congestiontax.taxrule;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.igrgin.congestiontax.taxrule.model.CityTaxRuleSet;
import org.junit.jupiter.api.Test;

class TaxRuleServiceTest {

    @Test
    void loadsOneCityTaxRuleSetWithoutCalculationDates() throws NoSuchMethodException {
        var method = TaxRuleService.class.getDeclaredMethod("getCityTaxRuleSet", String.class);

        assertThat(method.getReturnType()).isEqualTo(CityTaxRuleSet.class);
    }
}
