package io.github.igrgin.congestiontax.taxrule.exception;

import java.time.LocalDate;
import java.time.LocalTime;

public final class OverlappingTaxTimeBandsException extends IllegalStateException {

    public OverlappingTaxTimeBandsException(
            String cityCode,
            LocalDate effectiveFrom,
            LocalTime firstStartTime,
            LocalTime firstEndTime,
            LocalTime secondStartTime,
            LocalTime secondEndTime) {
        super("Tax Rule Set for City "
                + cityCode
                + " effective from "
                + effectiveFrom
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
