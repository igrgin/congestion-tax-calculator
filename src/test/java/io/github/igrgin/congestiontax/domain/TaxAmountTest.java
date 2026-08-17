package io.github.igrgin.congestiontax.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.igrgin.congestiontax.domain.exception.CurrencyMismatchException;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class TaxAmountTest {

    private static final Currency SEK = Currency.getInstance("SEK");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void addsTaxAmounts() {
        var left = new TaxAmount(new BigDecimal("8.00"), SEK);
        var right = new TaxAmount(new BigDecimal("13.00"), SEK);

        assertThat(left.add(right)).isEqualTo(new TaxAmount(new BigDecimal("21.00"), SEK));
    }

    @Test
    void selectsSmallerTaxAmount() {
        var smaller = new TaxAmount(new BigDecimal("8.00"), SEK);
        var larger = new TaxAmount(new BigDecimal("13.00"), SEK);

        assertThat(smaller.min(larger)).isEqualTo(smaller);
    }

    @Test
    void createsZeroTaxAmount() {
        assertThat(TaxAmount.zero(SEK)).isEqualTo(new TaxAmount(new BigDecimal("0.00"), SEK));
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> new TaxAmount(null, SEK)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNegativeAmount() {
        var negativeAmount = new BigDecimal("-0.01");

        assertThatThrownBy(() -> new TaxAmount(negativeAmount, SEK)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullCurrency() {
        var amount = new BigDecimal("8.00");

        assertThatThrownBy(() -> new TaxAmount(amount, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsAdditionOfDifferentCurrencies() {
        var sekAmount = new TaxAmount(new BigDecimal("8.00"), SEK);
        var eurAmount = new TaxAmount(new BigDecimal("8.00"), EUR);

        assertThatThrownBy(() -> sekAmount.add(eurAmount)).isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void rejectsMinimumComparisonOfDifferentCurrencies() {
        var sekAmount = new TaxAmount(new BigDecimal("8.00"), SEK);
        var eurAmount = new TaxAmount(new BigDecimal("8.00"), EUR);

        assertThatThrownBy(() -> sekAmount.min(eurAmount)).isInstanceOf(CurrencyMismatchException.class);
    }
}
