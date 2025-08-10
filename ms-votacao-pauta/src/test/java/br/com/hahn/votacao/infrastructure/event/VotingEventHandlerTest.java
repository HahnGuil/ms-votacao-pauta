package br.com.hahn.votacao.infrastructure.event;

import br.com.hahn.votacao.domain.VotingClosedEvent;
import br.com.hahn.votacao.domain.dto.response.ResultResponseDTO;
import br.com.hahn.votacao.domain.service.ResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.Mockito.*;

class VotingEventHandlerTest {

    private ResultService resultService;
    private VotingEventHandler votingEventHandler;

    @BeforeEach
    void setUp() {
        resultService = mock(ResultService.class);
        votingEventHandler = new VotingEventHandler(resultService);
    }

    @Test
    void handleVotingClosed_shouldCallCreateResultAndSubscribeSuccessfully() {
        String votingId = "voting123";
        VotingClosedEvent event = new VotingClosedEvent(votingId, "Test Subject", Instant.now(), 10);
        ResultResponseDTO result = mock(ResultResponseDTO.class);

        when(resultService.createResult(votingId)).thenReturn(Mono.just(result));

        votingEventHandler.handleVotingClosed(event);

        verify(resultService, times(1)).createResult(votingId);
    }

    @Test
    void handleVotingClosed_shouldLogErrorOnFailure() {
        String votingId = "voting456";
        VotingClosedEvent event = new VotingClosedEvent(votingId, "Another Subject", Instant.now(), 5);

        when(resultService.createResult(votingId)).thenReturn(Mono.error(new RuntimeException("fail")));

        votingEventHandler.handleVotingClosed(event);

        verify(resultService, times(1)).createResult(votingId);
    }
}

