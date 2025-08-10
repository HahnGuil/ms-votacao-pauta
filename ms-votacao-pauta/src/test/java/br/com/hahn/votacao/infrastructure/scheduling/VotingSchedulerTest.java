package br.com.hahn.votacao.infrastructure.scheduling;


import br.com.hahn.votacao.domain.VotingClosedEvent;
import br.com.hahn.votacao.domain.model.Voting;
import br.com.hahn.votacao.domain.service.VotingService;
import br.com.hahn.votacao.infrastructure.service.VoteBatchConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class VotingSchedulerTest {

    private VotingService votingService;
    private VoteBatchConsumer voteBatchConsumer;
    private ApplicationEventPublisher eventPublisher;
    private VotingScheduler votingScheduler;

    @BeforeEach
    void setUp() {
        votingService = mock(VotingService.class);
        voteBatchConsumer = mock(VoteBatchConsumer.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        votingScheduler = new VotingScheduler(votingService, voteBatchConsumer, eventPublisher);
    }

    @Test
    void checkAndCloseExpiredVotings_shouldCloseExpiredVotingAndPublishEvent() {
        Voting voting = new Voting();
        voting.setVotingId("votingId");
        voting.setSubject("subject");
        voting.setVotingSatus(true);
        voting.setCloseVotingDate(Instant.now().minusSeconds(10));

        when(votingService.findAllVotings()).thenReturn(Flux.just(voting));
        when(voteBatchConsumer.forceFlushForVotingReactive("votingId")).thenReturn(Mono.empty());
        when(votingService.saveVoting(any(Voting.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<Void> result = Mono.fromRunnable(() -> votingScheduler.checkAndCloseExpiredVotings());

        StepVerifier.create(result)
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        verify(voteBatchConsumer, times(1)).forceFlushForVotingReactive("votingId");
        verify(votingService, times(1)).saveVoting(any(Voting.class));
        verify(eventPublisher, times(1)).publishEvent(any(VotingClosedEvent.class));
    }

    @Test
    void checkAndCloseExpiredVotings_shouldNotCloseIfNotExpiredOrAlreadyClosed() {
        Voting votingActive = new Voting();
        votingActive.setVotingId("active");
        votingActive.setVotingSatus(true);
        votingActive.setCloseVotingDate(Instant.now().plusSeconds(100));

        Voting votingClosed = new Voting();
        votingClosed.setVotingId("closed");
        votingClosed.setVotingSatus(false);
        votingClosed.setCloseVotingDate(Instant.now().minusSeconds(100));

        when(votingService.findAllVotings()).thenReturn(Flux.just(votingActive, votingClosed));

        Mono<Void> result = Mono.fromRunnable(() -> votingScheduler.checkAndCloseExpiredVotings());

        StepVerifier.create(result)
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        verify(voteBatchConsumer, never()).forceFlushForVotingReactive(anyString());
        verify(votingService, never()).saveVoting(any(Voting.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void checkAndCloseExpiredVotings_shouldHandleErrorGracefully() {
        Voting voting = new Voting();
        voting.setVotingId("errId");
        voting.setVotingSatus(true);
        voting.setCloseVotingDate(Instant.now().minusSeconds(10));

        when(votingService.findAllVotings()).thenReturn(Flux.just(voting));
        when(voteBatchConsumer.forceFlushForVotingReactive("errId")).thenReturn(Mono.error(new RuntimeException("flush error")));

        Mono<Void> result = Mono.fromRunnable(() -> votingScheduler.checkAndCloseExpiredVotings());

        StepVerifier.create(result)
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        verify(voteBatchConsumer, times(1)).forceFlushForVotingReactive("errId");
        verify(votingService, never()).saveVoting(any(Voting.class));
        verify(eventPublisher, never()).publishEvent(any());
    }
}

