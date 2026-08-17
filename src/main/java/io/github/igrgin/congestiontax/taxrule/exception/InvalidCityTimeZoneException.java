package io.github.igrgin.congestiontax.taxrule.exception;

public final class InvalidCityTimeZoneException extends IllegalStateException {

    public InvalidCityTimeZoneException(String cityCode) {
        super("The City has an invalid stored IANA time-zone identifier: " + cityCode + ".");
    }
}
