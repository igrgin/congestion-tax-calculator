package io.github.igrgin.congestiontax.calculation.exception;

public final class InvalidStoredTaxRuleOptionException extends IllegalStateException {

    private final String optionTypeCode;

    public InvalidStoredTaxRuleOptionException(String optionTypeCode, Throwable cause) {
        super("Stored Tax Rule Option is invalid: " + optionTypeCode + ".", cause);
        this.optionTypeCode = optionTypeCode;
    }

    public String optionTypeCode() {
        return optionTypeCode;
    }
}
