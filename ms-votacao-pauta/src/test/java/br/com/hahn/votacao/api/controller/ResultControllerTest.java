package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.domain.dto.response.ResultResponseDTO;
import br.com.hahn.votacao.domain.service.ResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResultControllerTest {

    private ResultService resultService;
    private ResultController resultController;

    @BeforeEach
    void setUp() {
        resultService = mock(ResultService.class);
        resultController = new ResultController(resultService);
    }

    @Test
    void constructor_ShouldInitializeResultService() {
        assertNotNull(resultController);
    }

    @Test
    void getResult_WithValidVotingId_ShouldReturnResultResponseDTO() {
        String votingId = "valid-voting-123";
        ResultResponseDTO dto = new ResultResponseDTO("valid-voting-123", "Should we approve budget?", 10, "APROVADO");
        when(resultService.getResult(votingId)).thenReturn(Mono.just(dto));

        Mono<ResultResponseDTO> resultMono = resultController.getResult(votingId);

        StepVerifier.create(resultMono)
                .expectNext(dto)
                .verifyComplete();

        verify(resultService, times(1)).getResult(votingId);
    }

    @Test
    void getResult_WithNumericVotingId_ShouldReturnResultResponseDTO() {
        String votingId = "999";
        ResultResponseDTO dto = new ResultResponseDTO("999", "New policy implementation", 25, "REPROVADO");
        when(resultService.getResult(votingId)).thenReturn(Mono.just(dto));

        Mono<ResultResponseDTO> resultMono = resultController.getResult(votingId);

        StepVerifier.create(resultMono)
                .expectNext(dto)
                .verifyComplete();

        verify(resultService, times(1)).getResult(votingId);
    }

    @Test
    void getResult_WithAlphanumericVotingId_ShouldReturnResultResponseDTO() {
        String votingId = "vote-abc123-def";
        ResultResponseDTO dto = new ResultResponseDTO("vote-abc123-def", "Meeting schedule change", 5, "APROVADO");
        when(resultService.getResult(votingId)).thenReturn(Mono.just(dto));

        Mono<ResultResponseDTO> resultMono = resultController.getResult(votingId);

        StepVerifier.create(resultMono)
                .expectNext(dto)
                .verifyComplete();

        verify(resultService, times(1)).getResult(votingId);
    }

    @Test
    void getResult_WithUUIDVotingId_ShouldReturnResultResponseDTO() {
        String votingId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
        ResultResponseDTO dto = new ResultResponseDTO("f47ac10b-58cc-4372-a567-0e02b2c3d479", "System upgrade proposal", 0, "REPROVADO");
        when(resultService.getResult(votingId)).thenReturn(Mono.just(dto));

        Mono<ResultResponseDTO> resultMono = resultController.getResult(votingId);

        StepVerifier.create(resultMono)
                .expectNext(dto)
                .verifyComplete();

        verify(resultService, times(1)).getResult(votingId);
    }

    @Test
    void resultExists_ShouldReturnTrueResponseEntity() {
        String votingId = "456";
        when(resultService.isResultAvailable(votingId)).thenReturn(Mono.just(true));

        Mono<ResponseEntity<Boolean>> responseMono = resultController.resultExists(votingId);

        StepVerifier.create(responseMono)
                .expectNext(ResponseEntity.ok(true))
                .verifyComplete();

        verify(resultService, times(1)).isResultAvailable(votingId);
    }

    @Test
    void resultExists_ShouldReturnFalseResponseEntity() {
        String votingId = "789";
        when(resultService.isResultAvailable(votingId)).thenReturn(Mono.just(false));

        Mono<ResponseEntity<Boolean>> responseMono = resultController.resultExists(votingId);

        StepVerifier.create(responseMono)
                .expectNext(ResponseEntity.ok(false))
                .verifyComplete();

        verify(resultService, times(1)).isResultAvailable(votingId);
    }
}

