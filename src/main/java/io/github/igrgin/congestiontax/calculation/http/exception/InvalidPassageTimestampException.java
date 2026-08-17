package io.github.igrgin.congestiontax.calculation.http.exception;

public final class InvalidPassageTimestampException extends IllegalArgumentException {

    private final int passageIndex;

    public InvalidPassageTimestampException(int passageIndex, Throwable cause) {
        super(
                "Passage at index "
                        + passageIndex
                        + " must contain a valid timestamp"
                        + " with an explicit UTC offset.",
                cause);
        this.passageIndex = passageIndex;
    }

    public int passageIndex() {
        return passageIndex;
    }
}
