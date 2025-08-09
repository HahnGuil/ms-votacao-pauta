package br.com.hahn.votacao.api.controller;

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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VotingControllerTest {

    @Mock
    private VotingService votingService;

    @InjectMocks
    private VotingController votingController;

    @Test
    void testCreateVoting_ReturnsCreatedResponse() {
        VotingRequestDTO mockRequest = new VotingRequestDTO("Teste", "5");

        String votingId = "6896c1cd2cf92a49ee6ddc14";
        String voteUrl = "http://localhost/voting/" + votingId;
        Instant closeVotingDate = Instant.now().plusSeconds(300);

        VotingResponseDTO mockResponse = new VotingResponseDTO(votingId, voteUrl, closeVotingDate);

        when(votingService.createVoting(mockRequest)).thenReturn(mockResponse);

        Mono<ResponseEntity<VotingResponseDTO>> resultMono = votingController.createVoting(mockRequest);

        ResponseEntity<VotingResponseDTO> response = resultMono.block();
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(votingService, times(1)).createVoting(mockRequest);
    }
}

