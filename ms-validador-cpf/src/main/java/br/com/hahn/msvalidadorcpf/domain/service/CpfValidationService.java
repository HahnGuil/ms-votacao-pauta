package br.com.hahn.msvalidadorcpf.domain.service;

import br.com.hahn.msvalidadorcpf.domain.dto.response.CpfValidationResponseDTO;
import br.com.hahn.msvalidadorcpf.domain.enums.CPFStatus;
import br.com.hahn.msvalidadorcpf.domain.exception.InvalidCpfException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Random;

@Service
public class CpfValidationService {

    private static final Logger cpfValidationServiceLogger = LoggerFactory.getLogger(CpfValidationService.class);
    private final Random random = new Random();

    public Mono<CpfValidationResponseDTO> validateCpf(String cpf){
        cpfValidationServiceLogger.info("Validando CPF: {}", maskCpf(cpf));

        return Mono.fromCallable(() -> {
            if(!isValidCpfFormat(cpf)){
                cpfValidationServiceLogger.info("Cpf com formato inválido: {}", maskCpf(cpf));
                throw new InvalidCpfException("Invalid CPF format");
            }

//            Simulação de probabilidade de CPF inválido: 30%
            if(random.nextDouble() < 0.3){
                cpfValidationServiceLogger.info("Rejeitando aleatoriamente: {}", maskCpf(cpf));
                throw new InvalidCpfException("Invalid CPF");
            }

            CPFStatus status = random.nextBoolean() ? CPFStatus.ABLE_TO_VOTE : CPFStatus.UNABLE_TO_VOTE;

            cpfValidationServiceLogger.info("Cpf successfuly validate: {} - Status: {}", maskCpf(cpf), status);

            return new CpfValidationResponseDTO(status);
        });
    }

    private boolean isValidCpfFormat(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return false;
        }
        // Verifica se todos os dígitos são iguais (CPF inválido)
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }

        return cpf.matches("\\d{11}");
    }

    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return "***.***.***-**";
        }
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
    }
}
