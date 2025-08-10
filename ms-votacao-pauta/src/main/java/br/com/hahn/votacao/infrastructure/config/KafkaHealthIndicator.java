package br.com.hahn.votacao.infrastructure.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class KafkaHealthIndicator implements ReactiveHealthIndicator {

    private static final String KAFKA_DETAIL_KEY = "kafka";
    private static final String KAFKA_AVAILABLE = "Available";
    private static final String KAFKA_UNAVAILABLE = "Unavailable";
    private static final String KAFKA_ERROR = "Error checking health";

    @Override
    public Mono<Health> health() {
        return checkKafkaHealth()
                .map(this::buildHealthFromStatus)
                .onErrorReturn(Health.down()
                        .withDetail(KAFKA_DETAIL_KEY, KAFKA_ERROR)
                        .build());
    }

    public Health buildHealthFromStatus(Boolean isHealthy) {
        if (Boolean.TRUE.equals(isHealthy)) {
            return Health.up()
                    .withDetail(KAFKA_DETAIL_KEY, KAFKA_AVAILABLE)
                    .build();
        } else {
            return Health.down()
                    .withDetail(KAFKA_DETAIL_KEY, KAFKA_UNAVAILABLE)
                    .build();
        }
    }

    public Mono<Boolean> checkKafkaHealth() {
        return Mono.just(Boolean.TRUE);
    }
}
