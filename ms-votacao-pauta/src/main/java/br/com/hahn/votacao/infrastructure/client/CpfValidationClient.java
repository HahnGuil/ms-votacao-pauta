package br.com.hahn.votacao.infrastructure.client;

import br.com.hahn.votacao.domain.dto.response.CpfValidationResponseDTO;
import br.com.hahn.votacao.domain.exception.InvalidCpfException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class CpfValidationClient {

    private static final Logger logger = LoggerFactory.getLogger(CpfValidationClient.class);
    private final WebClient webClient;

    public CpfValidationClient(WebClient.Builder webClientBuilder,
                               @Value("${cpf.validation.service.url:http://localhost:8081}") String baseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<CpfValidationResponseDTO> validateCpf(String cpf) {
        logger.info("Chamando serviço de validação de CPF");

        return webClient
                .post()
                .uri("/api/v1/cpf/validate")
                .bodyValue(Map.of("cpf", cpf))
                .retrieve()
                .bodyToMono(CpfValidationResponseDTO.class)
                .doOnSuccess(response -> logger.info("CPF validado com sucesso: {}", response.status()))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                        logger.warn("CPF inválido retornado pelo serviço");
                        return Mono.error(new InvalidCpfException("CPF inválido"));
                    }
                    logger.error("Erro ao validar CPF: {}", ex.getMessage());
                    return Mono.error(new RuntimeException("Erro na validação do CPF", ex));
                });
    }
}
