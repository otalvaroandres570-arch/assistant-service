package com.vida_cruz.assistant_service.domain.model;

import java.util.List;
import java.util.Optional;

/** What the language model produced, before it is validated. */
public record GeneratedAnswer(boolean answerable, String answer, List<Citation> citations) {

    public GeneratedAnswer {
        answer = Optional.ofNullable(answer).orElse("");
        citations = List.copyOf(Optional.ofNullable(citations).orElse(List.of()));
    }
}