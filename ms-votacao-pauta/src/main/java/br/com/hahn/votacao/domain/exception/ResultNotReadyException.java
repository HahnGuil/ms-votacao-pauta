package br.com.hahn.votacao.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.ACCEPTED)
public class ResultNotReadyException extends RuntimeException {
    public ResultNotReadyException(String message) {
        super(message);
    }
}
