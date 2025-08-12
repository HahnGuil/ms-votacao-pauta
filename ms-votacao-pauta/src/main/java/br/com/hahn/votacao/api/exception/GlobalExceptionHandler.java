package br.com.hahn.votacao.api.exception;

import br.com.hahn.votacao.domain.dto.response.ErrorResponseDTO;
import br.com.hahn.votacao.domain.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Manipulador global de exceções para a API de votação.
 *
 * Esta classe centraliza o tratamento de todas as exceções específicas do domínio
 * e genéricas, convertendo-as em respostas HTTP apropriadas com códigos de status
 * corretos e mensagens de erro padronizadas.
 *
 * @author HahnGuil
 * @since 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata exceções quando um usuário já existe no sistema.
     *
     * @param ex exceção contendo detalhes do usuário duplicado
     * @return Mono com ResponseEntity contendo erro HTTP 409 (Conflict)
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerUserAlreadyExists(UserAlreadyExistsException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
    }

    /**
     * Trata exceções quando um usuário tenta votar mais de uma vez na mesma votação.
     *
     * @param ex exceção contendo detalhes da tentativa de voto duplicado
     * @return Mono com ResponseEntity contendo erro HTTP 403 (Forbidden)
     */
    @ExceptionHandler(UserAlreadyVoteException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerUserAlreadyVote(UserAlreadyVoteException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(error));
    }

    /**
     * Trata exceções de formato inválido na data de expiração.
     *
     * @param ex exceção contendo detalhes do formato inválido
     * @return Mono com ResponseEntity contendo erro HTTP 400 (Bad Request)
     */
    @ExceptionHandler(InvalidFormatExpirationDate.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleInvalidFormatExpirationDate(InvalidFormatExpirationDate ex){
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    /**
     * Trata exceções quando uma votação não é encontrada.
     *
     * @param ex exceção contendo detalhes da votação não encontrada
     * @return Mono com ResponseEntity contendo erro HTTP 404 (Not Found)
     */
    @ExceptionHandler(VotingNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleVotingNotFoundException(VotingNotFoundException ex){
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    /**
     * Trata exceções de tentativa de voto em votação já expirada.
     *
     * @param ex exceção contendo detalhes da votação expirada
     * @return Mono com ResponseEntity contendo erro HTTP 403 (Forbidden)
     */
    @ExceptionHandler(VotingExpiredException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleVotingExpiredException(VotingExpiredException ex){
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(error));
    }

    /**
     * Trata exceções quando um resultado de votação não é encontrado.
     *
     * @param ex exceção contendo detalhes do resultado não encontrado
     * @return Mono com ResponseEntity contendo erro HTTP 404 (Not Found)
     */
    @ExceptionHandler(ResultNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleResultNotFoundException(ResultNotFoundException ex){
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    /**
     * Trata exceções de CPF em formato inválido.
     *
     * @param ex exceção contendo detalhes do CPF inválido
     * @return Mono com ResponseEntity contendo erro HTTP 400 (Bad Request)
     */
    @ExceptionHandler(InvalidCpfException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleInvalidCpfException(InvalidCpfException ex){
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    /**
     * Trata exceções quando um usuário não é encontrado.
     *
     * @param ex exceção contendo detalhes do usuário não encontrado
     * @return Mono com ResponseEntity contendo erro HTTP 404 (Not Found)
     */
    @ExceptionHandler(UserNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleUserNotFoundException(UserNotFoundException ex){
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    /**
     * Trata exceções quando o resultado da votação ainda não está pronto.
     * Indica que o processamento está em andamento.
     *
     * @param ex exceção contendo detalhes do resultado não processado
     * @return Mono com ResponseEntity contendo erro HTTP 202 (Accepted)
     */
    @ExceptionHandler(ResultNotReadyException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleResultNotReadyException(ResultNotReadyException ex){
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.ACCEPTED).body(error));
    }

    /**
     * Manipulador genérico para exceções de runtime não tratadas especificamente.
     * Evita que erros internos sejam expostos ao cliente.
     *
     * @param ex exceção de runtime não tratada
     * @return Mono com ResponseEntity contendo erro HTTP 500 (Internal Server Error)
     */
    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleGenericRuntimeException(RuntimeException ex){
        ErrorResponseDTO error = new ErrorResponseDTO("Internal server error: " + ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }


}
