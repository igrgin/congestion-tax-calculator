package io.github.igrgin.congestiontax.domain;

import io.github.igrgin.congestiontax.domain.exception.CurrencyMismatchException;
import java.math.BigDecimal;
import java.util.Currency;
import lombok.NonNull;

public record TaxAmount(@NonNull BigDecimal amount, @NonNull Currency currency) {

    public TaxAmount {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Tax Amount must not be negative.");
        }
    }

    public static TaxAmount zero(Currency currency) {
        return new TaxAmount(new BigDecimal("0.00"), currency);
    }

    public TaxAmount add(TaxAmount other) {
        validateSameCurrency(other);

        return new TaxAmount(amount.add(other.amount), currency);
    }

    public TaxAmount min(TaxAmount other) {
        validateSameCurrency(other);

        return amount.compareTo(other.amount) <= 0 ? this : other;
    }

    public TaxAmount max(TaxAmount other) {
        validateSameCurrency(other);

        return amount.compareTo(other.amount) >= 0 ? this : other;
    }

    public boolean isGreaterThan(TaxAmount other) {
        validateSameCurrency(other);

        return amount.compareTo(other.amount) > 0;
    }

    private void validateSameCurrency(TaxAmount other) {
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
    }
}
