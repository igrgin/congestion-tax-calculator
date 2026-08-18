package io.github.igrgin.congestiontax.calculation.exception;

public final class InvalidStoredCityTimeZoneException extends IllegalStateException {

    private final String cityCode;

    public InvalidStoredCityTimeZoneException(String cityCode, Throwable cause) {
        super("Stored City time zone is invalid for City " + cityCode + ".", cause);
        this.cityCode = cityCode;
    }

    public String cityCode() {
        return cityCode;
    }
}
