package io.github.igrgin.congestiontax.domain.rule;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode
public final class TaxExemptions {

    private final List<TaxExemption> exemptions;

    public TaxExemptions(@NonNull List<TaxExemption> exemptions) {
        this.exemptions = List.copyOf(exemptions);
    }

    public static TaxExemptions empty() {
        return new TaxExemptions(List.of());
    }
}
