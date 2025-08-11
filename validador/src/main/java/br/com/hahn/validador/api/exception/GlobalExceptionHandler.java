package br.com.hahn.validador.api.exception;



import br.com.hahn.validador.domain.exception.InvalidCpfException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidCpfException.class)
    public Mono<ResponseEntity<Map<String, String>>> handleInvalidCpfException(InvalidCpfException ex) {
        logger.error("CPF inválido: {}", ex.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "CPF inválido", "message", ex.getMessage())));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, String>>> handleValidationException(WebExchangeBindException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Erro de validação");

        logger.error("Erro de validação: {}", errorMessage);
        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Dados inválidos", "message", errorMessage)));
    }
}
