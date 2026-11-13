package com.vida_cruz.assistant_service.infraestructure.configuration;

import com.vida_cruz.assistant_service.domain.util.Texts;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "assistant.kafka")
public record KafkaConsumerProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("localhost:9092") String bootstrapServers,
        @DefaultValue("triaje.eventos") String topic,
        @DefaultValue("assistant-service") String groupId
) {

    public KafkaConsumerProperties {
        bootstrapServers = Texts.requireNotBlank(bootstrapServers, "assistant.kafka.bootstrap-servers");
        topic = Texts.requireNotBlank(topic, "assistant.kafka.topic");
        groupId = Texts.requireNotBlank(groupId, "assistant.kafka.group-id");
    }
}