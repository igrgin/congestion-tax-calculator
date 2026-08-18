package io.github.igrgin.congestiontax.calculation.http;

import io.github.igrgin.congestiontax.calculation.exception.CityNotFoundException;
import io.github.igrgin.congestiontax.calculation.exception.InvalidStoredTaxRuleOptionException;
import io.github.igrgin.congestiontax.calculation.exception.InvalidStoredTaxTimeBandsException;
import io.github.igrgin.congestiontax.calculation.exception.MissingStoredTaxRuleSetException;
import io.github.igrgin.congestiontax.calculation.exception.UnsupportedPassageYearException;
import io.github.igrgin.congestiontax.calculation.exception.VehicleTypeNotFoundException;
import io.github.igrgin.congestiontax.calculation.http.dto.CalculationProblemResponse;
import java.time.LocalDateTime;
import java.util.OptionalInt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.MismatchedInputException;

@RestControllerAdvice(assignableTypes = CalculationController.class)
@Slf4j
public class CalculationExceptionHandler {

    @ExceptionHandler(CityNotFoundException.class)
    public ResponseEntity<CalculationProblemResponse> handleCityNotFound(CityNotFoundException exception) {
        log.warn(
                "Rejected Congestion Tax Calculation request. reason={} cityCode={}",
                "unknown-city",
                exception.cityCode());

        return problem(HttpStatus.NOT_FOUND, CalculationProblemResponse.forUnknownCity());
    }

    @ExceptionHandler(VehicleTypeNotFoundException.class)
    public ResponseEntity<CalculationProblemResponse> handleVehicleTypeNotFound(
            VehicleTypeNotFoundException exception) {
        log.warn(
                "Rejected Congestion Tax Calculation request. reason={} vehicleTypeCode={}",
                "unknown-vehicle-type",
                exception.vehicleTypeCode());

        return problem(HttpStatus.BAD_REQUEST, CalculationProblemResponse.forInvalidRequest());
    }

    @ExceptionHandler(UnsupportedPassageYearException.class)
    public ResponseEntity<CalculationProblemResponse> handleUnsupportedPassageYear(
            UnsupportedPassageYearException exception) {
        log.warn(
                "Rejected Congestion Tax Calculation request. reason={} passageIndexes={}",
                "unsupported-passage-year",
                exception.passageIndexes());

        return problem(
                HttpStatus.BAD_REQUEST,
                CalculationProblemResponse.forUnsupportedPassageYears(exception.passageIndexes()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CalculationProblemResponse> handleInvalidRequest() {
        log.warn("Rejected Congestion Tax Calculation request. reason={}", "invalid-request");

        return problem(HttpStatus.BAD_REQUEST, CalculationProblemResponse.forInvalidRequest());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CalculationProblemResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception) {
        var invalidPassageIndex = invalidPassageIndex(exception);
        if (invalidPassageIndex.isPresent()) {
            log.warn(
                    "Rejected Congestion Tax Calculation request. reason={} passageIndex={}",
                    "invalid-passage-timestamp",
                    invalidPassageIndex.getAsInt());
            return problem(
                    HttpStatus.BAD_REQUEST,
                    CalculationProblemResponse.forInvalidPassageTimestamp(invalidPassageIndex.getAsInt()));
        }

        if (hasCause(exception, StreamReadException.class)) {
            log.warn("Rejected Congestion Tax Calculation request. reason={}", "invalid-json");
            return problem(HttpStatus.BAD_REQUEST, CalculationProblemResponse.forInvalidJson());
        }

        log.warn("Rejected Congestion Tax Calculation request. reason={}", "invalid-request");
        return problem(HttpStatus.BAD_REQUEST, CalculationProblemResponse.forInvalidRequest());
    }

    @ExceptionHandler(InvalidStoredTaxRuleOptionException.class)
    public ResponseEntity<CalculationProblemResponse> handleInvalidTaxRuleOption(
            InvalidStoredTaxRuleOptionException exception) {
        log.error(
                "Congestion Tax Calculation failed. reason={} optionTypeCode={}",
                "invalid-tax-rule-option",
                exception.optionTypeCode(),
                exception);

        return internalFailure();
    }

    @ExceptionHandler(MissingStoredTaxRuleSetException.class)
    public ResponseEntity<CalculationProblemResponse> handleMissingTaxRuleSet(
            MissingStoredTaxRuleSetException exception) {
        log.error(
                "Congestion Tax Calculation failed. reason={} cityCode={}",
                "missing-tax-rule-set",
                exception.cityCode(),
                exception);

        return internalFailure();
    }

    @ExceptionHandler(InvalidStoredTaxTimeBandsException.class)
    public ResponseEntity<CalculationProblemResponse> handleInvalidTaxTimeBands(
            InvalidStoredTaxTimeBandsException exception) {
        log.error(
                "Congestion Tax Calculation failed. reason={} cityCode={}",
                "overlapping-tax-time-bands",
                exception.cityCode(),
                exception);

        return internalFailure();
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<CalculationProblemResponse> handleServiceUnavailable(
            DataAccessResourceFailureException exception) {
        log.error("Congestion Tax Calculation failed. reason={}", "service-unavailable", exception);

        return problem(HttpStatus.SERVICE_UNAVAILABLE, CalculationProblemResponse.forServiceUnavailable());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CalculationProblemResponse> handleUnexpectedFailure(Exception exception) {
        log.error("Congestion Tax Calculation failed unexpectedly.", exception);

        return internalFailure();
    }

    private static ResponseEntity<CalculationProblemResponse> internalFailure() {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, CalculationProblemResponse.forCalculationFailure());
    }

    private static ResponseEntity<CalculationProblemResponse> problem(
            HttpStatus status, CalculationProblemResponse response) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(response);
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

    private static boolean hasCause(Throwable exception, Class<? extends Throwable> causeType) {
        for (var cause = exception; cause != null; cause = cause.getCause()) {
            if (causeType.isInstance(cause)) {
                return true;
            }
        }
        return false;
    }
}
