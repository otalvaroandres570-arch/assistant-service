package com.vida_cruz.assistant_service.infraestructure.adapters.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

public class ProviderGuard {

    private final CircuitBreaker circuitBreaker;
    private final Duration timeout;

    public ProviderGuard(CircuitBreaker circuitBreaker, Duration timeout) {
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker);
        this.timeout = Objects.requireNonNull(timeout);
    }

    /** Fails if the provider takes longer than the timeout; timeouts and errors count towards opening the circuit. */
    public <T> Mono<T> protect(Mono<T> providerCall) {
        return providerCall
                .timeout(timeout)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
    }
}