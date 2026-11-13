package com.vida_cruz.assistant_service;

import com.vida_cruz.assistant_service.domain.api.AskClinicalAssistant;
import com.vida_cruz.assistant_service.domain.model.ClinicalAnswer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Talks to the real Gemini API, whose latency sometimes approaches the production 20s timeout.
// This test checks answer content, not the timeout (ProviderGuardTest covers that), so it allows more time.
@SpringBootTest(properties = "assistant.resilience.timeout=60s")
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class ClinicalAssistantAcceptanceTest {

    @Autowired
    private AskClinicalAssistant assistant;

    @Test
    void shouldAnswerTheHypertensiveCrisisQuestionFromProto001() {
        assertCitesProtocol("¿Cuáles son las medidas iniciales ante una crisis hipertensiva?", "PROTO-001");
    }

    @Test
    void shouldAnswerTheSepsisAntibioticQuestionFromProto002() {
        assertCitesProtocol("¿Cuándo se administra el primer antibiótico en sepsis?", "PROTO-002");
    }

    @Test
    void shouldNotGenerateClinicalContentWhenThereIsNoProtocol() {
        ClinicalAnswer answer = assistant
                .ask("¿Cuál es el tratamiento de primera línea para la diabetes tipo 2?").block();

        assertEquals(new ClinicalAnswer.NoProtocolAvailable(), answer);
    }

    private void assertCitesProtocol(String question, String protocolId) {
        ClinicalAnswer.Answered answered = assertInstanceOf(ClinicalAnswer.Answered.class, assistant.ask(question).block());

        System.out.println("\n" + question + "\n  -> " + answered.answer() + "\n  sources: " + answered.sources());
        assertTrue(answered.sources().stream().anyMatch(source -> source.protocol().equals(protocolId)));
    }
}