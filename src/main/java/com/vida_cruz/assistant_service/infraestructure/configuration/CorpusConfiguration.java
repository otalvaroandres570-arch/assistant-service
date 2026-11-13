package com.vida_cruz.assistant_service.infraestructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vida_cruz.assistant_service.domain.model.ProtocolCorpus;
import com.vida_cruz.assistant_service.infraestructure.adapters.corpus.ClasspathProtocolCorpusLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CorpusConfiguration {

    @Bean
    public ProtocolCorpus protocolCorpus(ObjectMapper objectMapper,
                                         @Value("${assistant.corpus.location:protocols/protocols.json}") String location) {
        return new ClasspathProtocolCorpusLoader(objectMapper).load(location);
    }
}