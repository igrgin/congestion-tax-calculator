package io.github.igrgin.congestiontax.domain.exception;

import java.util.Currency;

public final class CurrencyMismatchException extends IllegalArgumentException {

    public CurrencyMismatchException(Currency left, Currency right) {
        super("Tax Amount currencies must match: %s and %s."
                .formatted(left.getCurrencyCode(), right.getCurrencyCode()));
    }
}
