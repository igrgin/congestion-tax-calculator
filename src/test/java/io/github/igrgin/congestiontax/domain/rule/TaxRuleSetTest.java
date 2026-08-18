package io.github.igrgin.congestiontax.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TaxRuleSetTest {

    private static final Currency CURRENCY = Currency.getInstance("SEK");
    private static final TaxTimeBand TAX_TIME_BAND =
            new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), new TaxAmount(new BigDecimal("8.00"), CURRENCY));

    @Test
    void exposesOnlyCurrentCityTaxRuleFields() {
        assertThat(Arrays.stream(TaxRuleSet.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("cityCode", "currency", "taxTimeBands", "taxRuleOptions");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullValues")
    void rejectsNullValues(
            String scenario,
            String cityCode,
            Currency currency,
            List<TaxTimeBand> taxTimeBands,
            TaxRuleOptions taxRuleOptions) {
        assertThatNullPointerException()
                .isThrownBy(() -> new TaxRuleSet(cityCode, currency, taxTimeBands, taxRuleOptions));
    }

    @Test
    void rejectsEmptyTaxTimeBands() {
        assertThatThrownBy(() -> new TaxRuleSet("gothenburg", CURRENCY, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> nullValues() {
        return Stream.of(
                Arguments.of("null City code", null, CURRENCY, List.of(TAX_TIME_BAND), TaxRuleOptions.empty()),
                Arguments.of("null currency", "gothenburg", null, List.of(TAX_TIME_BAND), TaxRuleOptions.empty()),
                Arguments.of("null Tax Time Bands", "gothenburg", CURRENCY, null, TaxRuleOptions.empty()),
                Arguments.of("null Tax Rule Options", "gothenburg", CURRENCY, List.of(TAX_TIME_BAND), null));
    }
}
