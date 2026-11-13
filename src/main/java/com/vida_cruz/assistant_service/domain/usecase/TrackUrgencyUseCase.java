package com.vida_cruz.assistant_service.domain.usecase;

import com.vida_cruz.assistant_service.domain.api.TrackUrgency;
import com.vida_cruz.assistant_service.domain.model.UrgencyEvent;
import com.vida_cruz.assistant_service.domain.model.UrgencySummary;
import com.vida_cruz.assistant_service.domain.spi.UrgencyBoard;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class TrackUrgencyUseCase implements TrackUrgency {

    private final UrgencyBoard board;

    public TrackUrgencyUseCase(UrgencyBoard board) {
        this.board = Objects.requireNonNull(board);
    }

    @Override
    public Mono<Void> record(UrgencyEvent event) {
        return board.apply(event);
    }

    @Override
    public Mono<UrgencySummary> current() {
        return board.summary();
    }
}