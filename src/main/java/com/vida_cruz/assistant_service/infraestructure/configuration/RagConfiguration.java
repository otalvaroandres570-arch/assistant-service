package com.vida_cruz.assistant_service.infraestructure.configuration;

import com.vida_cruz.assistant_service.domain.model.ProtocolCorpus;
import com.vida_cruz.assistant_service.domain.spi.ProtocolRetriever;
import com.vida_cruz.assistant_service.infraestructure.adapters.retrieval.VectorStoreProtocolRetriever;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfiguration {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public ProtocolRetriever protocolRetriever(VectorStore vectorStore, ProtocolCorpus corpus,
                                               RagProperties properties) {
        return new VectorStoreProtocolRetriever(vectorStore, corpus,
                properties.topK(), properties.similarityThreshold());
    }
}