package com.vida_cruz.assistant_service.infraestructure.entrypoints.rest;

import com.vida_cruz.assistant_service.domain.api.AskClinicalAssistant;
import com.vida_cruz.assistant_service.domain.exceptions.AssistantUnavailableException;
import com.vida_cruz.assistant_service.domain.model.Citation;
import com.vida_cruz.assistant_service.domain.model.ClinicalAnswer;
import com.vida_cruz.assistant_service.infraestructure.entrypoints.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(AssistantController.class)
@Import(GlobalExceptionHandler.class)
class AssistantControllerTest {

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private AskClinicalAssistant assistant;

    @Test
    void shouldReturnTheAnswerWithItsSources() {
        when(assistant.ask(anyString())).thenReturn(Mono.just(new ClinicalAnswer.Answered(
                "Reconfirmar la presión.", List.of(new Citation("PROTO-001", "Medidas iniciales")))));

        consult("¿Medidas iniciales?")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.respuesta").isEqualTo("Reconfirmar la presión.")
                .jsonPath("$.fuentes[0].protocolo").isEqualTo("PROTO-001")
                .jsonPath("$.fuentes[0].seccion").isEqualTo("Medidas iniciales");
    }

    @Test
    void shouldReturnOnlyTheNoticeAndNoSourcesWhenThereIsNoProtocol() {
        when(assistant.ask(anyString())).thenReturn(Mono.just(new ClinicalAnswer.NoProtocolAvailable()));

        consult("¿Tratamiento de la diabetes?")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.respuesta").isEqualTo("No existe un protocolo disponible para la pregunta consultada.")
                .jsonPath("$.fuentes").isEmpty();
    }

    @Test
    void shouldRejectAnEmptyQuestionNamingTheFieldTheClientSent() {
        consult("")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.violations[0].field").isEqualTo("pregunta");
    }

    @Test
    void shouldAnswer503WithAClearMessageWhenTheAssistantIsUnavailable() {
        when(assistant.ask(anyString())).thenReturn(
                Mono.error(new AssistantUnavailableException(new IllegalStateException("gemini down"))));

        consult("¿Medidas iniciales?")
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("ASSISTANT_UNAVAILABLE")
                .jsonPath("$.message").value(message ->
                        org.junit.jupiter.api.Assertions.assertTrue(message.toString().contains("no está disponible")));
    }

    private WebTestClient.ResponseSpec consult(String question) {
        return client.post().uri("/asistente/consulta")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("pregunta", question))
                .exchange();
    }
}