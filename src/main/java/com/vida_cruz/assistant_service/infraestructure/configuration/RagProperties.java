package com.vida_cruz.assistant_service.infraestructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Optional;

@ConfigurationProperties(prefix = "assistant.rag")
public record RagProperties(
        @DefaultValue("3") int topK,
        @DefaultValue("0.65") double similarityThreshold
) {

    public RagProperties {
        topK = Optional.of(topK)
                .filter(value -> value > 0)
                .orElseThrow(() -> new IllegalArgumentException("assistant.rag.top-k must be positive"));
        similarityThreshold = Optional.of(similarityThreshold)
                .filter(value -> value >= 0.0 && value <= 1.0)
                .orElseThrow(() -> new IllegalArgumentException(
                        "assistant.rag.similarity-threshold must be between 0 and 1"));
    }
}