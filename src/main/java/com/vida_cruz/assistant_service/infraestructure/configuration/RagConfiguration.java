package com.vida_cruz.assistant_service.infraestructure.configuration;

import com.vida_cruz.assistant_service.domain.api.AskClinicalAssistant;
import com.vida_cruz.assistant_service.domain.exceptions.AssistantUnavailableException;
import com.vida_cruz.assistant_service.domain.model.ClinicalAnswer;
import com.vida_cruz.assistant_service.domain.model.ProtocolCorpus;
import com.vida_cruz.assistant_service.domain.spi.ClinicalAnswerGenerator;
import com.vida_cruz.assistant_service.domain.spi.ProtocolRetriever;
import com.vida_cruz.assistant_service.domain.usecase.AskClinicalAssistantUseCase;
import com.vida_cruz.assistant_service.infraestructure.adapters.generation.SpringAiClinicalAnswerGenerator;
import com.vida_cruz.assistant_service.infraestructure.adapters.observability.ProviderTimer;
import com.vida_cruz.assistant_service.infraestructure.adapters.resilience.ProviderGuard;
import com.vida_cruz.assistant_service.infraestructure.adapters.retrieval.VectorStoreProtocolRetriever;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Stream;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfiguration {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public ProviderTimer providerTimer(MeterRegistry registry) {
        return new ProviderTimer(registry);
    }

    @Bean
    public ProtocolRetriever protocolRetriever(VectorStore vectorStore, ProtocolCorpus corpus,
                                               RagProperties properties, ProviderGuard guard, ProviderTimer timer) {
        ProtocolRetriever retriever = new VectorStoreProtocolRetriever(vectorStore, corpus,
                properties.topK(), properties.similarityThreshold());
        return question -> timer.time("assistant.retrieval.latency", guard.protect(retriever.retrieve(question)));
    }

    @Bean
    public ClinicalAnswerGenerator clinicalAnswerGenerator(ChatModel chatModel, ProviderGuard guard,
                                                           ProviderTimer timer) {
        ClinicalAnswerGenerator generator = new SpringAiClinicalAnswerGenerator(chatModel);
        return (question, context) ->
                timer.time("assistant.llm.latency", guard.protect(generator.generate(question, context)));
    }

    /** Counts every consultation by its result. The three series exist from the start, at zero. */
    @Bean
    public AskClinicalAssistant askClinicalAssistant(ProtocolRetriever retriever,
                                                     ClinicalAnswerGenerator generator,
                                                     ProtocolCorpus corpus,
                                                     MeterRegistry registry) {
        AskClinicalAssistant useCase = new AskClinicalAssistantUseCase(retriever, generator, corpus);
        Stream.of("answered", "no_protocol", "unavailable").forEach(result -> consultations(registry, result));

        return question -> useCase.ask(question)
                .doOnNext(answer -> consultations(registry, resultOf(answer)).increment())
                .doOnError(AssistantUnavailableException.class,
                        error -> consultations(registry, "unavailable").increment());
    }

    private static Counter consultations(MeterRegistry registry, String result) {
        return registry.counter("assistant.consultations", "result", result);
    }

    private static String resultOf(ClinicalAnswer answer) {
        return switch (answer) {
            case ClinicalAnswer.Answered answered -> "answered";
            case ClinicalAnswer.NoProtocolAvailable none -> "no_protocol";
        };
    }
}