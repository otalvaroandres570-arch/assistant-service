package com.vida_cruz.assistant_service.domain.model;

import java.util.Objects;
import java.util.UUID;
import com.vida_cruz.assistant_service.domain.model.enums.PatientStatus;
import com.vida_cruz.assistant_service.domain.model.enums.TriageLevel;

public record UrgencyEvent(UUID patientId, TriageLevel level, PatientStatus status) {

    public UrgencyEvent {
        Objects.requireNonNull(patientId, "patientId must not be null");
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}