package io.github.igrgin.congestiontax.taxrule.exception;

public final class MissingTaxTimeBandsException extends IllegalStateException {

    public MissingTaxTimeBandsException(String cityCode) {
        super("Tax Rule Set for City " + cityCode + " has no Tax Time Bands.");
    }
}
