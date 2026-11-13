package com.vida_cruz.assistant_service.infraestructure.entrypoints.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vida_cruz.assistant_service.domain.exceptions.AssistantUnavailableException;
import com.vida_cruz.assistant_service.infraestructure.entrypoints.error.ErrorResponse.FieldViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex) {
        List<FieldViolation> violations = ex.getFieldErrors().stream()
                .map(error -> new FieldViolation(clientFieldName(ex, error.getField()), error.getDefaultMessage()))
                .toList();
        String message = violations.stream()
                .findFirst()
                .map(violation -> "Campo '%s' inválido: %s".formatted(violation.field(), violation.message()))
                .orElse("Solicitud inválida");

        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, violations);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableInput(ServerWebInputException ex) {
        log.debug("Unreadable request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "El cuerpo de la solicitud es ilegible o tiene un formato inválido", null);
    }

    /** Degraded mode: the language model (or its embeddings) cannot be used right now. */
    @ExceptionHandler(AssistantUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(AssistantUnavailableException ex) {
        log.warn("Assistant unavailable: {}", Optional.ofNullable(ex.getCause()).orElse(ex).toString());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "ASSISTANT_UNAVAILABLE", ex.getMessage(), null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        String message = Optional.ofNullable(ex.getReason()).orElse("Error en la solicitud");
        return ResponseEntity.status(status).body(ErrorResponse.of(status, "HTTP_" + status, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrió un error interno. Intente nuevamente", null);
    }

    /** The name of the field in the JSON the client sent, not the Java property name. */
    private static String clientFieldName(WebExchangeBindException ex, String javaName) {
        return Optional.ofNullable(ex.getBindingResult().getTarget())
                .map(Object::getClass)
                .filter(Class::isRecord)
                .flatMap(type -> Arrays.stream(type.getRecordComponents())
                        .filter(component -> component.getName().equals(javaName))
                        .findFirst())
                .map(component -> component.getAccessor().getAnnotation(JsonProperty.class))
                .map(JsonProperty::value)
                .orElse(javaName);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                List<FieldViolation> violations) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), code, message, violations));
    }

    /** The state store (Redis) cannot be reached: the urgency view is temporarily unavailable. */
    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ErrorResponse> handleStateStoreUnavailable(DataAccessResourceFailureException ex) {
        log.warn("State store unavailable: {}", ex.toString());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "STATE_STORE_UNAVAILABLE",
                "El estado de las urgencias no está disponible en este momento. Intente de nuevo más tarde", null);
    }
}