package com.vida_cruz.assistant_service.domain.model;

import jakarta.annotation.Nonnull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** The official set of protocols. An answer is only valid if every source it cites exists here (RN-05). */
public record ProtocolCorpus(List<ProtocolSection> sections) {

    public ProtocolCorpus {
        List<ProtocolSection> copy = Optional.ofNullable(sections)
                .map(List::copyOf)
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("The corpus must contain at least one section"));

        Set<Citation> distinctCitations = getDistinctCitations(copy);

        sections = getTheCorpusContainsDuplicatedSections(copy, distinctCitations);
    }

    @Nonnull
    private static List<ProtocolSection> getTheCorpusContainsDuplicatedSections(List<ProtocolSection> copy, Set<Citation> distinctCitations) {
        return Optional.of(copy)
                .filter(list -> list.size() == distinctCitations.size())
                .orElseThrow(() -> new IllegalArgumentException("The corpus contains duplicated sections"));
    }

    @Nonnull
    private static Set<Citation> getDistinctCitations(List<ProtocolSection> copy) {
        return copy.stream()
                .map(ProtocolSection::citation)
                .collect(Collectors.toSet());
    }

    public Optional<ProtocolSection> find(Citation citation) {
        return sections.stream()
                .filter(section -> section.citation().equals(citation))
                .findFirst();
    }

    public boolean contains(Citation citation) {
        return find(citation).isPresent();
    }

    public Set<String> protocolIds() {
        return sections.stream()
                .map(ProtocolSection::protocolId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}