package io.github.igrgin.congestiontax.domain.rule;

public record PublicHolidayPrecedingDateOption(int calendarDateCount) implements TaxRuleOption {

    public PublicHolidayPrecedingDateOption {
        if (calendarDateCount <= 0) {
            throw new IllegalArgumentException("Public Holiday Preceding-Date Option count must be positive.");
        }
    }
}
