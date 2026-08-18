package io.github.igrgin.congestiontax.calculation.exception;

public final class MissingStoredTaxTimeBandsException extends IllegalStateException {

    private final String cityCode;

    public MissingStoredTaxTimeBandsException(String cityCode, Throwable cause) {
        super("Stored Tax Time Bands do not exist for City " + cityCode + ".", cause);
        this.cityCode = cityCode;
    }

    public String cityCode() {
        return cityCode;
    }
}
