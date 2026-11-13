package com.vida_cruz.assistant_service.domain.usecase;

import com.vida_cruz.assistant_service.domain.exceptions.AssistantUnavailableException;
import com.vida_cruz.assistant_service.domain.model.Citation;
import com.vida_cruz.assistant_service.domain.model.ClinicalAnswer;
import com.vida_cruz.assistant_service.domain.model.GeneratedAnswer;
import com.vida_cruz.assistant_service.domain.model.ProtocolCorpus;
import com.vida_cruz.assistant_service.domain.model.ProtocolSection;
import com.vida_cruz.assistant_service.domain.model.RetrievedSection;
import com.vida_cruz.assistant_service.domain.spi.ClinicalAnswerGenerator;
import com.vida_cruz.assistant_service.domain.spi.ProtocolRetriever;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AskClinicalAssistantUseCaseTest {

    private static final ProtocolSection MEASURES = new ProtocolSection(
            "PROTO-001", "Crisis Hipertensiva", "Medidas iniciales", "Reconfirmar la presión tras 5 minutos de reposo.");
    private static final ProtocolSection CRITERIA = new ProtocolSection(
            "PROTO-002", "Sepsis", "Criterios", "2 o más de: frecuencia respiratoria ≥ 22.");
    private static final ProtocolCorpus CORPUS = new ProtocolCorpus(List.of(MEASURES, CRITERIA));
    private static final ProtocolRetriever FINDS_MEASURES = question -> Mono.just(List.of(new RetrievedSection(MEASURES, 0.86)));
    private static final ClinicalAnswer NO_PROTOCOL = new ClinicalAnswer.NoProtocolAvailable();

    @Test
    void shouldAnswerWithTheCitedSourcesAndGiveTheModelOnlyTheRetrievedContext() {
        AtomicReference<List<ProtocolSection>> receivedContext = new AtomicReference<>();
        AskClinicalAssistantUseCase useCase = useCase(FINDS_MEASURES, (question, context) -> {
            receivedContext.set(context);
            return Mono.just(new GeneratedAnswer(true, "Reconfirmar la presión tras 5 minutos.", List.of(MEASURES.citation())));
        });

        StepVerifier.create(useCase.ask("¿Medidas iniciales?"))
                .expectNext(new ClinicalAnswer.Answered("Reconfirmar la presión tras 5 minutos.", List.of(MEASURES.citation())))
                .verifyComplete();
        assertEquals(List.of(MEASURES), receivedContext.get());
    }

    @Test
    void shouldAnswerWithTheNoticeWithoutConsultingTheModelWhenNothingIsRelevant() {
        AtomicInteger modelCalls = new AtomicInteger();
        AskClinicalAssistantUseCase useCase = useCase(question -> Mono.just(List.of()), (question, context) -> {
            modelCalls.incrementAndGet();
            return Mono.just(new GeneratedAnswer(true, "inventado", List.of(MEASURES.citation())));
        });

        StepVerifier.create(useCase.ask("¿Tratamiento de la diabetes?"))
                .expectNext(NO_PROTOCOL)
                .verifyComplete();
        assertEquals(0, modelCalls.get());
    }

    @ParameterizedTest(name = "ungrounded draft #{index} is replaced by the notice")
    @MethodSource("ungroundedDrafts")
    void shouldNeverReturnAnAnswerThatIsNotGroundedInTheCorpus(GeneratedAnswer draft) {
        AskClinicalAssistantUseCase useCase = useCase(FINDS_MEASURES, (question, context) -> Mono.just(draft));

        StepVerifier.create(useCase.ask("¿Medidas iniciales?"))
                .expectNext(NO_PROTOCOL)
                .verifyComplete();
    }

    static Stream<GeneratedAnswer> ungroundedDrafts() {
        Citation unknown = new Citation("PROTO-999", "Dosis");
        return Stream.of(
                new GeneratedAnswer(false, "", List.of()),                                        // the model says it cannot answer
                new GeneratedAnswer(true, "Algo clínico.", List.of()),                            // no sources
                new GeneratedAnswer(true, "Algo clínico.", List.of(unknown)),                     // source not in the corpus
                new GeneratedAnswer(true, "Algo clínico.", List.of(MEASURES.citation(), unknown)), // one valid, one invented
                new GeneratedAnswer(true, "Algo clínico.", List.of(CRITERIA.citation())),         // in the corpus, but not provided
                new GeneratedAnswer(true, "   ", List.of(MEASURES.citation())));                 // no text
    }

    @Test
    void shouldReportTheAssistantAsUnavailableWhenRetrievalOrGenerationFail() {
        AskClinicalAssistantUseCase failingRetrieval = useCase(
                question -> Mono.error(new IllegalStateException("embeddings down")),
                (question, context) -> Mono.just(new GeneratedAnswer(true, "x", List.of(MEASURES.citation()))));
        AskClinicalAssistantUseCase failingModel = useCase(
                FINDS_MEASURES, (question, context) -> Mono.error(new IllegalStateException("llm down")));

        StepVerifier.create(failingRetrieval.ask("¿Medidas?"))
                .expectErrorSatisfies(error -> assertInstanceOf(AssistantUnavailableException.class, error))
                .verify();
        StepVerifier.create(failingModel.ask("¿Medidas?"))
                .expectErrorSatisfies(error -> assertInstanceOf(AssistantUnavailableException.class, error))
                .verify();
    }

    private static AskClinicalAssistantUseCase useCase(ProtocolRetriever retriever, ClinicalAnswerGenerator generator) {
        return new AskClinicalAssistantUseCase(retriever, generator, CORPUS);
    }
}