package com.example.aem.translation.util;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

public class ResilienceHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ResilienceHelper.class);

    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final int maxRetries;
    private final long waitDurationMillis;

    public ResilienceHelper(int maxRetries, long waitDurationMillis, 
                           int failureRateThreshold, int slowCallRateThreshold,
                           int slowCallDurationMillis, int circuitBreakerWaitDurationSeconds,
                           int slidingWindowSize) {
        this.maxRetries = maxRetries;
        this.waitDurationMillis = waitDurationMillis;

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(maxRetries)
                .waitDuration(Duration.ofMillis(waitDurationMillis))
                .retryExceptions(Exception.class)
                .build();
        this.retryRegistry = RetryRegistry.of(retryConfig);

        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .slowCallRateThreshold(slowCallRateThreshold)
                .slowCallDurationThreshold(Duration.ofMillis(slowCallDurationMillis))
                .waitDurationInOpenState(Duration.ofSeconds(circuitBreakerWaitDurationSeconds))
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig);

        LOG.info("ResilienceHelper initialized with maxRetries={}, waitDurationMillis={}, " +
                "failureRateThreshold={}, slowCallRateThreshold={}", 
                maxRetries, waitDurationMillis, failureRateThreshold, slowCallRateThreshold);
    }

    public <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        Retry retry = retryRegistry.retry(operationName);
        
        return Retry.decorateSupplier(retry, () -> {
            LOG.debug("Executing operation: {}", operationName);
            return operation.get();
        }).get();
    }

    public void executeWithRetry(Runnable operation, String operationName) {
        Retry retry = retryRegistry.retry(operationName);
        
        Retry.decorateRunnable(retry, () -> {
            LOG.debug("Executing operation: {}", operationName);
            operation.run();
        }).run();
    }

    public <T> T executeWithCircuitBreaker(Supplier<T> operation, String circuitBreakerName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);

        return CircuitBreaker.decorateSupplier(circuitBreaker, operation).get();
    }

    public <T> T executeWithRetryAndCircuitBreaker(Supplier<T> operation, 
                                                   String retryName, 
                                                   String circuitBreakerName) {
        Retry retry = retryRegistry.retry(retryName);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);

        return io.github.resilience4j.retry.Retry.decorateSupplier(
            retry,
            CircuitBreaker.decorateSupplier(circuitBreaker, operation)
        ).get();
    }

    public CircuitBreaker getCircuitBreaker(String name) {
        return circuitBreakerRegistry.circuitBreaker(name);
    }

    public CircuitBreaker.Metrics getCircuitBreakerMetrics(String name) {
        return circuitBreakerRegistry.circuitBreaker(name).getMetrics();
    }

    public boolean isCircuitBreakerOpen(String name) {
        return circuitBreakerRegistry.circuitBreaker(name).getState() == CircuitBreaker.State.OPEN;
    }

    public void resetCircuitBreaker(String name) {
        circuitBreakerRegistry.circuitBreaker(name).reset();
        LOG.info("Circuit breaker '{}' reset", name);
    }

    public static class Builder {
        private int maxRetries = 3;
        private long waitDurationMillis = 1000;
        private int failureRateThreshold = 50;
        private int slowCallRateThreshold = 100;
        private int slowCallDurationMillis = 5000;
        private int circuitBreakerWaitDurationSeconds = 30;
        private int slidingWindowSize = 10;

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder waitDurationMillis(long waitDurationMillis) {
            this.waitDurationMillis = waitDurationMillis;
            return this;
        }

        public Builder failureRateThreshold(int failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
            return this;
        }

        public Builder slowCallRateThreshold(int slowCallRateThreshold) {
            this.slowCallRateThreshold = slowCallRateThreshold;
            return this;
        }

        public Builder slowCallDurationMillis(int slowCallDurationMillis) {
            this.slowCallDurationMillis = slowCallDurationMillis;
            return this;
        }

        public Builder circuitBreakerWaitDurationSeconds(int circuitBreakerWaitDurationSeconds) {
            this.circuitBreakerWaitDurationSeconds = circuitBreakerWaitDurationSeconds;
            return this;
        }

        public Builder slidingWindowSize(int slidingWindowSize) {
            this.slidingWindowSize = slidingWindowSize;
            return this;
        }

        public ResilienceHelper build() {
            return new ResilienceHelper(
                maxRetries, waitDurationMillis, 
                failureRateThreshold, slowCallRateThreshold,
                slowCallDurationMillis, circuitBreakerWaitDurationSeconds,
                slidingWindowSize
            );
        }
    }
}
