package br.com.hahn.votacao.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidFormatExpirationDate extends RuntimeException {
    public InvalidFormatExpirationDate(String message) {
        super(message);
    }
}
