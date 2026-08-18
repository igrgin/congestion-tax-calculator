package io.github.igrgin.congestiontax.calculation.http.dto;

import java.util.List;

public record InvalidRequestResponse(String title, int status, String detail, String code, List<RequestError> errors) {

    public static InvalidRequestResponse forUnsupportedPassageYears(List<Integer> passageIndexes) {
        var errors = passageIndexes.stream().map(RequestError::unsupportedYear).toList();

        return invalidPassages(errors);
    }

    public static InvalidRequestResponse forInvalidPassageTimestamp(int passageIndex) {
        return invalidPassages(List.of(RequestError.invalidTimestamp(passageIndex)));
    }

    public static InvalidRequestResponse forInvalidJson() {
        return new InvalidRequestResponse(
                "Invalid JSON", 400, "The request body is not valid JSON.", "INVALID_JSON", List.of());
    }

    private static InvalidRequestResponse invalidPassages(List<RequestError> errors) {
        return new InvalidRequestResponse(
                "Invalid calculation request",
                400,
                "The request contains invalid Passages.",
                "INVALID_REQUEST",
                errors);
    }

    public record RequestError(String field, String code, String message) {

        private static RequestError unsupportedYear(int passageIndex) {
            return new RequestError(
                    field(passageIndex), "UNSUPPORTED_PASSAGE_YEAR", "A Passage City Local Time date must be in 2013.");
        }

        private static RequestError invalidTimestamp(int passageIndex) {
            return new RequestError(
                    field(passageIndex),
                    "INVALID_PASSAGE_TIMESTAMP",
                    "A Passage must use the City Local Time format uuuu-MM-dd HH:mm:ss.");
        }

        private static String field(int passageIndex) {
            return "passages[" + passageIndex + "]";
        }
    }
}
