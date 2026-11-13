package com.vida_cruz.assistant_service.infraestructure.entrypoints.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultationRequest(

        @JsonProperty("pregunta")
        @NotBlank(message = "es obligatoria")
        @Size(max = 1000, message = "debe tener máximo 1000 caracteres")
        String question
) {
}