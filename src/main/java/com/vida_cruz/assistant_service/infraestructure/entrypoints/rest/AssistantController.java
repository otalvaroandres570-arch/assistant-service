package com.vida_cruz.assistant_service.infraestructure.entrypoints.rest;

import com.vida_cruz.assistant_service.domain.api.AskClinicalAssistant;
import com.vida_cruz.assistant_service.infraestructure.entrypoints.dto.ConsultationRequest;
import com.vida_cruz.assistant_service.infraestructure.entrypoints.dto.ConsultationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
@Slf4j
@RestController
@RequestMapping("/asistente")
@RequiredArgsConstructor
public class AssistantController {

    private final AskClinicalAssistant askClinicalAssistant;

    @PostMapping("/consulta")
    public Mono<ConsultationResponse> consult(@Valid @RequestBody ConsultationRequest request) {
        return askClinicalAssistant.ask(request.question())
                .doOnNext(answer -> log.info("Consultation resolved: {}", answer.getClass().getSimpleName()))
                .map(ConsultationResponse::from);
    }
}