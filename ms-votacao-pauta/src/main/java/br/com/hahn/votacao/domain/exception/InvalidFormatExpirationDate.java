package br.com.hahn.votacao.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Indica formato inválido para data de expiração de votação.
 *
 * Lançada quando a data fornecida não atende ao padrão esperado
 * ou contém valores inválidos para configuração da votação.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidFormatExpirationDate extends RuntimeException {
    public InvalidFormatExpirationDate(String message) {
        super(message);
    }
}
