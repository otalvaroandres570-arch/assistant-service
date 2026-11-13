package com.vida_cruz.assistant_service.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolCorpusTest {

    private static final ProtocolSection DEFINITION =
            new ProtocolSection("PROTO-001", "Crisis Hipertensiva", "Definición", "Presión arterial ≥ 180/120 mmHg.");
    private static final ProtocolSection MEASURES =
            new ProtocolSection("PROTO-001", "Crisis Hipertensiva", "Medidas iniciales", "Reconfirmar la presión.");
    private static final ProtocolSection CRITERIA =
            new ProtocolSection("PROTO-002", "Sepsis", "Criterios", "2 o más de: frecuencia respiratoria ≥ 22.");

    private final ProtocolCorpus corpus = new ProtocolCorpus(List.of(DEFINITION, MEASURES, CRITERIA));

    @Test
    void shouldFindASectionByItsCitation() {
        assertEquals(Optional.of(MEASURES), corpus.find(new Citation("PROTO-001", "Medidas iniciales")));
    }

    @Test
    void shouldContainExistingCitations() {
        assertTrue(corpus.contains(DEFINITION.citation()));
        assertTrue(corpus.contains(CRITERIA.citation()));
    }

    @ParameterizedTest(name = "{0} / {1} does not exist")
    @CsvSource({
            "PROTO-999,Definición",
            "PROTO-001,Dosis",
            "PROTO-002,Definición",
            "proto-001,Definición"
    })
    void shouldNotContainCitationsToUnknownSources(String protocol, String section) {
        assertFalse(corpus.contains(new Citation(protocol, section)));
    }

    @Test
    void shouldListTheProtocolsWithoutRepeatingThemAndInOrder() {
        assertEquals(List.of("PROTO-001", "PROTO-002"), List.copyOf(corpus.protocolIds()));
        assertEquals(Set.of("PROTO-001", "PROTO-002"), corpus.protocolIds());
    }

    @Test
    void shouldRejectAnEmptyCorpus() {
        assertThrows(IllegalArgumentException.class, () -> new ProtocolCorpus(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolCorpus(null));
    }

    @Test
    void shouldRejectDuplicatedSections() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProtocolCorpus(List.of(DEFINITION, DEFINITION)));
    }

    @Test
    void shouldNotBeAffectedByLaterChangesToTheListItWasBuiltFrom() {
        List<ProtocolSection> source = new ArrayList<>(List.of(DEFINITION));
        ProtocolCorpus snapshot = new ProtocolCorpus(source);

        source.add(CRITERIA);

        assertEquals(1, snapshot.sections().size());
    }

    @ParameterizedTest(name = "blank {0} is rejected")
    @CsvSource({"protocolId", "protocolTitle", "name", "content"})
    void shouldRejectBlankFieldsInASection(String blankField) {
        assertThrows(IllegalArgumentException.class, () -> new ProtocolSection(
                blankField.equals("protocolId") ? " " : "PROTO-001",
                blankField.equals("protocolTitle") ? "" : "Crisis Hipertensiva",
                blankField.equals("name") ? " " : "Definición",
                blankField.equals("content") ? "" : "Contenido"));
    }

    @Test
    void shouldRejectBlankCitations() {
        assertThrows(IllegalArgumentException.class, () -> new Citation("", "Definición"));
        assertThrows(IllegalArgumentException.class, () -> new Citation("PROTO-001", null));
    }
}