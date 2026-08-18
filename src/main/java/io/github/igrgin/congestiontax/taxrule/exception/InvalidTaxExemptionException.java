package io.github.igrgin.congestiontax.taxrule.exception;

public final class InvalidTaxExemptionException extends IllegalStateException {

    private final String taxExemptionTypeCode;

    public InvalidTaxExemptionException(String taxExemptionTypeCode) {
        super("Tax Rule Set contains an invalid stored Tax Exemption: " + taxExemptionTypeCode + ".");
        this.taxExemptionTypeCode = taxExemptionTypeCode;
    }

    public String taxExemptionTypeCode() {
        return taxExemptionTypeCode;
    }
}
