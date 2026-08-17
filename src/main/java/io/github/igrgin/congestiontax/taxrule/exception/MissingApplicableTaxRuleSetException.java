package io.github.igrgin.congestiontax.taxrule.exception;

import java.time.LocalDate;

public final class MissingApplicableTaxRuleSetException extends IllegalStateException {

    public MissingApplicableTaxRuleSetException(String cityCode, LocalDate calculationDate) {
        super("Applicable Tax Rule Set does not exist for City " + cityCode + " on " + calculationDate + ".");
    }
}
