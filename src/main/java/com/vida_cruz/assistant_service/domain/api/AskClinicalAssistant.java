package com.vida_cruz.assistant_service.domain.api;

import com.vida_cruz.assistant_service.domain.model.ClinicalAnswer;
import reactor.core.publisher.Mono;

public interface AskClinicalAssistant {

    /** Never emits an answer that was not validated. Emits AssistantUnavailableException if it cannot answer. */
    Mono<ClinicalAnswer> ask(String question);
}