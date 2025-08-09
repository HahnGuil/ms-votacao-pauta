package br.com.hahn.votacao.api.exception;

import br.com.hahn.votacao.domain.exception.UserAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public Mono<ResponseEntity<String>> handlerUserAlreadyExists(UserAlreadyExistsException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()));
    }
}
