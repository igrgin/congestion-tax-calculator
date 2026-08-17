package io.github.igrgin.congestiontax.taxrule.exception;

import java.util.NoSuchElementException;

public final class UnknownCityException extends NoSuchElementException {

    public UnknownCityException(String cityCode) {
        super("City does not exist: " + cityCode + ".");
    }
}
