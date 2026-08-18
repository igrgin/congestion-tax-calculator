package io.github.igrgin.congestiontax.domain.rule;

public record HolidayPreceding(int calendarDateCount) implements TaxRuleOption {

    public HolidayPreceding {
        if (calendarDateCount <= 0) {
            throw new IllegalArgumentException("Public Holiday Preceding-Date Option count must be positive.");
        }
    }
}
