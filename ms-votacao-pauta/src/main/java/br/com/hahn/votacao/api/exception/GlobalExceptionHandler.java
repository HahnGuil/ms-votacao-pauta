package br.com.hahn.votacao.api.exception;

import br.com.hahn.votacao.domain.dto.response.ErrorResponseDTO;
import br.com.hahn.votacao.domain.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerUserAlreadyExists(UserAlreadyExistsException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
    }

    @ExceptionHandler(UserAlreadyVoteException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handlerUserAlreadyVote(UserAlreadyVoteException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(error));
    }

    @ExceptionHandler(InvalidFormatExpirationDate.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleInvalidFormatExpirationDate(InvalidFormatExpirationDate ex){
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    @ExceptionHandler(VotingNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleVotingNotFoundException(VotingNotFoundException ex){
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    @ExceptionHandler(VotingExpiredException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleVotingExpiredException(VotingExpiredException ex){
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(error));
    }

    @ExceptionHandler(ResultNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleResultNotFoundException(ResultNotFoundException ex){
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    @ExceptionHandler(ResultNotReadyException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleResultNotReadyException(ResultNotReadyException ex){
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.ACCEPTED).body(error));
    }

    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<ErrorResponseDTO>> handleGenericRuntimeException(RuntimeException ex){
        ErrorResponseDTO error = new ErrorResponseDTO("Internal server error: " + ex.getMessage(), Instant.now());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }
}
