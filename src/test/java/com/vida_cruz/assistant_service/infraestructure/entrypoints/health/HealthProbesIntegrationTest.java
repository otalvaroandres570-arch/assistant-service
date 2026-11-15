package com.vida_cruz.assistant_service.infraestructure.entrypoints.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureWebTestClient
class HealthProbesIntegrationTest {

    @Autowired
    private HealthEndpointGroups groups;

    @Autowired
    private WebTestClient client;

    @Test
    void shouldNotMakeReadinessDependOnRedisKafkaOrTheLanguageModel() {
        assertTrue(groups.get("readiness").isMember("readinessState"));
        assertFalse(groups.get("readiness").isMember("redis"));
        assertFalse(groups.get("readiness").isMember("kafka"));
        assertTrue(groups.get("liveness").isMember("livenessState"));
    }

    @Test
    void shouldAnswerUpOnBothProbesEvenThoughRedisAndKafkaAreNotReachable() {
        client.get().uri("/actuator/health/liveness").exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("UP");
        client.get().uri("/actuator/health/readiness").exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("UP");
    }
}