package com.vida_cruz.assistant_service.infraestructure.entrypoints.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vida_cruz.assistant_service.domain.model.UrgencyEvent;
import com.vida_cruz.assistant_service.domain.model.enums.PatientStatus;
import com.vida_cruz.assistant_service.domain.model.enums.TriageLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrgencyEventParserTest {

    private static final String PATIENT_ID = "3c608219-a3f2-4968-aa6d-a1180a5a36d8";

    private final UrgencyEventParser parser = new UrgencyEventParser(new ObjectMapper());

    @Test
    void shouldReadAnEventOfTheTriageContractIgnoringFieldsItDoesNotKnow() {
        String payload = """
                {"eventId":"7d6f3c1e-0b5a-4c1e-9a52-5f0e2f9d1a11","type":"paciente_en_consulta","version":1,
                 "occurredAt":"2026-11-13T10:00:00Z","patientId":"%s","level":"URGENTE_2","status":"EN_CONSULTA",
                 "previousStatus":"REGISTRADO","slaMinutes":30,"someFutureField":"x"}
                """.formatted(PATIENT_ID);

        assertEquals(
                Optional.of(new UrgencyEvent(UUID.fromString(PATIENT_ID), TriageLevel.URGENTE_2, PatientStatus.EN_CONSULTA)),
                parser.parse(payload));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @MethodSource("untrustworthyPayloads")
    void shouldDiscardWhatItCannotTrust(String payload) {
        assertTrue(parser.parse(payload).isEmpty());
    }

    static Stream<String> untrustworthyPayloads() {
        return Stream.of(
                "{ not json",
                event("\"version\":1,\"patientId\":\"not-a-uuid\",\"level\":\"CRITICO\",\"status\":\"REGISTRADO\""),
                event("\"version\":1,\"patientId\":\"" + PATIENT_ID + "\",\"level\":\"GRAVE\",\"status\":\"REGISTRADO\""),
                event("\"version\":1,\"patientId\":\"" + PATIENT_ID + "\",\"level\":\"CRITICO\",\"status\":\"HOSPITALIZADO\""),
                event("\"version\":2,\"patientId\":\"" + PATIENT_ID + "\",\"level\":\"CRITICO\",\"status\":\"REGISTRADO\""),
                event("\"version\":1,\"level\":\"CRITICO\",\"status\":\"REGISTRADO\""));
    }

    private static String event(String fields) {
        return "{" + fields + "}";
    }
}