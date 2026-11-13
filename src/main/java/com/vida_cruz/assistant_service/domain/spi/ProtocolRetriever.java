package com.vida_cruz.assistant_service.domain.spi;

import com.vida_cruz.assistant_service.domain.model.RetrievedSection;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ProtocolRetriever {

    /**
     * The corpus sections most similar to the question, best first. Empty when none reaches the
     * relevance threshold. Nothing is done until the returned Mono is subscribed.
     */
    Mono<List<RetrievedSection>> retrieve(String question);
}