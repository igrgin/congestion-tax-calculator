package io.github.igrgin.congestiontax.calculation.http;

import io.github.igrgin.congestiontax.calculation.exception.CityNotFoundException;
import io.github.igrgin.congestiontax.calculation.exception.InvalidStoredTaxRuleOptionException;
import io.github.igrgin.congestiontax.calculation.exception.UnsupportedPassageYearException;
import io.github.igrgin.congestiontax.calculation.exception.VehicleTypeNotFoundException;
import io.github.igrgin.congestiontax.calculation.http.dto.InvalidRequestResponse;
import java.time.LocalDateTime;
import java.util.OptionalInt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

@RestControllerAdvice(assignableTypes = CalculationController.class)
@Slf4j
public class CalculationExceptionHandler {

    @ExceptionHandler(CityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleCityNotFound(CityNotFoundException exception) {
        log.warn(
                "Rejected Congestion Tax Calculation request. reason={} cityCode={}",
                "unknown-city",
                exception.cityCode());
    }

    @ExceptionHandler(VehicleTypeNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleVehicleTypeNotFound(VehicleTypeNotFoundException exception) {
        log.warn(
                "Rejected Congestion Tax Calculation request. reason={} vehicleTypeCode={}",
                "unknown-vehicle-type",
                exception.vehicleTypeCode());
    }

    @ExceptionHandler(UnsupportedPassageYearException.class)
    public ResponseEntity<InvalidRequestResponse> handleUnsupportedPassageYear(
            UnsupportedPassageYearException exception) {
        log.warn(
                "Rejected Congestion Tax Calculation request. reason={} passageIndexes={}",
                "unsupported-passage-year",
                exception.passageIndexes());

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(InvalidRequestResponse.forUnsupportedPassageYears(exception.passageIndexes()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleInvalidRequest() {
        log.warn("Rejected Congestion Tax Calculation request. reason={}", "invalid-request");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<InvalidRequestResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        var invalidPassageIndex = invalidPassageIndex(exception);
        if (invalidPassageIndex.isPresent()) {
            log.warn(
                    "Rejected Congestion Tax Calculation request. reason={} passageIndex={}",
                    "invalid-passage-timestamp",
                    invalidPassageIndex.getAsInt());
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(InvalidRequestResponse.forInvalidPassageTimestamp(invalidPassageIndex.getAsInt()));
        }

        log.warn("Rejected Congestion Tax Calculation request. reason={}", "invalid-json");
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(InvalidRequestResponse.forInvalidJson());
    }

    @ExceptionHandler(InvalidStoredTaxRuleOptionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleInvalidTaxRuleOption(InvalidStoredTaxRuleOptionException exception) {
        log.error(
                "Congestion Tax Calculation failed. reason={} optionTypeCode={}",
                "invalid-tax-rule-option",
                exception.optionTypeCode(),
                exception);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleUnexpectedFailure(Exception exception) {
        log.error("Congestion Tax Calculation failed unexpectedly.", exception);
    }

    private static OptionalInt invalidPassageIndex(Throwable exception) {
        for (var cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof MismatchedInputException mismatch
                    && mismatch.getTargetType() == LocalDateTime.class
                    && hasPassagesProperty(mismatch)) {
                return mismatch.getPath().stream()
                        .mapToInt(JacksonException.Reference::getIndex)
                        .filter(index -> index >= 0)
                        .findFirst();
            }
        }
        return OptionalInt.empty();
    }

    private static boolean hasPassagesProperty(MismatchedInputException exception) {
        return exception.getPath().stream()
                .map(JacksonException.Reference::getPropertyName)
                .anyMatch("passages"::equals);
    }
}
