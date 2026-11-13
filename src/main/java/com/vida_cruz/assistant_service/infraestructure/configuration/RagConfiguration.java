package com.vida_cruz.assistant_service.infraestructure.configuration;

import com.vida_cruz.assistant_service.domain.api.AskClinicalAssistant;
import com.vida_cruz.assistant_service.domain.model.ProtocolCorpus;
import com.vida_cruz.assistant_service.domain.spi.ClinicalAnswerGenerator;
import com.vida_cruz.assistant_service.domain.spi.ProtocolRetriever;
import com.vida_cruz.assistant_service.domain.usecase.AskClinicalAssistantUseCase;
import com.vida_cruz.assistant_service.infraestructure.adapters.generation.SpringAiClinicalAnswerGenerator;
import com.vida_cruz.assistant_service.infraestructure.adapters.resilience.ProviderGuard;
import com.vida_cruz.assistant_service.infraestructure.adapters.retrieval.VectorStoreProtocolRetriever;
import org.springframework.ai.chat.model.ChatModel;
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
                                               RagProperties properties, ProviderGuard guard) {
        ProtocolRetriever retriever = new VectorStoreProtocolRetriever(vectorStore, corpus,
                properties.topK(), properties.similarityThreshold());
        return question -> guard.protect(retriever.retrieve(question));
    }

    @Bean
    public ClinicalAnswerGenerator clinicalAnswerGenerator(ChatModel chatModel, ProviderGuard guard) {
        ClinicalAnswerGenerator generator = new SpringAiClinicalAnswerGenerator(chatModel);
        return (question, context) -> guard.protect(generator.generate(question, context));
    }

    @Bean
    public AskClinicalAssistant askClinicalAssistant(ProtocolRetriever retriever,
                                                     ClinicalAnswerGenerator generator,
                                                     ProtocolCorpus corpus) {
        return new AskClinicalAssistantUseCase(retriever, generator, corpus);
    }
}