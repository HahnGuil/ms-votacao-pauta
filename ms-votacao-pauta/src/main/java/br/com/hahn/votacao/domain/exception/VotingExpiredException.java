package br.com.hahn.votacao.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class VotingExpiredException extends RuntimeException {
    public VotingExpiredException(String message) {
        super(message);
    }
}
