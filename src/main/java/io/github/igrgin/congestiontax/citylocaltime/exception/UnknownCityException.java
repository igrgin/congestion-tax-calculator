package io.github.igrgin.congestiontax.citylocaltime.exception;

import java.util.NoSuchElementException;

public final class UnknownCityException extends NoSuchElementException {

    public UnknownCityException(String cityCode) {
        super("City does not exist: " + cityCode + ".");
    }
}
