package br.com.hahn.validador.infraestracture;



import br.com.hahn.validador.domain.service.CpfValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component("cpfValidation")
public class CpfValidationHealthIndicator implements ReactiveHealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(CpfValidationHealthIndicator.class);
    private final CpfValidationService cpfValidationService;

    public CpfValidationHealthIndicator(CpfValidationService cpfValidationService) {
        this.cpfValidationService = cpfValidationService;
    }

    @Override
    public Mono<Health> health() {
        return checkCpfValidationService()
                .map(isHealthy -> isHealthy
                        ? Health.up()
                        .withDetail("status", "CPF validation service is operational")
                        .withDetail("service", "ms-valida-cpf")
                        .withDetail("timestamp", System.currentTimeMillis())
                        .build()
                        : Health.down()
                        .withDetail("status", "CPF validation service is not responding correctly")
                        .withDetail("service", "ms-valida-cpf")
                        .withDetail("timestamp", System.currentTimeMillis())
                        .build())
                .onErrorResume(ex -> {
                    logger.error("Health check failed", ex);
                    return Mono.just(Health.down()
                            .withDetail("error", ex.getMessage())
                            .withDetail("status", "CPF validation service health check failed")
                            .withDetail("service", "ms-valida-cpf")
                            .withDetail("timestamp", System.currentTimeMillis())
                            .build());
                });
    }

    private Mono<Boolean> checkCpfValidationService() {
        // Testa o serviço com um CPF de teste
        String testCpf = "12345678901"; // CPF de teste

        return cpfValidationService.validateCpf(testCpf)
                .map(response -> true)
                .onErrorReturn(false)
                .timeout(Duration.ofSeconds(5))
                .doOnNext(result -> logger.debug("Health check result: {}", result))
                .onErrorReturn(false);
    }
}
