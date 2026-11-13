package com.vida_cruz.assistant_service.domain.model.enums;

import java.util.Arrays;
import java.util.Optional;

public enum TriageLevel {
    CRITICO, URGENTE, URGENTE_2, NO_URGENTE, NO_URGENTE_2;

    public static Optional<TriageLevel> parse(String text) {
        return Arrays.stream(values())
                .filter(level -> level.name().equals(text))
                .findFirst();
    }
}