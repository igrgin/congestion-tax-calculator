package io.github.igrgin.congestiontax.calculation.exception;

import java.util.List;

public final class UnsupportedPassageYearException extends IllegalArgumentException {

    private final List<Integer> passageIndexes;

    public UnsupportedPassageYearException(List<Integer> passageIndexes) {
        super("A Passage City Local Time date must be in 2013.");
        this.passageIndexes = List.copyOf(passageIndexes);
    }

    public List<Integer> passageIndexes() {
        return passageIndexes;
    }
}
