package br.com.hahn.votacao.api.controller;

// ms-votacao-pauta/src/test/java/br/com/hahn/votacao/api/controller/VotingControllerTest.java
import br.com.hahn.votacao.domain.dto.request.VotingRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VotingResponseDTO;
import br.com.hahn.votacao.domain.service.VotingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VotingControllerTest {

    @Mock
    private VotingService votingService;

    @InjectMocks
    private VotingController votingController;

    @Test
    void testCreateVoting_ReturnsCreatedResponse() throws Exception {
        // Initialize inherited field to avoid NullPointerException
        Field field = votingController.getClass().getSuperclass().getDeclaredField("apiCurrentVersion");
        field.setAccessible(true);
        field.set(votingController, "/v1");

        String version = "v1";
        // Use Integer for expiration as expected by VotingRequestDTO
        VotingRequestDTO requestDTO = new VotingRequestDTO("Assunto", 60, "v1");

        // All parameters as String for VotingResponseDTO
        VotingResponseDTO responseDTO = new VotingResponseDTO("voting-id", "api/vote", "2024-12-31T23:59:59", "api/result");

        when(votingService.createVoting(any(VotingRequestDTO.class)))
                .thenReturn(Mono.just(responseDTO));

        Mono<ResponseEntity<VotingResponseDTO>> resultMono =
                votingController.createVoting(version, requestDTO);

        ResponseEntity<VotingResponseDTO> response = resultMono.block();
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(responseDTO.votingId(), response.getBody().votingId());
        assertEquals(responseDTO.closeVotingDate(), response.getBody().closeVotingDate());

        verify(votingService, times(1)).createVoting(any(VotingRequestDTO.class));
    }
}
