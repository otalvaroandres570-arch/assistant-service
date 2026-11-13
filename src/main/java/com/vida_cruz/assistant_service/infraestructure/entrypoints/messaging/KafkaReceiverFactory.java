package com.vida_cruz.assistant_service.infraestructure.entrypoints.messaging;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class KafkaReceiverFactory {

    private KafkaReceiverFactory() {
    }

    public static KafkaReceiver<String, String> create(String bootstrapServers, String groupId, String topic) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, groupId,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return KafkaReceiver.create(ReceiverOptions.<String, String>create(config)
                .subscription(List.of(topic))
                .commitInterval(Duration.ofSeconds(1)));
    }
}