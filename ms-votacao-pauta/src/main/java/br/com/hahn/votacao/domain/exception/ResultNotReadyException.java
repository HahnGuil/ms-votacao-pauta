package br.com.hahn.votacao.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Indica que o resultado da votação ainda não está disponível.
 *
 * Lançada quando se tenta consultar resultado de votação que ainda
 * está em andamento ou cujo processamento não foi finalizado.
 */
@ResponseStatus(HttpStatus.ACCEPTED)
public class ResultNotReadyException extends RuntimeException {
    public ResultNotReadyException(String message) {
        super(message);
    }
}
