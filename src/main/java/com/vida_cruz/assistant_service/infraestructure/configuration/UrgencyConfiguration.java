package com.vida_cruz.assistant_service.infraestructure.configuration;

import com.vida_cruz.assistant_service.domain.api.TrackUrgency;
import com.vida_cruz.assistant_service.domain.spi.UrgencyBoard;
import com.vida_cruz.assistant_service.domain.usecase.TrackUrgencyUseCase;
import com.vida_cruz.assistant_service.infraestructure.adapters.urgency.RedisUrgencyBoard;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@Configuration
@EnableConfigurationProperties(UrgencyProperties.class)
public class UrgencyConfiguration {

    @Bean
    public UrgencyBoard urgencyBoard(ReactiveStringRedisTemplate redis, UrgencyProperties properties) {
        return new RedisUrgencyBoard(redis, properties.dischargedPatientTtl());
    }

    @Bean
    public TrackUrgency trackUrgency(UrgencyBoard board) {
        return new TrackUrgencyUseCase(board);
    }
}