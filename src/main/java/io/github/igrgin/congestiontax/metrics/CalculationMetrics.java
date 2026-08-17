package io.github.igrgin.congestiontax.metrics;

import static io.github.igrgin.congestiontax.metrics.CalculationMetricConstants.OUTCOME_TAG;
import static io.github.igrgin.congestiontax.metrics.CalculationMetricConstants.TIMER_NAME;

import io.github.igrgin.congestiontax.citylocaltime.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public final class CalculationMetrics {

    private final MeterRegistry meterRegistry;
    private final Meter.MeterProvider<Timer> calculationTimer;

    public CalculationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.calculationTimer = Timer.builder(TIMER_NAME).withRegistry(meterRegistry);
    }

    public <T> T recordCalculation(Supplier<T> calculation) {
        var timerSample = startTimer();
        var outcome = Outcome.FAILED;

        try {
            var result = calculation.get();
            outcome = Outcome.SUCCESS;
            return result;
        } catch (UnknownCityException | UnknownVehicleTypeException exception) {
            outcome = Outcome.REJECTED;
            throw exception;
        } finally {
            stopTimer(timerSample, outcome);
        }
    }

    private Timer.Sample startTimer() {
        try {
            return Timer.start(meterRegistry);
        } catch (RuntimeException exception) {
            log.warn("Could not start the Congestion Tax Calculation timer.", exception);
            return null;
        }
    }

    private void stopTimer(Timer.Sample timerSample, Outcome outcome) {
        if (timerSample == null) {
            return;
        }

        try {
            timerSample.stop(calculationTimer.withTag(OUTCOME_TAG, outcome.tagValue));
        } catch (RuntimeException exception) {
            log.warn("Could not stop the Congestion Tax Calculation timer.", exception);
        }
    }

    private enum Outcome {
        SUCCESS("success"),
        REJECTED("rejected"),
        FAILED("failed");

        private final String tagValue;

        private Outcome(String tagValue) {
            this.tagValue = tagValue;
        }
    }
}
