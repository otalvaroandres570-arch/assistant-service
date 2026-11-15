package com.vida_cruz.assistant_service.infraestructure.adapters.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProviderTimerTest {

    @Test
    void shouldRecordOneMeasurementPerCallTaggedWithItsOutcome() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ProviderTimer timer = new ProviderTimer(registry);

        StepVerifier.create(timer.time("test.latency", Mono.just("ok"))).expectNext("ok").verifyComplete();
        StepVerifier.create(timer.time("test.latency", Mono.error(new IllegalStateException("down"))))
                .expectError(IllegalStateException.class).verify();

        assertEquals(1, registry.get("test.latency").tag("outcome", "success").timer().count());
        assertEquals(1, registry.get("test.latency").tag("outcome", "error").timer().count());
    }
}