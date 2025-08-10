package br.com.hahn.votacao.api.exception;

import br.com.hahn.votacao.domain.dto.response.ErrorResponseDTO;
import br.com.hahn.votacao.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void testHandlerUserAlreadyExists() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("Usuário já existe");
        Mono<ResponseEntity<ErrorResponseDTO>> mono = handler.handlerUserAlreadyExists(ex);

        StepVerifier.create(mono)
            .assertNext(response -> {
                assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("Usuário já existe", response.getBody().message());
                assertNotNull(response.getBody().timestamp());
            })
            .verifyComplete();
    }

    @Test
    void testHandlerUserAlreadyVote() {
        UserAlreadyVoteException ex = new UserAlreadyVoteException("Usuário já votou");
        Mono<ResponseEntity<ErrorResponseDTO>> mono = handler.handlerUserAlreadyVote(ex);

        StepVerifier.create(mono)
            .assertNext(response -> {
                assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("Usuário já votou", response.getBody().message());
                assertNotNull(response.getBody().timestamp());
            })
            .verifyComplete();
    }

    @Test
    void testHandleInvalidFormatExpirationDate() {
        InvalidFormatExpirationDate ex = new InvalidFormatExpirationDate("Formato inválido");
        Mono<ResponseEntity<ErrorResponseDTO>> mono = handler.handleInvalidFormatExpirationDate(ex);

        StepVerifier.create(mono)
            .assertNext(response -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("Formato inválido", response.getBody().message());
                assertNotNull(response.getBody().timestamp());
            })
            .verifyComplete();
    }

    @Test
    void testHandleVotingNotFoundException() {
        VotingNotFoundException ex = new VotingNotFoundException("Votação não encontrada");
        Mono<ResponseEntity<ErrorResponseDTO>> mono = handler.handleVotingNotFoundException(ex);

        StepVerifier.create(mono)
            .assertNext(response -> {
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("Votação não encontrada", response.getBody().message());
                assertNotNull(response.getBody().timestamp());
            })
            .verifyComplete();
    }

    @Test
    void testHandleVotingExpiredException() {
        VotingExpiredException ex = new VotingExpiredException("Votação expirada");
        Mono<ResponseEntity<ErrorResponseDTO>> mono = handler.handleVotingExpiredException(ex);

        StepVerifier.create(mono)
            .assertNext(response -> {
                assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("Votação expirada", response.getBody().message());
                assertNotNull(response.getBody().timestamp());
            })
            .verifyComplete();
    }

    @Test
    void testHandleResultNotFoundException() {
        ResultNotFoundException ex = new ResultNotFoundException("Resultado não encontrado");
        Mono<ResponseEntity<ErrorResponseDTO>> mono = handler.handleResultNotFoundException(ex);

        StepVerifier.create(mono)
            .assertNext(response -> {
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("Resultado não encontrado", response.getBody().message());
                assertNotNull(response.getBody().timestamp());
            })
            .verifyComplete();
    }

    @Test
    void testHandleResultNotReadyException() {
        ResultNotReadyException ex = new ResultNotReadyException("Resultado não pronto");
        Mono<ResponseEntity<ErrorResponseDTO>> mono = handler.handleResultNotReadyException(ex);

        StepVerifier.create(mono)
            .assertNext(response -> {
                assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("Resultado não pronto", response.getBody().message());
                assertNotNull(response.getBody().timestamp());
            })
            .verifyComplete();
    }

    @Test
    void testHandleGenericRuntimeException() {
        RuntimeException ex = new RuntimeException("Erro genérico");
        Mono<ResponseEntity<ErrorResponseDTO>> mono = handler.handleGenericRuntimeException(ex);

        StepVerifier.create(mono)
            .assertNext(response -> {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                assertNotNull(response.getBody());
                assertTrue(response.getBody().message().contains("Internal server error: Erro genérico"));
                assertNotNull(response.getBody().timestamp());
            })
            .verifyComplete();
    }
}

