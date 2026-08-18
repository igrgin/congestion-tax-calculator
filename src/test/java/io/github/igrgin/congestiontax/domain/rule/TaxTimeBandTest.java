package io.github.igrgin.congestiontax.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.igrgin.congestiontax.domain.TaxAmount;
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("fullDayTimes")
    void fullDayTaxTimeBandMatchesEveryLocalTime(String scenario, LocalTime localTime) {
        var boundary = LocalTime.of(6, 0);
        var band = new TaxTimeBand(boundary, boundary, AMOUNT);

        assertThat(band.includes(localTime)).isTrue();
    }

    @Test
    void fullDayTaxTimeBandRejectsNullLocalTime() {
        var boundary = LocalTime.of(6, 0);
        var band = new TaxTimeBand(boundary, boundary, AMOUNT);

        assertThatNullPointerException().isThrownBy(() -> band.includes(null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("crossMidnightTimes")
    void matchesCrossMidnightTimeBand(String scenario, LocalTime localTime, boolean expected) {
        var band = new TaxTimeBand(LocalTime.of(18, 30), LocalTime.of(6, 0), AMOUNT);

        assertThat(band.includes(localTime)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullValues")
    void rejectsNullValues(String scenario, LocalTime startTime, LocalTime endTime, TaxAmount amount) {
        assertThatNullPointerException().isThrownBy(() -> new TaxTimeBand(startTime, endTime, amount));
    }

    @Test
    void acceptsZeroTaxAmount() {
        var zeroAmount = TaxAmount.zero(Currency.getInstance("SEK"));

        var band = new TaxTimeBand(LocalTime.of(6, 0), LocalTime.of(6, 30), zeroAmount);

        assertThat(band.amount()).isEqualTo(zeroAmount);
    }

    private static Stream<Arguments> nullValues() {
        return Stream.of(
                Arguments.of("null start time", null, LocalTime.of(6, 30), AMOUNT),
                Arguments.of("null end time", LocalTime.of(6, 0), null, AMOUNT),
                Arguments.of("null Tax Amount", LocalTime.of(6, 0), LocalTime.of(6, 30), null));
    }

    private static Stream<Arguments> crossMidnightTimes() {
        return Stream.of(
                Arguments.of("includes start", LocalTime.of(18, 30), true),
                Arguments.of("includes before midnight", LocalTime.of(23, 59, 59), true),
                Arguments.of("includes after midnight", LocalTime.of(0, 0), true),
                Arguments.of("excludes end", LocalTime.of(6, 0), false),
                Arguments.of("excludes before start", LocalTime.of(18, 29, 59), false));
    }

    private static Stream<Arguments> fullDayTimes() {
        return Stream.of(
                Arguments.of("matches midnight", LocalTime.MIN),
                Arguments.of("matches before boundary", LocalTime.of(5, 59, 59)),
                Arguments.of("matches boundary", LocalTime.of(6, 0)),
                Arguments.of("matches after boundary", LocalTime.of(6, 0, 0, 1)),
                Arguments.of("matches end of day", LocalTime.MAX));
    }
}
