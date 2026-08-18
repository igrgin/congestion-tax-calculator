package io.github.igrgin.congestiontax.domain.rule;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PublicHolidayPrecedingDateOptionTest {

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveCalendarDateCount(int calendarDateCount) {
        assertThatThrownBy(() -> new PublicHolidayPrecedingDateOption(calendarDateCount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Public Holiday Preceding-Date Option count must be positive.");
    }
}
