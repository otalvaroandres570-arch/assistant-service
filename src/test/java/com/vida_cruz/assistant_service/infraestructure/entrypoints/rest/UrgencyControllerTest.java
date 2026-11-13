package com.vida_cruz.assistant_service.infraestructure.entrypoints.rest;

import com.vida_cruz.assistant_service.domain.api.TrackUrgency;
import com.vida_cruz.assistant_service.domain.model.UrgencySummary;
import com.vida_cruz.assistant_service.domain.model.enums.TriageLevel;
import com.vida_cruz.assistant_service.infraestructure.entrypoints.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.Mockito.when;

@WebFluxTest(UrgencyController.class)
@Import(GlobalExceptionHandler.class)
class UrgencyControllerTest {

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private TrackUrgency trackUrgency;

    @Test
    void shouldReturnTheActivePatientsOfEveryLevelIncludingThoseWithNone() {
        when(trackUrgency.current()).thenReturn(Mono.just(
                new UrgencySummary(Map.of(TriageLevel.CRITICO, 2L, TriageLevel.URGENTE_2, 1L))));

        client.get().uri("/asistente/estado-urgencias").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(3)
                .jsonPath("$.porNivel.CRITICO").isEqualTo(2)
                .jsonPath("$.porNivel.URGENTE").isEqualTo(0)
                .jsonPath("$.porNivel.URGENTE_2").isEqualTo(1)
                .jsonPath("$.porNivel.NO_URGENTE").isEqualTo(0)
                .jsonPath("$.porNivel.NO_URGENTE_2").isEqualTo(0);
    }

    @Test
    void shouldAnswer503WhenTheStateStoreIsUnreachable() {
        when(trackUrgency.current()).thenReturn(Mono.error(new RedisConnectionFailureException("redis down")));

        client.get().uri("/asistente/estado-urgencias").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("STATE_STORE_UNAVAILABLE");
    }
}