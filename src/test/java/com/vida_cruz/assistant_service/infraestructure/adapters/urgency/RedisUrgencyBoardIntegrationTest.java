package com.vida_cruz.assistant_service.infraestructure.adapters.urgency;

import com.vida_cruz.assistant_service.domain.model.UrgencyEvent;
import com.vida_cruz.assistant_service.domain.model.UrgencySummary;
import com.vida_cruz.assistant_service.domain.model.enums.PatientStatus;
import com.vida_cruz.assistant_service.domain.model.enums.TriageLevel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;

import static com.vida_cruz.assistant_service.domain.model.enums.PatientStatus.ALTA;
import static com.vida_cruz.assistant_service.domain.model.enums.PatientStatus.EN_CONSULTA;
import static com.vida_cruz.assistant_service.domain.model.enums.PatientStatus.REGISTRADO;
import static com.vida_cruz.assistant_service.domain.model.enums.TriageLevel.CRITICO;
import static com.vida_cruz.assistant_service.domain.model.enums.TriageLevel.URGENTE;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class RedisUrgencyBoardIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static ReactiveStringRedisTemplate redis;

    private final RedisUrgencyBoard board = new RedisUrgencyBoard(redis, Duration.ofDays(7));

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redis = new ReactiveStringRedisTemplate(connectionFactory);
    }

    @AfterAll
    static void disconnect() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void emptyRedis() {
        redis.execute(connection -> connection.serverCommands().flushDb()).blockLast();
    }

    @Test
    void shouldCountTheActivePatientsOfEachLevel() {
        apply(UUID.randomUUID(), CRITICO, REGISTRADO);
        apply(UUID.randomUUID(), CRITICO, REGISTRADO);
        apply(UUID.randomUUID(), URGENTE, REGISTRADO);

        assertSummary(Map.of(CRITICO, 2L, URGENTE, 1L));
    }

    @Test
    void shouldKeepAPatientActiveInConsultationAndRemoveItOnDischarge() {
        UUID patient = UUID.randomUUID();

        apply(patient, CRITICO, REGISTRADO);
        assertSummary(Map.of(CRITICO, 1L));
        apply(patient, CRITICO, EN_CONSULTA);
        assertSummary(Map.of(CRITICO, 1L));
        apply(patient, CRITICO, ALTA);
        assertSummary(Map.of());

        Long ttlSeconds = redis.getExpire(RedisUrgencyBoard.patientKey(patient)).map(Duration::toSeconds).block();
        assertTrue(ttlSeconds > 0 && ttlSeconds <= Duration.ofDays(7).toSeconds(),
                "a discharged patient is remembered only for a limited time");
    }

    @Test
    void shouldIgnoreDuplicatesAndEscalations() {
        UUID patient = UUID.randomUUID();

        apply(patient, CRITICO, REGISTRADO);
        apply(patient, CRITICO, REGISTRADO); // duplicate, or the escalation event (same state)
        apply(patient, CRITICO, REGISTRADO);
        assertSummary(Map.of(CRITICO, 1L));

        apply(patient, CRITICO, ALTA);
        apply(patient, CRITICO, ALTA);
        assertSummary(Map.of());
    }

    @Test
    void shouldTolerateEventsArrivingOutOfOrder() {
        UUID patient = UUID.randomUUID();

        apply(patient, TriageLevel.CRITICO, PatientStatus.ALTA);          // the discharge arrives first
        apply(patient, TriageLevel.CRITICO, PatientStatus.REGISTRADO);    // late
        apply(patient, TriageLevel.CRITICO, PatientStatus.EN_CONSULTA);   // late

        assertSummary(Map.of());
    }

    @Test
    void shouldCountAPatientOnceEvenWhenTheSameEventIsAppliedConcurrently() {
        UrgencyEvent event = new UrgencyEvent(UUID.randomUUID(), TriageLevel.CRITICO, PatientStatus.REGISTRADO);

        Flux.range(0, 25).flatMap(i -> board.apply(event), 25).then().block();

        assertSummary(Map.of(TriageLevel.CRITICO, 1L));
    }

    private void apply(UUID patient, TriageLevel level, PatientStatus status) {
        board.apply(new UrgencyEvent(patient, level, status)).block();
    }

    private void assertSummary(Map<TriageLevel, Long> expected) {
        assertEquals(new UrgencySummary(expected), board.summary().block());
    }
}