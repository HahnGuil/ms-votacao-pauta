package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VoteResponseDTO;
import br.com.hahn.votacao.domain.service.VoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VotingControllerTest {

    @Mock
    private VoteService voteService;

    @InjectMocks
    private VoteController voteController;

    @Test
    void testVote_ReturnsCreatedResponse() {
        String votingId = "123";
        VoteRequestDTO mockRequest = new VoteRequestDTO(votingId, "user1", "SIM");
        when(voteService.sendVoteToQueue(any(VoteRequestDTO.class))).thenReturn(Mono.empty());

        Mono<ResponseEntity<VoteResponseDTO>> resultMono = voteController.vote(votingId, mockRequest);

        ResponseEntity<VoteResponseDTO> response = resultMono.block();
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Voto recebido com sucesso", response.getBody().message());
        verify(voteService, times(1)).sendVoteToQueue(any(VoteRequestDTO.class));
    }
}

