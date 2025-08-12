package br.com.hahn.votacao.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Indica tentativa de voto duplicado pelo mesmo usuário.
 *
 * Lançada quando um CPF tenta votar mais de uma vez na mesma
 * votação, violando a regra de unicidade do voto.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UserAlreadyVoteException extends RuntimeException {
    public UserAlreadyVoteException(String message) {
        super(message);
    }
}
