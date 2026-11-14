package com.vida_cruz.assistant_service.infraestructure.entrypoints.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vida_cruz.assistant_service.domain.api.TrackUrgency;
import com.vida_cruz.assistant_service.domain.model.UrgencyEvent;
import com.vida_cruz.assistant_service.domain.model.UrgencySummary;
import com.vida_cruz.assistant_service.domain.model.enums.TriageLevel;
import com.vida_cruz.assistant_service.domain.usecase.TrackUrgencyUseCase;
import com.vida_cruz.assistant_service.infraestructure.adapters.urgency.RedisUrgencyBoard;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class UrgencyEventConsumerIntegrationTest {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.9.1");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static ReactiveStringRedisTemplate redis;

    private Disposable running;

    @BeforeAll
    static void connectToRedis() {
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

    @AfterEach
    void stopConsumer() {
        Optional.ofNullable(running).ifPresent(Disposable::dispose);
    }

    @Test
    void shouldBuildTheUrgencyViewFromTheTriageEvents() throws Exception {
        String topic = createTopic();
        RedisUrgencyBoard board = new RedisUrgencyBoard(redis, Duration.ofDays(7));
        startConsumer(topic, new TrackUrgencyUseCase(board));
        UUID discharged = UUID.randomUUID();
        UUID inConsultation = UUID.randomUUID();
        UUID waiting = UUID.randomUUID();
        UUID dischargedFirst = UUID.randomUUID();

        publish(topic,
                message(discharged, "CRITICO", "REGISTRADO"),
                message(inConsultation, "CRITICO", "REGISTRADO"),
                message(waiting, "URGENTE", "REGISTRADO"),
                "{ this is not json",                                // corrupt message: skipped, the rest continue
                message(discharged, "CRITICO", "REGISTRADO"),        // escalation or duplicate: no effect
                message(inConsultation, "CRITICO", "EN_CONSULTA"),
                message(discharged, "CRITICO", "ALTA"),
                message(dischargedFirst, "URGENTE", "ALTA"),         // the discharge arrives first...
                message(dischargedFirst, "URGENTE", "REGISTRADO"));  // ...and the registration late

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> assertEquals(
                new UrgencySummary(Map.of(TriageLevel.CRITICO, 1L, TriageLevel.URGENTE, 1L)),
                board.summary().block()));
    }

    @Test
    void shouldRecoverFromAFailureWithoutLosingEvents() throws Exception {
        String topic = createTopic();
        List<UrgencyEvent> recorded = new CopyOnWriteArrayList<>();
        TrackUrgency flaky = getFlaky(recorded);
        startConsumer(topic, flaky);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        publish(topic, message(first, "CRITICO", "REGISTRADO"), message(second, "URGENTE", "REGISTRADO"));

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> assertTrue(
                recorded.stream().map(UrgencyEvent::patientId).collect(Collectors.toSet())
                        .containsAll(Set.of(first, second))));
    }

    private static @NonNull TrackUrgency getFlaky(List<UrgencyEvent> recorded) {
        AtomicInteger attempts = new AtomicInteger();
        return new TrackUrgency() {
            @Override

            public Mono<Void> record(UrgencyEvent event) {
                return attempts.getAndIncrement() == 0
                        ? Mono.error(new IllegalStateException("redis down"))
                        : Mono.fromRunnable(() -> recorded.add(event));
            }

            @Override
            public Mono<UrgencySummary> current() {
                return Mono.empty();
            }
        };
    }

    // ---------- helpers ----------

    private void startConsumer(String topic, TrackUrgency trackUrgency) {
        UrgencyEventConsumer consumer = new UrgencyEventConsumer(
                KafkaReceiverFactory.create(KAFKA.getBootstrapServers(), "test-" + UUID.randomUUID(), topic),
                new UrgencyEventParser(new ObjectMapper()),
                trackUrgency);
        running = consumer.consume().subscribe();
    }

    private static String message(UUID patientId, String level, String status) {
        return """
                {"eventId":"%s","type":"x","version":1,"occurredAt":"2026-11-13T10:00:00Z","patientId":"%s",
                 "level":"%s","status":"%s","previousStatus":null,"slaMinutes":5}
                """.formatted(UUID.randomUUID(), patientId, level, status);
    }

    private static void publish(String topic, String... payloads) throws Exception {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(config)) {
            for (String payload : payloads) {
                producer.send(new ProducerRecord<>(topic, UUID.randomUUID().toString(), payload)).get();
            }
        }
    }

    private static String createTopic() throws Exception {
        String topic = "triaje.eventos.test-" + UUID.randomUUID();
        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (AdminClient admin = AdminClient.create(config)) {
            admin.createTopics(List.of(new NewTopic(topic, 3, (short) 1))).all().get();
        }
        return topic;
    }
}