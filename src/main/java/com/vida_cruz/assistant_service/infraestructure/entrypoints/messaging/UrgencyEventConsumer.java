package com.vida_cruz.assistant_service.infraestructure.entrypoints.messaging;

import com.vida_cruz.assistant_service.domain.api.TrackUrgency;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Objects;

/** Feeds the urgency view from the triage events. Never calls the triage API. */
@Slf4j
public class UrgencyEventConsumer {

    private final KafkaReceiver<String, String> receiver;
    private final UrgencyEventParser parser;
    private final TrackUrgency trackUrgency;

    public UrgencyEventConsumer(KafkaReceiver<String, String> receiver,
                                UrgencyEventParser parser,
                                TrackUrgency trackUrgency) {
        this.receiver = Objects.requireNonNull(receiver);
        this.parser = Objects.requireNonNull(parser);
        this.trackUrgency = Objects.requireNonNull(trackUrgency);
    }

    public Flux<Void> consume() {
        return Flux.defer(receiver::receive)
                .concatMap(this::process)
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(30))
                        .transientErrors(true)
                        .doBeforeRetry(signal -> log.warn("Urgency consumer failed, restarting: {}",
                                signal.failure().toString())));
    }

    private Mono<Void> process(ReceiverRecord<String, String> record) {
        return parser.parse(record.value())
                .map(trackUrgency::record)
                .orElseGet(() -> Mono.fromRunnable(() ->
                        log.warn("Skipping unreadable event (partition {}, offset {})", record.partition(), record.offset())))
                .doOnSuccess(done -> record.receiverOffset().acknowledge());
    }
}