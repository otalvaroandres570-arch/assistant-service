package com.vida_cruz.assistant_service.infraestructure.configuration;

import com.vida_cruz.assistant_service.infraestructure.adapters.resilience.ProviderGuard;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(ResilienceProperties.class)
public class ResilienceConfiguration {

    @Bean
    public CircuitBreaker geminiCircuitBreaker(ResilienceProperties properties) {
        CircuitBreaker circuitBreaker = CircuitBreaker.of("gemini", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(properties.slidingWindowSize())
                .minimumNumberOfCalls(properties.minimumNumberOfCalls())
                .failureRateThreshold(properties.failureRateThreshold())
                .waitDurationInOpenState(properties.openStateDuration())
                .permittedNumberOfCallsInHalfOpenState(2)
                .build());

        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn("Gemini circuit breaker: {}", event.getStateTransition()));
        return circuitBreaker;
    }

    @Bean
    public ProviderGuard providerGuard(CircuitBreaker geminiCircuitBreaker, ResilienceProperties properties) {
        return new ProviderGuard(geminiCircuitBreaker, properties.timeout());
    }
}