package com.vida_cruz.assistant_service.infraestructure.adapters.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProviderGuardTest {

    private static CircuitBreaker breaker() {
        return CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build());
    }

    @Test
    void shouldOpenTheCircuitAfterRepeatedFailuresAndStopCallingTheProvider() {
        ProviderGuard guard = new ProviderGuard(breaker(), Duration.ofSeconds(1));
        AtomicInteger providerCalls = new AtomicInteger();
        Supplier<Mono<String>> failingCall = () -> guard.protect(Mono.defer(() -> {
            providerCalls.incrementAndGet();
            return Mono.error(new IllegalStateException("provider down"));
        }));

        StepVerifier.create(failingCall.get()).expectError(IllegalStateException.class).verify();
        StepVerifier.create(failingCall.get()).expectError(IllegalStateException.class).verify();
        StepVerifier.create(failingCall.get()).expectError(CallNotPermittedException.class).verify();

        assertEquals(2, providerCalls.get(), "with the circuit open the provider is not called");
    }

    @Test
    void shouldTreatATimeoutAsAFailure() {
        ProviderGuard guard = new ProviderGuard(breaker(), Duration.ofMillis(50));

        StepVerifier.create(guard.protect(Mono.never()))
                .expectError(TimeoutException.class)
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void shouldLetSuccessfulCallsThrough() {
        ProviderGuard guard = new ProviderGuard(breaker(), Duration.ofSeconds(1));

        StepVerifier.create(guard.protect(Mono.just("ok")))
                .expectNext("ok")
                .verifyComplete();
    }
}