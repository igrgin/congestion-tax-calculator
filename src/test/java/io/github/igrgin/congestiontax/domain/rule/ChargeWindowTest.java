package io.github.igrgin.congestiontax.domain.rule;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ChargeWindowTest {

    @ParameterizedTest
    @MethodSource("invalidDurations")
    void rejectsInvalidDuration(Duration duration, Class<? extends RuntimeException> exceptionType) {
        assertThatThrownBy(() -> new ChargeWindow(duration)).isInstanceOf(exceptionType);
    }

    private static Stream<Arguments> invalidDurations() {
        return Stream.of(
                Arguments.of(null, NullPointerException.class),
                Arguments.of(Duration.ZERO, IllegalArgumentException.class),
                Arguments.of(Duration.ofMinutes(-1), IllegalArgumentException.class));
    }
}
