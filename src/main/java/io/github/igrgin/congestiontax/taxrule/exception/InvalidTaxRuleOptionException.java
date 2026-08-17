package io.github.igrgin.congestiontax.taxrule.exception;

public final class InvalidTaxRuleOptionException extends IllegalStateException {

    private final String optionTypeCode;

    public InvalidTaxRuleOptionException(String optionTypeCode) {
        super("Tax Rule Set contains an invalid stored Tax Rule Option: " + optionTypeCode + ".");
        this.optionTypeCode = optionTypeCode;
    }

    public String optionTypeCode() {
        return optionTypeCode;
    }
}
