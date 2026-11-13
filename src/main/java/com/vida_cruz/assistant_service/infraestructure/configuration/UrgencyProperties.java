package com.vida_cruz.assistant_service.infraestructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Optional;

@ConfigurationProperties(prefix = "assistant.urgency")
public record UrgencyProperties(
        @DefaultValue("7d") Duration dischargedPatientTtl
) {

    public UrgencyProperties {
        dischargedPatientTtl = Optional.ofNullable(dischargedPatientTtl)
                .filter(value -> !value.isZero() && !value.isNegative())
                .orElseThrow(() -> new IllegalArgumentException(
                        "assistant.urgency.discharged-patient-ttl must be positive"));
    }
}