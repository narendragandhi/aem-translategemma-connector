package com.example.aem.translation.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class TranslationMetrics {
    private static final Logger LOG = LoggerFactory.getLogger(TranslationMetrics.class);

    private final MeterRegistry meterRegistry;
    private final Counter translationRequestsCounter;
    private final Counter translationSuccessCounter;
    private final Counter translationFailureCounter;
    private final Counter translationCacheHitCounter;
    private final Timer translationLatencyTimer;
    private final Counter retryCounter;
    private final Counter circuitBreakerOpenCounter;

    public TranslationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.translationRequestsCounter = Counter.builder("translation.requests.total")
                .description("Total translation requests")
                .register(meterRegistry);

        this.translationSuccessCounter = Counter.builder("translation.success.total")
                .description("Successful translations")
                .register(meterRegistry);

        this.translationFailureCounter = Counter.builder("translation.failures.total")
                .description("Failed translations")
                .register(meterRegistry);

        this.translationCacheHitCounter = Counter.builder("translation.cache.hits.total")
                .description("Translation cache hits")
                .register(meterRegistry);

        this.translationLatencyTimer = Timer.builder("translation.latency")
                .description("Translation latency")
                .register(meterRegistry);

        this.retryCounter = Counter.builder("translation.retry.total")
                .description("Translation retry attempts")
                .register(meterRegistry);

        this.circuitBreakerOpenCounter = Counter.builder("translation.circuitbreaker.open.total")
                .description("Circuit breaker open events")
                .register(meterRegistry);
    }

    public void recordTranslationRequest() {
        translationRequestsCounter.increment();
    }

    public void recordTranslationSuccess() {
        translationSuccessCounter.increment();
    }

    public void recordTranslationFailure() {
        translationFailureCounter.increment();
    }

    public void recordCacheHit() {
        translationCacheHitCounter.increment();
    }

    public void recordLatency(long durationMs) {
        translationLatencyTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordRetry() {
        retryCounter.increment();
    }

    public void recordCircuitBreakerOpen() {
        circuitBreakerOpenCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(translationLatencyMeter());
    }

    private Timer translationLatencyMeter() {
        return translationLatencyTimer;
    }
}
