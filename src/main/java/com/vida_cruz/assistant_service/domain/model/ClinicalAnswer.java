package com.vida_cruz.assistant_service.domain.model;

import java.util.List;

public sealed interface ClinicalAnswer {

    /** A grounded answer: its text and the protocol sections it relies on (never empty). */
    record Answered(String answer, List<Citation> sources) implements ClinicalAnswer {
    }

    /** No relevant protocol: the response carries only the notice, never generated clinical content. */
    record NoProtocolAvailable() implements ClinicalAnswer {
    }
}