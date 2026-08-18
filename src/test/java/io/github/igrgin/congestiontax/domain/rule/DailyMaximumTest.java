package io.github.igrgin.congestiontax.domain.rule;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import java.util.Currency;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DailyMaximumTest {

    private static final Currency SEK = Currency.getInstance("SEK");

    @ParameterizedTest
    @MethodSource("invalidTaxAmounts")
    void rejectsInvalidTaxAmount(TaxAmount amount, Class<? extends RuntimeException> exceptionType) {
        assertThatThrownBy(() -> new DailyMaximum(amount)).isInstanceOf(exceptionType);
    }

    private static Stream<Arguments> invalidTaxAmounts() {
        return Stream.of(
                Arguments.of((TaxAmount) null, NullPointerException.class),
                Arguments.of(TaxAmount.zero(SEK), IllegalArgumentException.class));
    }
}
