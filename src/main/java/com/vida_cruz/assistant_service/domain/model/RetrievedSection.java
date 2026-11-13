package com.vida_cruz.assistant_service.domain.model;

/** A corpus section found for a question, with its similarity score (higher is more similar). */
public record RetrievedSection(ProtocolSection section, double score) {
}
