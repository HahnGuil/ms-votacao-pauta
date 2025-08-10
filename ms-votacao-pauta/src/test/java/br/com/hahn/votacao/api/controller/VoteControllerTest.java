package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VoteResponseDTO;
import br.com.hahn.votacao.domain.service.VoteService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

class VoteControllerTest {

    private VoteService voteService;
    private VoteController voteController;

    @BeforeEach
    void setUp() {
        voteService = Mockito.mock(VoteService.class);
        voteController = new VoteController(voteService);
    }

    @Test
    void testVote_ReturnsCreatedResponse() {
        String votingId = "voting123";
        VoteRequestDTO voteRequestDTO = new VoteRequestDTO("06f8376c08df3ec","user456", "SIM");
        VoteRequestDTO expectedVote = new VoteRequestDTO(votingId, voteRequestDTO.userId(), voteRequestDTO.voteOption());

        Mockito.when(voteService.sendVoteToQueue(any(VoteRequestDTO.class)))
                .thenReturn(Mono.empty());

        Mono<ResponseEntity<VoteResponseDTO>> result = voteController.vote(votingId, voteRequestDTO);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.CREATED, response.getStatusCode());
                    Assertions.assertNotNull(response.getBody());
                    assertEquals("Voto recebido com sucesso", response.getBody().message());
                })
                .verifyComplete();

        Mockito.verify(voteService).sendVoteToQueue(expectedVote);
    }
}

