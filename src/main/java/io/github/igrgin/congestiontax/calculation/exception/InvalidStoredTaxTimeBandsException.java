package io.github.igrgin.congestiontax.calculation.exception;

public final class InvalidStoredTaxTimeBandsException extends IllegalStateException {

    private final String cityCode;

    public InvalidStoredTaxTimeBandsException(String cityCode, Throwable cause) {
        super("Stored Tax Time Bands are invalid for City " + cityCode + ".", cause);
        this.cityCode = cityCode;
    }

    public String cityCode() {
        return cityCode;
    }
}
