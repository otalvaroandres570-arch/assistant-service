package com.vida_cruz.assistant_service.infraestructure.adapters.retrieval;

import com.vida_cruz.assistant_service.domain.model.ProtocolCorpus;
import com.vida_cruz.assistant_service.domain.model.ProtocolSection;
import com.vida_cruz.assistant_service.domain.model.RetrievedSection;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VectorStoreProtocolRetrieverTest {

    private static final ProtocolSection MEASURES = new ProtocolSection(
            "PROTO-001", "Crisis Hipertensiva", "Medidas iniciales", "Reconfirmar la presión.");
    private static final ProtocolSection CRITERIA = new ProtocolSection(
            "PROTO-002", "Sepsis", "Criterios", "2 o más de: frecuencia respiratoria ≥ 22.");
    private static final ProtocolCorpus CORPUS = new ProtocolCorpus(List.of(MEASURES, CRITERIA));

    @Test
    void shouldReturnCorpusSectionsBestFirstAndEmbedTheCorpusOnlyOnce() {
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(match(MEASURES, 0.82), match(CRITERIA, 0.61)));
        VectorStoreProtocolRetriever retriever = new VectorStoreProtocolRetriever(vectorStore, CORPUS, 3, 0.0);

        StepVerifier.create(retriever.retrieve("medidas iniciales"))
                .expectNext(List.of(new RetrievedSection(MEASURES, 0.82), new RetrievedSection(CRITERIA, 0.61)))
                .verifyComplete();
        StepVerifier.create(retriever.retrieve("otra pregunta"))
                .expectNextCount(1)
                .verifyComplete();

        verify(vectorStore, times(1)).add(anyList());
    }

    @Test
    void shouldRetryTheEmbeddingAfterAFailureAndIgnoreDocumentsOutsideTheCorpus() {
        VectorStore vectorStore = mock(VectorStore.class);
        doThrow(new IllegalStateException("gemini down")).doNothing().when(vectorStore).add(anyList());
        Document foreign = Document.builder().id("x").text("x").score(0.9)
                .metadata(Map.of("protocol", "PROTO-999", "section", "Dosis")).build();
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(foreign, match(MEASURES, 0.7)));
        VectorStoreProtocolRetriever retriever = new VectorStoreProtocolRetriever(vectorStore, CORPUS, 3, 0.0);

        StepVerifier.create(retriever.retrieve("pregunta"))
                .expectErrorMessage("gemini down")
                .verify();
        StepVerifier.create(retriever.retrieve("pregunta"))
                .expectNext(List.of(new RetrievedSection(MEASURES, 0.7)))
                .verifyComplete();

        verify(vectorStore, times(2)).add(anyList());
    }

    private static Document match(ProtocolSection section, double score) {
        return Document.builder()
                .id(section.protocolId() + "|" + section.name())
                .text(section.content())
                .score(score)
                .metadata(Map.of("protocol", section.protocolId(), "section", section.name()))
                .build();
    }
}