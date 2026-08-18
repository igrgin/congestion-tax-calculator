package io.github.igrgin.congestiontax.domain.calculation.exception;

public final class NoMatchingTaxTimeBandException extends IllegalStateException {

    public NoMatchingTaxTimeBandException() {
        super("No Tax Time Band contains a non-exempt Passage City Local Time.");
    }
}
