package com.vida_cruz.assistant_service.infraestructure.entrypoints.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vida_cruz.assistant_service.domain.model.ClinicalAnswer;

import java.util.List;

public record ConsultationResponse(
        @JsonProperty("respuesta") String answer,
        @JsonProperty("fuentes") List<Source> sources
) {

    public static final String NO_PROTOCOL_NOTICE = "No existe un protocolo disponible para la pregunta consultada.";

    public record Source(
            @JsonProperty("protocolo") String protocol,
            @JsonProperty("seccion") String section
    ) {
    }

    public static ConsultationResponse from(ClinicalAnswer clinicalAnswer) {
        return switch (clinicalAnswer) {
            case ClinicalAnswer.Answered answered -> new ConsultationResponse(
                    answered.answer(),
                    answered.sources().stream()
                            .map(citation -> new Source(citation.protocol(), citation.section()))
                            .toList());
            case ClinicalAnswer.NoProtocolAvailable none -> new ConsultationResponse(NO_PROTOCOL_NOTICE, List.of());
        };
    }
}