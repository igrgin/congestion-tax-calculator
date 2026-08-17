package io.github.igrgin.congestiontax.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

class CalculationMetricsTest {

    @Test
    void calculationContinuesWhenTimerStartFails() {
        var meterRegistry = mock(MeterRegistry.class);
        var calculationMetrics = new CalculationMetrics(meterRegistry);

        given(meterRegistry.config()).willThrow(new IllegalStateException("Timer start failed."));

        var result = calculationMetrics.recordCalculation(() -> "calculated");

        assertThat(result).isEqualTo("calculated");
    }

    @Test
    void calculationContinuesWhenTimerStopFails() {
        var meterRegistry = mock(MeterRegistry.class);
        var meterRegistryConfig = mock(MeterRegistry.Config.class);
        var calculationMetrics = new CalculationMetrics(meterRegistry);

        given(meterRegistry.config()).willReturn(meterRegistryConfig);
        given(meterRegistryConfig.clock()).willReturn(Clock.SYSTEM);

        var result = calculationMetrics.recordCalculation(() -> "calculated");

        assertThat(result).isEqualTo("calculated");
    }
}
