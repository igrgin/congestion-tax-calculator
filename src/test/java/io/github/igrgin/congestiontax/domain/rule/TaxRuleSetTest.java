package io.github.igrgin.congestiontax.domain.rule;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.Currency;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TaxRuleSetTest {

    private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2013, Month.JANUARY, 1);
    private static final Currency CURRENCY = Currency.getInstance("SEK");
    private static final TaxTimeBand TAX_TIME_BAND =
            new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), new TaxAmount(new BigDecimal("8.00"), CURRENCY));

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullValues")
    void rejectsNullValues(
            String scenario,
            String cityCode,
            LocalDate effectiveFrom,
            Currency currency,
            List<TaxTimeBand> taxTimeBands) {
        assertThatNullPointerException()
                .isThrownBy(() -> new TaxRuleSet(cityCode, effectiveFrom, currency, taxTimeBands));
    }

    @Test
    void rejectsEmptyTaxTimeBands() {
        assertThatThrownBy(() -> new TaxRuleSet("gothenburg", EFFECTIVE_FROM, CURRENCY, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> nullValues() {
        return Stream.of(
                Arguments.of("null City code", null, EFFECTIVE_FROM, CURRENCY, List.of(TAX_TIME_BAND)),
                Arguments.of("null effective date", "gothenburg", null, CURRENCY, List.of(TAX_TIME_BAND)),
                Arguments.of("null currency", "gothenburg", EFFECTIVE_FROM, null, List.of(TAX_TIME_BAND)),
                Arguments.of("null Tax Time Bands", "gothenburg", EFFECTIVE_FROM, CURRENCY, null));
    }
}
