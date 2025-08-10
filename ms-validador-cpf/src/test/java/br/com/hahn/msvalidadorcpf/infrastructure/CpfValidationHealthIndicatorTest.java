package br.com.hahn.msvalidadorcpf.infrastructure;


import br.com.hahn.msvalidadorcpf.domain.enums.CPFStatus;
import br.com.hahn.msvalidadorcpf.domain.service.CpfValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

class CpfValidationHealthIndicatorTest {

    private CpfValidationService cpfValidationService;
    private CpfValidationHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        cpfValidationService = Mockito.mock(CpfValidationService.class);
        healthIndicator = new CpfValidationHealthIndicator(cpfValidationService);
    }

    @Test
    void healthShouldReturnUpWhenServiceIsHealthy() {
        Mockito.when(cpfValidationService.validateCpf(anyString()))
                .thenReturn(Mono.just(new br.com.hahn.msvalidadorcpf.domain.dto.response.CpfValidationResponseDTO(CPFStatus.ABLE_TO_VOTE)));

        Mono<Health> healthMono = healthIndicator.health();

        StepVerifier.create(healthMono)
                .assertNext(health -> {
                    assertThat(health.getStatus()).isEqualTo(Status.UP);
                    assertThat(health.getDetails()).containsEntry("status", "CPF validation service is operational");
                    assertThat(health.getDetails()).containsEntry("service", "ms-valida-cpf");
                    assertThat(health.getDetails().get("timestamp")).isInstanceOf(Long.class);
                })
                .verifyComplete();
    }

    @Test
    void healthShouldReturnDownWhenServiceIsNotHealthy() {
        Mockito.when(cpfValidationService.validateCpf(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        Mono<Health> healthMono = healthIndicator.health();

        StepVerifier.create(healthMono)
                .assertNext(health -> {
                    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
                    assertThat(health.getDetails()).containsEntry("status", "CPF validation service is not responding correctly");
                    assertThat(health.getDetails()).containsEntry("service", "ms-valida-cpf");
                    assertThat(health.getDetails().get("timestamp")).isInstanceOf(Long.class);
                })
                .verifyComplete();
    }

    @Test
    void healthShouldReturnDownWhenServiceReturnsFalse() {
        Mockito.when(cpfValidationService.validateCpf(anyString()))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid CPF")));

        Mono<Health> healthMono = healthIndicator.health();

        StepVerifier.create(healthMono)
                .assertNext(health -> {
                    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
                    assertThat(health.getDetails()).containsEntry("status", "CPF validation service is not responding correctly");
                    assertThat(health.getDetails()).containsEntry("service", "ms-valida-cpf");
                    assertThat(health.getDetails().get("timestamp")).isInstanceOf(Long.class);
                })
                .verifyComplete();
    }

    @Test
    void healthShouldReturnDownOnTimeout() {
        Mockito.when(cpfValidationService.validateCpf(anyString()))
                .thenReturn(Mono.never());

        Mono<Health> healthMono = healthIndicator.health();

        StepVerifier.create(healthMono)
                .assertNext(health -> {
                    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
                    assertThat(health.getDetails()).containsEntry("status", "CPF validation service is not responding correctly");
                    assertThat(health.getDetails()).containsEntry("service", "ms-valida-cpf");
                    assertThat(health.getDetails().get("timestamp")).isInstanceOf(Long.class);
                })
                .verifyComplete();
    }
}

