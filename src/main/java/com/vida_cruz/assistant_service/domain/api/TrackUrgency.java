package com.vida_cruz.assistant_service.domain.api;

import com.vida_cruz.assistant_service.domain.model.UrgencyEvent;
import com.vida_cruz.assistant_service.domain.model.UrgencySummary;
import reactor.core.publisher.Mono;

public interface TrackUrgency {

    Mono<Void> record(UrgencyEvent event);

    Mono<UrgencySummary> current();
}