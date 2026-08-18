package io.github.igrgin.congestiontax.calculation.exception;

public final class InvalidStoredTaxExemptionException extends IllegalStateException {

    private final String taxExemptionTypeCode;

    public InvalidStoredTaxExemptionException(String taxExemptionTypeCode, Throwable cause) {
        super("Stored Tax Exemption is invalid: " + taxExemptionTypeCode + ".", cause);
        this.taxExemptionTypeCode = taxExemptionTypeCode;
    }

    public String taxExemptionTypeCode() {
        return taxExemptionTypeCode;
    }
}
