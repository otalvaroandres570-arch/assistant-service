package com.vida_cruz.assistant_service.infraestructure.adapters.corpus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vida_cruz.assistant_service.domain.model.Citation;
import com.vida_cruz.assistant_service.domain.model.ProtocolCorpus;
import com.vida_cruz.assistant_service.domain.model.ProtocolSection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClasspathProtocolCorpusLoaderTest {

    private static final ClasspathProtocolCorpusLoader LOADER = new ClasspathProtocolCorpusLoader(new ObjectMapper());
    private static final ProtocolCorpus CORPUS = LOADER.load("protocols/protocols.json");

    @Test
    void shouldLoadTheThreeMandatoryProtocols() {
        assertEquals(Set.of("PROTO-001", "PROTO-002", "PROTO-003"), CORPUS.protocolIds());
    }

    @Test
    void shouldSplitEachProtocolIntoItsSectionsInOrder() {
        assertEquals(
                List.of("PROTO-001 / Definición", "PROTO-001 / Medidas iniciales", "PROTO-001 / Contraindicaciones",
                        "PROTO-002 / Criterios", "PROTO-002 / Acciones iniciales",
                        "PROTO-003 / Clasificación", "PROTO-003 / Rehidratación"),
                CORPUS.sections().stream().map(section -> section.protocolId() + " / " + section.name()).toList());
    }

    @ParameterizedTest(name = "{0} is titled \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "PROTO-001|Crisis Hipertensiva, manejo inicial",
            "PROTO-002|Sepsis, identificación temprana (qSOFA)",
            "PROTO-003|Deshidratación pediátrica"
    })
    void shouldKeepTheProtocolTitles(String protocolId, String expectedTitle) {
        assertEquals(Set.of(expectedTitle), CORPUS.sections().stream()
                .filter(section -> section.protocolId().equals(protocolId))
                .map(ProtocolSection::protocolTitle)
                .collect(java.util.stream.Collectors.toSet()));
    }

    @ParameterizedTest(name = "{0} / {1} is transcribed verbatim")
    @CsvSource(delimiter = '|', value = {
            "PROTO-001|Definición|Presión arterial ≥ 180/120 mmHg. Si hay daño de órgano diana es emergencia; si no, urgencia.",
            "PROTO-001|Medidas iniciales|Reconfirmar la presión tras 5 minutos de reposo. No reducir la presión más de un 25% en la primera hora. Vigilar signos de encefalopatía.",
            "PROTO-001|Contraindicaciones|Nifedipino sublingual. Reducción intravenosa rápida sin monitorización en cuidados intensivos.",
            "PROTO-002|Criterios|2 o más de: frecuencia respiratoria ≥ 22, alteración del estado mental, presión sistólica ≤ 100 mmHg.",
            "PROTO-002|Acciones iniciales|Lactato y hemocultivos antes del antibiótico. Antibiótico de amplio espectro en la primera hora. Fluidoterapia con 30 ml/kg de cristaloides si hay hipotensión o lactato ≥ 4.",
            "PROTO-003|Clasificación|Leve (pérdida 3-5% del peso), moderada (5-8%), severa (>8% o alteración del estado mental).",
            "PROTO-003|Rehidratación|Leve o moderada sin vómito → solución oral 50 ml/kg en 4 horas. Severa → vía intravenosa, bolo de 20 ml/kg."
    })
    void shouldKeepTheClinicalTextVerbatim(String protocolId, String section, String expectedContent) {
        assertEquals(Optional.of(expectedContent),
                CORPUS.find(new Citation(protocolId, section)).map(ProtocolSection::content));
    }

    @Test
    void shouldFailWithAClearErrorWhenTheFileDoesNotExist() {
        UncheckedIOException error = assertThrows(UncheckedIOException.class,
                () -> LOADER.load("protocols/does-not-exist.json"));

        assertEquals("Could not read the protocol corpus from classpath:protocols/does-not-exist.json",
                error.getMessage());
    }
}