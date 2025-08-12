package br.com.hahn.votacao.infrastructure.event;

import br.com.hahn.votacao.domain.VotingClosedEvent;
import br.com.hahn.votacao.domain.dto.response.ResultResponseDTO;
import br.com.hahn.votacao.domain.service.ResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void handleVotingClosed_shouldHandleEventWithNullVotingId() {
        VotingClosedEvent eventWithNullId = new VotingClosedEvent(null, "Subject", Instant.now(), 10);

        when(resultService.createResult(null)).thenReturn(Mono.error(new IllegalArgumentException("Voting ID cannot be null")));

        assertDoesNotThrow(() -> {
            votingEventHandler.handleVotingClosed(eventWithNullId);
        });

        verify(resultService, times(1)).createResult(null);
    }

    @Test
    void handleVotingClosed_shouldHandleEmptyMonoFromResultService() {
        String votingId = "voting789";
        VotingClosedEvent event = new VotingClosedEvent(votingId, "Subject", Instant.now(), 0);

        when(resultService.createResult(votingId)).thenReturn(Mono.empty());

        assertDoesNotThrow(() -> {
            votingEventHandler.handleVotingClosed(event);
        });

        verify(resultService, times(1)).createResult(votingId);
    }

    @Test
    void handleVotingClosed_shouldVerifyAsyncExecution() throws InterruptedException {
        String votingId = "asyncTest";
        VotingClosedEvent event = new VotingClosedEvent(votingId, "Async Subject", Instant.now(), 15);
        CountDownLatch latch = new CountDownLatch(1);

        when(resultService.createResult(votingId)).thenReturn(
                Mono.fromCallable(() -> {
                    latch.countDown();
                    return mock(ResultResponseDTO.class);
                })
        );

        votingEventHandler.handleVotingClosed(event);

        // Aguarda até 2 segundos pela execução assíncrona
        assertTrue(latch.await(2, TimeUnit.SECONDS), "O método deveria executar de forma assíncrona");
        verify(resultService, times(1)).createResult(votingId);
    }

    @Test
    void handleVotingClosed_shouldVerifySubscriptionOnBoundedElastic() throws InterruptedException {
        String votingId = "schedulerTest";
        VotingClosedEvent event = new VotingClosedEvent(votingId, "Scheduler Subject", Instant.now(), 20);
        AtomicReference<String> threadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        when(resultService.createResult(votingId)).thenReturn(
                Mono.fromCallable(() -> {
                    threadName.set(Thread.currentThread().getName());
                    latch.countDown();
                    return mock(ResultResponseDTO.class);
                })
        );

        votingEventHandler.handleVotingClosed(event);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(threadName.get().contains("boundedElastic"),
                "Deveria executar em thread do boundedElastic scheduler");
        verify(resultService, times(1)).createResult(votingId);
    }

    @Test
    void handleVotingClosed_shouldHandleResultServiceTimeout() {
        String votingId = "timeoutTest";
        VotingClosedEvent event = new VotingClosedEvent(votingId, "Timeout Subject", Instant.now(), 25);

        when(resultService.createResult(votingId)).thenReturn(
                Mono.delay(Duration.ofSeconds(10))
                        .then(Mono.just(mock(ResultResponseDTO.class)))
        );

        assertDoesNotThrow(() -> {
            votingEventHandler.handleVotingClosed(event);
        });

        verify(resultService, times(1)).createResult(votingId);
    }

    @Test
    void constructor_shouldAcceptResultService() {
        assertDoesNotThrow(() -> {
            new VotingEventHandler(resultService);
        });
    }

}

