package com.vida_cruz.assistant_service.domain.exceptions;

public class AssistantUnavailableException extends RuntimeException {
    public AssistantUnavailableException(Throwable cause) {
        super("El asistente clínico no está disponible en este momento. Intente de nuevo más tarde", cause);
    }
}
