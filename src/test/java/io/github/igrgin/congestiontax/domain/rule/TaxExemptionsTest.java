package io.github.igrgin.congestiontax.domain.rule;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TaxExemptionsTest {

    @Test
    void rejectsDuplicateTaxExemptions() {
        assertThatThrownBy(() -> new TaxExemptions(List.of(
                        new WeekdayTaxExemption(DayOfWeek.SATURDAY), new WeekdayTaxExemption(DayOfWeek.SATURDAY))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tax Exemptions must not contain duplicate values.");
    }

    @Test
    void rejectsNullCollection() {
        assertThatNullPointerException().isThrownBy(() -> new TaxExemptions(null));
    }

    @Test
    void rejectsNullTaxExemption() {
        var exemptions = Arrays.asList((TaxExemption) null);

        assertThatNullPointerException().isThrownBy(() -> new TaxExemptions(exemptions));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullTaxExemptionValues")
    void typedTaxExemptionsRejectNullValues(String scenario, Runnable constructor) {
        assertThatNullPointerException().isThrownBy(constructor::run);
    }

    private static Stream<Arguments> nullTaxExemptionValues() {
        return Stream.of(
                Arguments.of("null weekday", (Runnable) () -> new WeekdayTaxExemption(null)),
                Arguments.of("null month", (Runnable) () -> new MonthTaxExemption(null)),
                Arguments.of("null public holiday date", (Runnable) () -> new PublicHolidayTaxExemption(null)),
                Arguments.of("null Vehicle Type code", (Runnable) () -> new VehicleTypeTaxExemption(null)));
    }
}
