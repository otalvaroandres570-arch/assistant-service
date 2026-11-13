package com.vida_cruz.assistant_service.infraestructure.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class GeminiConnectivitySmokeTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private ChatModel chatModel;

    @Test
    void shouldEmbedSpanishText() {
        float[] vector = embeddingModel.embed("Medidas iniciales ante una crisis hipertensiva");

        System.out.println("Embedding dimensions: " + vector.length);
        assertTrue(vector.length > 0);
    }

    @Test
    void shouldAnswerAPrompt() {
        ChatResponse response = chatModel.call(new Prompt("Responde únicamente con la palabra OK"));
        String answer = response.getResult().getOutput().getText();

        System.out.println("Model used: " + response.getMetadata().getModel());
        System.out.println("Answer: " + answer);
        assertFalse(answer.isBlank());
    }
}