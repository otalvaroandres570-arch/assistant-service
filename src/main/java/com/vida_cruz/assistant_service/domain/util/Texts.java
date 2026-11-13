package com.vida_cruz.assistant_service.domain.util;

import java.util.Optional;

public final class Texts {

    private Texts() {
    }

    public static String requireNotBlank(String value, String fieldName) {
        return Optional.ofNullable(value)
                .filter(text -> !text.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(fieldName + " must not be blank"));
    }
}