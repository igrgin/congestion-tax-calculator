package io.github.igrgin.congestiontax.calculation.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculationCommandTest {

    @Test
    void ownsAnUnmodifiablePassageTimestampList() {
        var passageTimestamps = new ArrayList<>(List.of("2013-02-08 06:20:27"));
        var command = new CalculationCommand("gothenburg", "OTHER", passageTimestamps);

        passageTimestamps.add("2013-02-08 07:20:27");

        assertThat(command.passageTimestamps()).containsExactly("2013-02-08 06:20:27");
        assertThatThrownBy(() -> command.passageTimestamps().add("2013-02-08 08:20:27"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
