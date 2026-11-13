package com.vida_cruz.assistant_service.infraestructure.configuration;

import com.vida_cruz.assistant_service.domain.model.ProtocolCorpus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Only the corpus configuration and Jackson: the full context would require R2DBC/Kafka infrastructure.
@SpringBootTest(classes = {CorpusConfiguration.class, JacksonAutoConfiguration.class})
class CorpusConfigurationIntegrationTest {

    @Autowired
    private ProtocolCorpus corpus;

    @Test
    void shouldLoadTheCorpusAtStartupAsABean() {
        assertEquals(7, corpus.sections().size());
        assertEquals(3, corpus.protocolIds().size());
    }
}