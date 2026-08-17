package io.github.igrgin.congestiontax.calculation.http.exception;

public final class InvalidPassageTimestampException extends IllegalArgumentException {

    private final int passageIndex;

    public InvalidPassageTimestampException(int passageIndex, Throwable cause) {
        super("Passage at index " + passageIndex + " must use the format uuuu-MM-dd HH:mm:ss.", cause);
        this.passageIndex = passageIndex;
    }

    public int passageIndex() {
        return passageIndex;
    }
}
