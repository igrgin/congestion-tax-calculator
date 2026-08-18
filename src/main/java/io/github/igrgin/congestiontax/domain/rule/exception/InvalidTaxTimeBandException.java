package io.github.igrgin.congestiontax.domain.rule.exception;

public final class InvalidTaxTimeBandException extends IllegalArgumentException {

    public InvalidTaxTimeBandException() {
        super("Tax Time Band Tax Amount must not be negative.");
    }
}
