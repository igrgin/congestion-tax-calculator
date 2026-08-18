package io.github.igrgin.congestiontax.taxrule.exception;

public final class MissingTaxRuleSetException extends IllegalStateException {

    public MissingTaxRuleSetException(String cityCode) {
        super("Tax Rule Set does not exist for City " + cityCode + ".");
    }
}
