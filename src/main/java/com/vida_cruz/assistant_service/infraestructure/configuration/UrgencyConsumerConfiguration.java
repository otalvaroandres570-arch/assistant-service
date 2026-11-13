package com.vida_cruz.assistant_service.infraestructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vida_cruz.assistant_service.domain.api.TrackUrgency;
import com.vida_cruz.assistant_service.infraestructure.entrypoints.messaging.KafkaReceiverFactory;
import com.vida_cruz.assistant_service.infraestructure.entrypoints.messaging.StreamRunner;
import com.vida_cruz.assistant_service.infraestructure.entrypoints.messaging.UrgencyEventConsumer;
import com.vida_cruz.assistant_service.infraestructure.entrypoints.messaging.UrgencyEventParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KafkaConsumerProperties.class)
@ConditionalOnProperty(prefix = "assistant.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UrgencyConsumerConfiguration {

    @Bean
    public UrgencyEventParser urgencyEventParser(ObjectMapper objectMapper) {
        return new UrgencyEventParser(objectMapper);
    }

    @Bean
    public UrgencyEventConsumer urgencyEventConsumer(KafkaConsumerProperties properties,
                                                     UrgencyEventParser parser,
                                                     TrackUrgency trackUrgency) {
        return new UrgencyEventConsumer(
                KafkaReceiverFactory.create(properties.bootstrapServers(), properties.groupId(), properties.topic()),
                parser,
                trackUrgency);
    }

    @Bean
    public StreamRunner urgencyConsumerRunner(UrgencyEventConsumer consumer) {
        return new StreamRunner("Urgency event consumer", consumer::consume);
    }
}