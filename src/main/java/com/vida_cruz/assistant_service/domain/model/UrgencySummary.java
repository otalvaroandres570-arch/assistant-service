package com.vida_cruz.assistant_service.domain.model;

import com.vida_cruz.assistant_service.domain.model.enums.TriageLevel;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public record UrgencySummary(Map<TriageLevel, Long> activeByLevel) {

    public UrgencySummary {
        Map<TriageLevel, Long> source = Optional.ofNullable(activeByLevel).orElse(Map.of());
        activeByLevel = Arrays.stream(TriageLevel.values())
                .collect(Collectors.toUnmodifiableMap(
                        Function.identity(),
                        level -> Math.max(0L, source.getOrDefault(level, 0L))));
    }

    public long total() {
        return activeByLevel.values().stream().mapToLong(Long::longValue).sum();
    }
}