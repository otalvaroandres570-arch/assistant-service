package com.vida_cruz.assistant_service.domain.spi;

import com.vida_cruz.assistant_service.domain.model.GeneratedAnswer;
import com.vida_cruz.assistant_service.domain.model.ProtocolSection;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ClinicalAnswerGenerator {

    /**
     * Asks the model to answer using ONLY the given sections and to report which ones it relied on.
     * The result is a draft: the caller must validate it. Nothing happens until it is subscribed.
     */
    Mono<GeneratedAnswer> generate(String question, List<ProtocolSection> context);
}