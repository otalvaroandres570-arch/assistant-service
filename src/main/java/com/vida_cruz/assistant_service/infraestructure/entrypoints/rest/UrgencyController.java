package com.vida_cruz.assistant_service.infraestructure.entrypoints.rest;

import com.vida_cruz.assistant_service.domain.api.TrackUrgency;
import com.vida_cruz.assistant_service.infraestructure.entrypoints.dto.UrgencyStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/asistente")
@RequiredArgsConstructor
public class UrgencyController {

    private final TrackUrgency trackUrgency;

    @GetMapping("/estado-urgencias")
    public Mono<UrgencyStatusResponse> status() {
        return trackUrgency.current().map(UrgencyStatusResponse::from);
    }
}