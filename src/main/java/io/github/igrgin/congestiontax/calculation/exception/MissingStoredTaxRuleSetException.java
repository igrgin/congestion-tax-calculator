package io.github.igrgin.congestiontax.calculation.exception;

public final class MissingStoredTaxRuleSetException extends IllegalStateException {

    private final String cityCode;

    public MissingStoredTaxRuleSetException(String cityCode, Throwable cause) {
        super("Stored Tax Rule Set does not exist for City " + cityCode + ".", cause);
        this.cityCode = cityCode;
    }

    public String cityCode() {
        return cityCode;
    }
}
