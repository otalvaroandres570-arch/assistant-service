package com.vida_cruz.assistant_service.infraestructure.adapters.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.util.Objects;

/** Times a call to the provider and tags the measurement with its outcome. */
public class ProviderTimer {

    private final MeterRegistry registry;

    public ProviderTimer(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
    }

    public <T> Mono<T> time(String metricName, Mono<T> call) {
        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start(registry);
            return call.doFinally(signal ->
                    sample.stop(Timer.builder(metricName).tag("outcome", outcome(signal)).register(registry)));
        });
    }

    private static String outcome(SignalType signal) {
        return switch (signal) {
            case ON_COMPLETE -> "success";
            case ON_ERROR -> "error";
            default -> "cancelled";
        };
    }
}