package com.vida_cruz.assistant_service.domain.spi;

import com.vida_cruz.assistant_service.domain.model.UrgencyEvent;
import com.vida_cruz.assistant_service.domain.model.UrgencySummary;
import reactor.core.publisher.Mono;

public interface UrgencyBoard {

    /**
     * Applies an event to the view. It must be idempotent (a repeated event changes nothing) and tolerate
     * out-of-order delivery (an event older than what is already known is ignored).
     */
    Mono<Void> apply(UrgencyEvent event);

    Mono<UrgencySummary> summary();
}