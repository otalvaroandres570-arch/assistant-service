package com.vida_cruz.assistant_service.infraestructure.entrypoints.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vida_cruz.assistant_service.domain.model.UrgencyEvent;
import com.vida_cruz.assistant_service.domain.model.enums.PatientStatus;
import com.vida_cruz.assistant_service.domain.model.enums.TriageLevel;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Reads the events of the "triaje.eventos" contract, version 1. Anything it cannot trust is discarded. */
public class UrgencyEventParser {

    private static final int SUPPORTED_VERSION = 1;

    private final ObjectMapper objectMapper;

    public UrgencyEventParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public Optional<UrgencyEvent> parse(String payload) {
        return Optional.ofNullable(payload)
                .flatMap(this::read)
                .filter(message -> message.version() == SUPPORTED_VERSION)
                .flatMap(message -> Optional.ofNullable(message.patientId())
                        .flatMap(patientId -> TriageLevel.parse(message.level())
                                .flatMap(level -> PatientStatus.parse(message.status())
                                        .map(status -> new UrgencyEvent(patientId, level, status)))));
    }

    private Optional<TriageEventMessage> read(String payload) {
        try {
            return Optional.ofNullable(objectMapper.readValue(payload, TriageEventMessage.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    /** Only the fields the assistant needs; the rest of the contract is ignored on purpose. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record TriageEventMessage(int version, UUID patientId, String level, String status) {
    }
}