package com.vida_cruz.assistant_service.infraestructure.entrypoints.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vida_cruz.assistant_service.domain.model.UrgencySummary;
import com.vida_cruz.assistant_service.domain.model.enums.TriageLevel;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public record UrgencyStatusResponse(
        @JsonProperty("total") long total,
        @JsonProperty("porNivel") Map<String, Long> byLevel
) {

    public static UrgencyStatusResponse from(UrgencySummary summary) {
        Map<String, Long> byLevel = Arrays.stream(TriageLevel.values())
                .collect(Collectors.toMap(
                        TriageLevel::name,
                        level -> summary.activeByLevel().get(level),
                        (first, second) -> first,
                        LinkedHashMap::new)); // levels always in severity order
        return new UrgencyStatusResponse(summary.total(), byLevel);
    }
}