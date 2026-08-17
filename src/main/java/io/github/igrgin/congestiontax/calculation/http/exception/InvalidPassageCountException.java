package io.github.igrgin.congestiontax.calculation.http.exception;

public final class InvalidPassageCountException extends IllegalArgumentException {

    private final int passageCount;

    public InvalidPassageCountException(int passageCount) {
        super("Exactly one Passage is required, but " + passageCount + " were supplied.");
        this.passageCount = passageCount;
    }

    public int passageCount() {
        return passageCount;
    }
}
