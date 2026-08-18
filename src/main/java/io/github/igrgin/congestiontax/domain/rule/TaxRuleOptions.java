package io.github.igrgin.congestiontax.domain.rule;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode
public final class TaxRuleOptions {

    private final List<TaxRuleOption> options;

    public TaxRuleOptions(@NonNull List<TaxRuleOption> options) {
        this.options = List.copyOf(options);
        validateUniqueTypes(this.options);
    }

    public static TaxRuleOptions empty() {
        return new TaxRuleOptions(List.of());
    }

    public Optional<ChargeWindow> chargeWindow() {
        return find(ChargeWindow.class);
    }

    public Optional<DailyMaximum> dailyMaximum() {
        return find(DailyMaximum.class);
    }

    public Optional<HolidayPreceding> holidayPreceding() {
        return find(HolidayPreceding.class);
    }

    private <T extends TaxRuleOption> Optional<T> find(Class<T> optionType) {
        return options.stream()
                .filter(optionType::isInstance)
                .map(optionType::cast)
                .findFirst();
    }

    private static void validateUniqueTypes(List<TaxRuleOption> options) {
        var optionTypes = new HashSet<Class<? extends TaxRuleOption>>();

        for (var option : options) {
            if (!optionTypes.add(option.getClass())) {
                throw new IllegalArgumentException("Tax Rule Options must not contain duplicate types.");
            }
        }
    }
}
