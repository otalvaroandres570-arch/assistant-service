package com.vida_cruz.assistant_service.infraestructure.adapters.urgency;

import com.vida_cruz.assistant_service.domain.model.UrgencyEvent;
import com.vida_cruz.assistant_service.domain.model.UrgencySummary;
import com.vida_cruz.assistant_service.domain.model.enums.TriageLevel;
import com.vida_cruz.assistant_service.domain.spi.UrgencyBoard;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class RedisUrgencyBoard implements UrgencyBoard {

    static final String ACTIVE_BY_LEVEL_KEY = "urgency:active-by-level";

    private static final RedisScript<Long> APPLY_SCRIPT =
            RedisScript.of(new ClassPathResource("redis/urgency-apply.lua"), Long.class);

    private final ReactiveStringRedisTemplate redis;
    private final Duration dischargedPatientTtl;

    public RedisUrgencyBoard(ReactiveStringRedisTemplate redis, Duration dischargedPatientTtl) {
        this.redis = Objects.requireNonNull(redis);
        this.dischargedPatientTtl = Objects.requireNonNull(dischargedPatientTtl);
    }

    @Override
    public Mono<Void> apply(UrgencyEvent event) {
        return redis.execute(APPLY_SCRIPT,
                        List.of(ACTIVE_BY_LEVEL_KEY, patientKey(event.patientId())),
                        List.of(event.level().name(), event.status().name(),
                                String.valueOf(dischargedPatientTtl.toSeconds())))
                .then();
    }

    @Override
    public Mono<UrgencySummary> summary() {
        return redis.<String, String>opsForHash().entries(ACTIVE_BY_LEVEL_KEY)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(RedisUrgencyBoard::toSummary);
    }

    static String patientKey(UUID patientId) {
        return "urgency:patient:" + patientId;
    }

    /** Fields that are not a known level are ignored instead of failing the whole query. */
    private static UrgencySummary toSummary(Map<String, String> raw) {
        return new UrgencySummary(raw.entrySet().stream()
                .flatMap(entry -> TriageLevel.parse(entry.getKey())
                        .map(level -> Map.entry(level, Long.parseLong(entry.getValue())))
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
}