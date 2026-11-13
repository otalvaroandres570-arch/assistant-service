package com.vida_cruz.assistant_service.domain.model.enums;

import java.util.Arrays;
import java.util.Optional;

public enum PatientStatus {
    REGISTRADO, EN_CONSULTA, ALTA;

    public boolean isActive() {
        return this != ALTA;
    }

    public static Optional<PatientStatus> parse(String text) {
        return Arrays.stream(values())
                .filter(status -> status.name().equals(text))
                .findFirst();
    }
}