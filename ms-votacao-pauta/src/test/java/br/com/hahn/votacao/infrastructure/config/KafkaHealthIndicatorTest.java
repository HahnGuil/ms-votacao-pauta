package br.com.hahn.votacao.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class KafkaHealthIndicatorTest {

    @Test
    void health_shouldReturnUpWhenKafkaIsAvailable() {
        KafkaHealthIndicator indicator = new KafkaHealthIndicator();
        Mono<Health> healthMono = indicator.health();

        StepVerifier.create(healthMono)
                .assertNext(health -> {
                    assertEquals(Status.UP, health.getStatus());
                    assertEquals("Available", health.getDetails().get("kafka"));
                })
                .verifyComplete();
    }

    @Test
    void buildHealthFromStatus_shouldReturnUpWhenTrue() throws Exception {
        KafkaHealthIndicator indicator = new KafkaHealthIndicator();
        Method method = KafkaHealthIndicator.class.getDeclaredMethod("buildHealthFromStatus", Boolean.class);
        method.setAccessible(true);

        Health health = (Health) method.invoke(indicator, true);
        assertEquals(Status.UP, health.getStatus());
        assertEquals("Available", health.getDetails().get("kafka"));
    }

    @Test
    void buildHealthFromStatus_shouldReturnDownWhenFalse() throws Exception {
        KafkaHealthIndicator indicator = new KafkaHealthIndicator();
        Method method = KafkaHealthIndicator.class.getDeclaredMethod("buildHealthFromStatus", Boolean.class);
        method.setAccessible(true);

        Health health = (Health) method.invoke(indicator, false);
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Unavailable", health.getDetails().get("kafka"));
    }

    @Test
    void health_shouldReturnDownOnError() {
        KafkaHealthIndicator indicator = new KafkaHealthIndicator() {
            @Override
            public Mono<Boolean> checkKafkaHealth() {
                return Mono.error(new RuntimeException("Kafka error"));
            }
        };

        Mono<Health> healthMono = indicator.health();

        StepVerifier.create(healthMono)
                .assertNext(health -> {
                    assertEquals(Status.DOWN, health.getStatus());
                    assertEquals("Error checking health", health.getDetails().get("kafka"));
                })
                .verifyComplete();
    }

    @Test
    void checkKafkaHealth_shouldReturnTrue() {
        KafkaHealthIndicator indicator = new KafkaHealthIndicator();
        Mono<Boolean> result = indicator.checkKafkaHealth();

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }
}