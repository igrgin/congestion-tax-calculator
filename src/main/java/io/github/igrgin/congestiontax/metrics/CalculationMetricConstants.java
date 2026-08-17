package io.github.igrgin.congestiontax.metrics;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class CalculationMetricConstants {

    static final String TIMER_NAME = "congestion.tax.calculation";
    static final String PASSAGE_COUNT_NAME = "congestion.tax.calculation.passages";
    static final String OUTCOME_TAG = "outcome";
}
