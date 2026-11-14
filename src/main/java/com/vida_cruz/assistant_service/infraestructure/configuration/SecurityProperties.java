package com.vida_cruz.assistant_service.infraestructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;
import java.util.Optional;

@ConfigurationProperties(prefix = "assistant.security")
public record SecurityProperties(
        @DefaultValue("true") boolean enabled,
        List<String> apiKeys
) {

    static final int MIN_KEY_LENGTH = 16;

    public SecurityProperties {
        apiKeys = Optional.ofNullable(apiKeys).orElse(List.of()).stream()
                .map(String::strip)
                .filter(key -> !key.isEmpty())
                .toList();
        List<String> keys = apiKeys;

        Optional.of(enabled)
                .filter(isEnabled -> !isEnabled
                        || (!keys.isEmpty() && keys.stream().allMatch(key -> key.length() >= MIN_KEY_LENGTH)))
                .orElseThrow(() -> new IllegalArgumentException(
                        "assistant.security.api-keys must contain at least one key of %d or more characters (env ASSISTANT_SECURITY_APIKEYS)"
                                .formatted(MIN_KEY_LENGTH)));
    }
}