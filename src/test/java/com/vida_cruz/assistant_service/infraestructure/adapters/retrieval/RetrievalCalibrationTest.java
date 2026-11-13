package com.vida_cruz.assistant_service.infraestructure.adapters.retrieval;

import com.vida_cruz.assistant_service.domain.model.RetrievedSection;
import com.vida_cruz.assistant_service.domain.spi.ProtocolRetriever;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "assistant.rag.top-k=7")
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class RetrievalCalibrationTest {

    private static final List<String> QUESTIONS = List.of(
            "¿Cuáles son las medidas iniciales ante una crisis hipertensiva?",
            "¿Cuándo se administra el primer antibiótico en sepsis?",
            "¿Cuál es el tratamiento de primera línea para la diabetes tipo 2?",
            "¿Cuál es la capital de Francia?",
            "¿Cómo se trata una fractura de fémur?",
            "¿Qué líquidos se le dan a un niño con diarrea y deshidratación severa?",
            "¿Qué hago si la presión está muy alta y el paciente tiene dolor de cabeza?");

    @Autowired
    private ProtocolRetriever retriever;

    @Test
    void shouldPrintTheSimilarityOfEachQuestionWithEverySection() {
        QUESTIONS.forEach(question -> {
            List<RetrievedSection> matches = retriever.retrieve(question).block();

            Assertions.assertNotNull(matches);
            matches.forEach(match -> System.out.printf("  %.3f  %s / %s%n",
                    match.score(), match.section().protocolId(), match.section().name()));
            assertEquals(7, matches.size());
        });
    }
}