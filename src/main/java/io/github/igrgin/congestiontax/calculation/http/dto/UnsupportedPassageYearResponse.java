package io.github.igrgin.congestiontax.calculation.http.dto;

import java.util.List;

public record UnsupportedPassageYearResponse(
        String title, int status, String detail, String code, List<PassageError> errors) {

    public static UnsupportedPassageYearResponse from(List<Integer> passageIndexes) {
        var errors = passageIndexes.stream().map(PassageError::unsupportedYear).toList();

        return new UnsupportedPassageYearResponse(
                "Invalid calculation request",
                400,
                "The request contains invalid Passages.",
                "INVALID_REQUEST",
                errors);
    }

    public record PassageError(String field, String code, String message) {

        private static PassageError unsupportedYear(int passageIndex) {
            return new PassageError(
                    "passages[" + passageIndex + "]",
                    "UNSUPPORTED_PASSAGE_YEAR",
                    "A Passage City Local Time date must be in 2013.");
        }
    }
}
