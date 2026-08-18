package io.github.igrgin.congestiontax.calculation.http;

import io.github.igrgin.congestiontax.calculation.exception.CityNotFoundException;
import io.github.igrgin.congestiontax.calculation.exception.InvalidPassageTimestampException;
import io.github.igrgin.congestiontax.calculation.exception.InvalidStoredTaxRuleOptionException;
import io.github.igrgin.congestiontax.calculation.exception.UnsupportedPassageYearException;
import io.github.igrgin.congestiontax.calculation.exception.VehicleTypeNotFoundException;
import io.github.igrgin.congestiontax.calculation.http.dto.UnsupportedPassageYearResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(InvalidPassageTimestampException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleInvalidPassageTimestamp(InvalidPassageTimestampException exception) {
        log.warn(
                "Rejected Congestion Tax Calculation request. reason={} passageIndex={}",
                "invalid-passage-timestamp",
                exception.passageIndex());
    }

    @ExceptionHandler(UnsupportedPassageYearException.class)
    public ResponseEntity<UnsupportedPassageYearResponse> handleUnsupportedPassageYear(
            UnsupportedPassageYearException exception) {
        log.warn(
                "Rejected Congestion Tax Calculation request. reason={} passageIndexes={}",
                "unsupported-passage-year",
                exception.passageIndexes());

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(UnsupportedPassageYearResponse.from(exception.passageIndexes()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleInvalidRequest() {
        log.warn("Rejected Congestion Tax Calculation request. reason={}", "invalid-request");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleInvalidJson() {
        log.warn("Rejected Congestion Tax Calculation request. reason={}", "invalid-json");
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
}
