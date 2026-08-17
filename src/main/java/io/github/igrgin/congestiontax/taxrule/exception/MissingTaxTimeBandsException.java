package io.github.igrgin.congestiontax.taxrule.exception;

import java.time.LocalDate;

public final class MissingTaxTimeBandsException extends IllegalStateException {

    public MissingTaxTimeBandsException(String cityCode, LocalDate effectiveFrom) {
        super("Tax Rule Set for City " + cityCode + " effective from " + effectiveFrom + " has no Tax Time Bands.");
    }
}
