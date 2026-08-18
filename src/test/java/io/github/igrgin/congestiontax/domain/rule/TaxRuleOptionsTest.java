package io.github.igrgin.congestiontax.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaxRuleOptionsTest {

    @Test
    void returnsPublicHolidayPrecedingDateOptionWhenPresent() {
        var publicHolidayPrecedingDateOption = new PublicHolidayPrecedingDateOption(1);

        assertThat(new TaxRuleOptions(List.of(publicHolidayPrecedingDateOption)).publicHolidayPrecedingDateOption())
                .contains(publicHolidayPrecedingDateOption);
    }

    @Test
    void returnsEmptyPublicHolidayPrecedingDateOptionWhenAbsent() {
        assertThat(TaxRuleOptions.empty().publicHolidayPrecedingDateOption()).isEmpty();
    }

    @Test
    void rejectsDuplicateOptionTypes() {
        var firstChargeWindow = new ChargeWindow(Duration.ofMinutes(60));
        var secondChargeWindow = new ChargeWindow(Duration.ofMinutes(30));

        assertThatThrownBy(() -> new TaxRuleOptions(List.of(firstChargeWindow, secondChargeWindow)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tax Rule Options must not contain duplicate types.");
    }
}
