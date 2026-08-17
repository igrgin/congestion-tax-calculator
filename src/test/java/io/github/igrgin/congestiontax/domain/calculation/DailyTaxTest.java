package io.github.igrgin.congestiontax.domain.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class DailyTaxTest {

    @Test
    void defaultsTaxExemptionReasonsToEmpty() {
        var dailyTax = new DailyTax(
                LocalDate.of(2013, 2, 8), new TaxAmount(new BigDecimal("8.00"), Currency.getInstance("SEK")));

        assertThat(dailyTax.taxExemptionReasons()).isEmpty();
    }
}
