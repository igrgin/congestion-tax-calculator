package io.github.igrgin.congestiontax.calculation.exception;

import java.util.NoSuchElementException;

public final class CityNotFoundException extends NoSuchElementException {

    private final String cityCode;

    public CityNotFoundException(String cityCode, Throwable cause) {
        super("City does not exist: " + cityCode + ".", cause);
        this.cityCode = cityCode;
    }

    public String cityCode() {
        return cityCode;
    }
}
