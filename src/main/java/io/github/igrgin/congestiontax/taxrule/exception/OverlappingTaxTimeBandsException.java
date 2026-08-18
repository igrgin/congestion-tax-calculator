package io.github.igrgin.congestiontax.taxrule.exception;

import java.time.LocalTime;

public final class OverlappingTaxTimeBandsException extends IllegalStateException {

    public OverlappingTaxTimeBandsException(
            String cityCode,
            LocalTime firstStartTime,
            LocalTime firstEndTime,
            LocalTime secondStartTime,
            LocalTime secondEndTime) {
        super("Tax Rule Set for City "
                + cityCode
                + " contains overlapping Tax Time Bands: "
                + firstStartTime
                + "-"
                + firstEndTime
                + " and "
                + secondStartTime
                + "-"
                + secondEndTime
                + ".");
    }
}
