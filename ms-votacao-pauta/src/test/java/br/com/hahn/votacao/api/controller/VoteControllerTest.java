package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VoteResponseDTO;
import br.com.hahn.votacao.domain.service.VoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VoteControllerTest {

    private VoteService voteService;
    private VoteController voteController;

    @BeforeEach
    void setUp() throws Exception {
        voteService = mock(VoteService.class);
        voteController = new VoteController(voteService);

        java.lang.reflect.Field field = voteController.getClass().getSuperclass().getDeclaredField("apiCurrentVersion");
        field.setAccessible(true);
        field.set(voteController, "/v1");
    }

    @Test
    void vote_shouldReturnCreatedResponse() {
        String version = "/v1";
        String votingId = "123";
        VoteRequestDTO requestDTO = new VoteRequestDTO(votingId, "user1", "YES", version);

        when(voteService.sendVoteToQueue(any(VoteRequestDTO.class))).thenReturn(Mono.empty());

        Mono<ResponseEntity<VoteResponseDTO>> responseMono = voteController.vote(version, votingId, requestDTO);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertEquals(HttpStatus.CREATED, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals("Voto recebido com sucesso", response.getBody().message());
                })
                .verifyComplete();

        ArgumentCaptor<VoteRequestDTO> captor = ArgumentCaptor.forClass(VoteRequestDTO.class);
        verify(voteService, times(1)).sendVoteToQueue(captor.capture());
        VoteRequestDTO captured = captor.getValue();
        assertEquals(votingId, captured.votingId());
        assertEquals("user1", captured.userId());
        assertEquals("YES", captured.voteOption());
        assertEquals(version, captured.apiVersion());
    }
}
