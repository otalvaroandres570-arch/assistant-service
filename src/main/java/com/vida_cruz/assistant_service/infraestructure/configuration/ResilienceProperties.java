package com.vida_cruz.assistant_service.infraestructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Optional;

@ConfigurationProperties(prefix = "assistant.resilience")
public record ResilienceProperties(
        @DefaultValue("20s") Duration timeout,
        @DefaultValue("10") int slidingWindowSize,
        @DefaultValue("5") int minimumNumberOfCalls,
        @DefaultValue("50") float failureRateThreshold,
        @DefaultValue("30s") Duration openStateDuration
) {

    public ResilienceProperties {
        timeout = positive(timeout, "timeout");
        openStateDuration = positive(openStateDuration, "open-state-duration");
        slidingWindowSize = positive(slidingWindowSize, "sliding-window-size");
        minimumNumberOfCalls = positive(minimumNumberOfCalls, "minimum-number-of-calls");
        failureRateThreshold = Optional.of(failureRateThreshold)
                .filter(value -> value > 0 && value <= 100)
                .orElseThrow(() -> new IllegalArgumentException(
                        "assistant.resilience.failure-rate-threshold must be in (0, 100]"));
    }

    private static Duration positive(Duration value, String name) {
        return Optional.ofNullable(value)
                .filter(duration -> !duration.isZero() && !duration.isNegative())
                .orElseThrow(() -> new IllegalArgumentException("assistant.resilience." + name + " must be positive"));
    }

    private static int positive(int value, String name) {
        return Optional.of(value)
                .filter(number -> number > 0)
                .orElseThrow(() -> new IllegalArgumentException("assistant.resilience." + name + " must be positive"));
    }
}