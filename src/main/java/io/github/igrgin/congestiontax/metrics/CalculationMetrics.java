package io.github.igrgin.congestiontax.metrics;

import static io.github.igrgin.congestiontax.metrics.CalculationMetricConstants.OUTCOME_TAG;
import static io.github.igrgin.congestiontax.metrics.CalculationMetricConstants.PASSAGE_COUNT_NAME;
import static io.github.igrgin.congestiontax.metrics.CalculationMetricConstants.TIMER_NAME;

import io.github.igrgin.congestiontax.taxrule.exception.UnknownCityException;
import io.github.igrgin.congestiontax.taxrule.exception.UnknownVehicleTypeException;
import io.micrometer.core.instrument.DistributionSummary;
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
    private final Meter.MeterProvider<DistributionSummary> passageCount;

    public CalculationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.calculationTimer = Timer.builder(TIMER_NAME).withRegistry(meterRegistry);
        this.passageCount = DistributionSummary.builder(PASSAGE_COUNT_NAME)
                .serviceLevelObjectives(1, 10, 100, 1000, 10000)
                .withRegistry(meterRegistry);
    }

    public <T> T recordCalculation(int acceptedPassageCount, Supplier<T> calculation) {
        recordPassageCount(acceptedPassageCount);
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

    private void recordPassageCount(int acceptedPassageCount) {
        try {
            passageCount.withTags().record(acceptedPassageCount);
        } catch (RuntimeException exception) {
            log.warn("Could not record the Congestion Tax Calculation Passage count.", exception);
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
