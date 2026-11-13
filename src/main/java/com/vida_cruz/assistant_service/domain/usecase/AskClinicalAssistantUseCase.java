package com.vida_cruz.assistant_service.domain.usecase;

import com.vida_cruz.assistant_service.domain.api.AskClinicalAssistant;
import com.vida_cruz.assistant_service.domain.exceptions.AssistantUnavailableException;
import com.vida_cruz.assistant_service.domain.model.ClinicalAnswer;
import com.vida_cruz.assistant_service.domain.model.Citation;
import com.vida_cruz.assistant_service.domain.model.GeneratedAnswer;
import com.vida_cruz.assistant_service.domain.model.ProtocolCorpus;
import com.vida_cruz.assistant_service.domain.model.ProtocolSection;
import com.vida_cruz.assistant_service.domain.model.RetrievedSection;
import com.vida_cruz.assistant_service.domain.spi.ClinicalAnswerGenerator;
import com.vida_cruz.assistant_service.domain.spi.ProtocolRetriever;
import com.vida_cruz.assistant_service.domain.util.Texts;
import org.jspecify.annotations.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AskClinicalAssistantUseCase implements AskClinicalAssistant {

    private static final ClinicalAnswer NO_PROTOCOL = new ClinicalAnswer.NoProtocolAvailable();

    private final ProtocolRetriever retriever;
    private final ClinicalAnswerGenerator generator;
    private final ProtocolCorpus corpus;

    public AskClinicalAssistantUseCase(ProtocolRetriever retriever,
                                       ClinicalAnswerGenerator generator,
                                       ProtocolCorpus corpus) {
        this.retriever = Objects.requireNonNull(retriever);
        this.generator = Objects.requireNonNull(generator);
        this.corpus = Objects.requireNonNull(corpus);
    }

    @Override
    public Mono<ClinicalAnswer> ask(String question) {
        return Mono.fromSupplier(() -> Texts.requireNotBlank(question, "question"))
                .flatMap(this::answer)
                .onErrorMap(error -> !(error instanceof IllegalArgumentException), AssistantUnavailableException::new);
    }

    private Mono<ClinicalAnswer> answer(String question) {
        return retriever.retrieve(question)
                .flatMap(retrieved -> retrieved.isEmpty()
                        ? Mono.just(NO_PROTOCOL)
                        : generator.generate(question, sectionsOf(retrieved))
                        .map(draft -> validate(draft, retrieved)));
    }

    /** An answer is only valid if it is grounded, and every source it cites was provided and exists in the corpus. */
    private ClinicalAnswer validate(GeneratedAnswer draft, List<RetrievedSection> retrieved) {
        Set<Citation> provided = getCitationSet(retrieved);

        return Optional.of(draft)
                .filter(GeneratedAnswer::answerable)
                .filter(generated -> !generated.answer().isBlank())
                .filter(generated -> !generated.citations().isEmpty())
                .filter(generated -> provided.containsAll(generated.citations()))
                .filter(this::isValidCorpus)
                .<ClinicalAnswer>map(AskClinicalAssistantUseCase::createAnswered)
                .orElse(NO_PROTOCOL);
    }

    private boolean isValidCorpus(GeneratedAnswer generated) {
        return generated.citations().stream().allMatch(corpus::contains);
    }

    private static ClinicalAnswer.@NonNull Answered createAnswered(GeneratedAnswer generated) {
        return new ClinicalAnswer.Answered(
                generated.answer(), generated.citations().stream().distinct().toList());
    }

    private static @NonNull Set<Citation> getCitationSet(List<RetrievedSection> retrieved) {
        return retrieved.stream()
                .map(match -> match.section().citation())
                .collect(Collectors.toSet());
    }

    private static List<ProtocolSection> sectionsOf(List<RetrievedSection> retrieved) {
        return retrieved.stream().map(RetrievedSection::section).toList();
    }
}