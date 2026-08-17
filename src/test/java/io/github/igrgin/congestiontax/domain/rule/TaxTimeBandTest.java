package io.github.igrgin.congestiontax.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.igrgin.congestiontax.domain.TaxAmount;
import io.github.igrgin.congestiontax.domain.rule.exception.InvalidTaxTimeBandException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Currency;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TaxTimeBandTest {

    private static final TaxAmount AMOUNT = new TaxAmount(new BigDecimal("8.00"), Currency.getInstance("SEK"));

    @Test
    void includesStartTime() {
        var band = new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), AMOUNT);

        assertThat(band.includes(LocalTime.of(6, 0))).isTrue();
    }

    @Test
    void excludesEndTime() {
        var band = new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), AMOUNT);

        assertThat(band.includes(LocalTime.of(6, 30))).isFalse();
    }

    @Test
    void excludesTimeOutsideBand() {
        var band = new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), AMOUNT);

        assertThat(band.includes(LocalTime.of(5, 59))).isFalse();
    }

    @Test
    void rejectsEndTimeEqualToStartTime() {
        var time = LocalTime.of(6, 0);

        assertThatThrownBy(() -> new TaxTimeBand(time, time, AMOUNT)).isInstanceOf(InvalidTaxTimeBandException.class);
    }

    @Test
    void rejectsEndTimeBeforeStartTime() {
        var startTime = LocalTime.of(6, 30);
        var endTime = LocalTime.of(6, 0);

        assertThatThrownBy(() -> new TaxTimeBand(startTime, endTime, AMOUNT))
                .isInstanceOf(InvalidTaxTimeBandException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullValues")
    void rejectsNullValues(String scenario, LocalTime startTime, LocalTime endTime, TaxAmount amount) {
        assertThatNullPointerException().isThrownBy(() -> new TaxTimeBand(startTime, endTime, amount));
    }

    @Test
    void rejectsZeroTaxAmount() {
        var zeroAmount = TaxAmount.zero(Currency.getInstance("SEK"));

        assertThatThrownBy(() -> new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), zeroAmount))
                .isInstanceOf(InvalidTaxTimeBandException.class);
    }

    private static Stream<Arguments> nullValues() {
        return Stream.of(
                Arguments.of("null start time", null, LocalTime.of(6, 30), AMOUNT),
                Arguments.of("null end time", LocalTime.of(6, 0), null, AMOUNT),
                Arguments.of("null Tax Amount", LocalTime.of(6, 0), LocalTime.of(6, 30), null));
    }
}
