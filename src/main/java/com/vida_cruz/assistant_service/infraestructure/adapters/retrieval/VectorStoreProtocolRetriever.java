package com.vida_cruz.assistant_service.infraestructure.adapters.retrieval;

import com.vida_cruz.assistant_service.domain.model.Citation;
import com.vida_cruz.assistant_service.domain.model.ProtocolCorpus;
import com.vida_cruz.assistant_service.domain.model.ProtocolSection;
import com.vida_cruz.assistant_service.domain.model.RetrievedSection;
import com.vida_cruz.assistant_service.domain.spi.ProtocolRetriever;
import com.vida_cruz.assistant_service.domain.util.Texts;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class VectorStoreProtocolRetriever implements ProtocolRetriever {

    private static final String PROTOCOL_KEY = "protocol";
    private static final String SECTION_KEY = "section";
    private static final Duration FOREVER = Duration.ofDays(3650);

    private final VectorStore vectorStore;
    private final ProtocolCorpus corpus;
    private final int topK;
    private final double similarityThreshold;

    /** Embeds the corpus once, on first use. A failure is not remembered, so the next request retries. */
    private final Mono<Void> ingestion;

    public VectorStoreProtocolRetriever(VectorStore vectorStore, ProtocolCorpus corpus,
                                        int topK, double similarityThreshold) {
        this.vectorStore = Objects.requireNonNull(vectorStore);
        this.corpus = Objects.requireNonNull(corpus);
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
        this.ingestion = Mono.fromRunnable(() -> vectorStore.add(documentsOf(corpus)))
                .subscribeOn(Schedulers.boundedElastic()) // the embedding client is blocking
                .then()
                .cache(value -> FOREVER, error -> Duration.ZERO, () -> FOREVER);
    }

    @Override
    public Mono<List<RetrievedSection>> retrieve(String question) {
        return Mono.fromSupplier(() -> Texts.requireNotBlank(question, "question"))
                .flatMap(validQuestion -> ingestion.then(search(validQuestion)))
                .map(this::toRetrievedSections);
    }

    private Mono<List<Document>> search(String question) {
        return Mono.fromCallable(() -> vectorStore.similaritySearch(SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build()))
                .subscribeOn(Schedulers.boundedElastic()); // the vector store embeds the question: blocking
    }

    private List<RetrievedSection> toRetrievedSections(List<Document> documents) {
        return documents.stream()
                .flatMap(document -> toRetrievedSection(document).stream())
                .toList();
    }

    /** The text and citation always come from the corpus; documents that are not in it are discarded. */
    private Optional<RetrievedSection> toRetrievedSection(Document document) {
        return citationOf(document)
                .flatMap(corpus::find)
                .map(section -> new RetrievedSection(section, Optional.ofNullable(document.getScore()).orElse(0.0)));
    }

    private static Optional<Citation> citationOf(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return Optional.ofNullable(metadata.get(PROTOCOL_KEY))
                .flatMap(protocol -> Optional.ofNullable(metadata.get(SECTION_KEY))
                        .map(section -> new Citation(protocol.toString(), section.toString())));
    }

    private static List<Document> documentsOf(ProtocolCorpus corpus) {
        return corpus.sections().stream()
                .map(VectorStoreProtocolRetriever::toDocument)
                .toList();
    }

    private static Document toDocument(ProtocolSection section) {
        return Document.builder()
                .id(section.protocolId() + "|" + section.name())
                .text("%s. %s. %s".formatted(section.protocolTitle(), section.name(), section.content()))
                .metadata(Map.of(PROTOCOL_KEY, section.protocolId(), SECTION_KEY, section.name()))
                .build();
    }
}