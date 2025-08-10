package br.com.hahn.votacao.infrastructure.service;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.model.Vote;
import br.com.hahn.votacao.domain.service.VoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

class VoteBatchConsumerTest {

    private VoteService voteService;
    private ReactiveStringRedisTemplate redisTemplate;
    private VoteBatchConsumer consumer;

    @BeforeEach
    void setUp() {
        voteService = mock(VoteService.class);
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        consumer = new VoteBatchConsumer(voteService, redisTemplate);
    }

    @Test
    void receiveVote_shouldAddVoteToBatch_whenNotDuplicate() {
        VoteRequestDTO vote = new VoteRequestDTO("votingId", "userId", "SIM");
        consumer.receiveVote(vote);
        // Adiciona novamente para garantir que duplicados são ignorados
        consumer.receiveVote(vote);

        // flushBatch irá processar apenas um voto
        Vote modelVote = new Vote();
        modelVote.setVotingId("votingId");
        modelVote.setUserId("userId");
        when(voteService.saveAllFromDTO(any())).thenReturn(Flux.just(modelVote));
        when(redisTemplate.delete(anyString())).thenReturn(Mono.just(1L));

        consumer.flushBatch();

        verify(voteService, times(1)).saveAllFromDTO(any());
        verify(redisTemplate, times(1)).delete("votingId:userId");
    }

    @Test
    void flushBatch_shouldNotCallService_whenBatchIsEmpty() {
        consumer.flushBatch();
        verifyNoInteractions(voteService);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void scheduledFlush_shouldCallFlushBatch_whenBatchIsNotEmpty() {
        VoteRequestDTO vote = new VoteRequestDTO("votingId", "userId", "SIM");
        consumer.receiveVote(vote);

        Vote modelVote = new Vote();
        modelVote.setVotingId("votingId");
        modelVote.setUserId("userId");
        when(voteService.saveAllFromDTO(any())).thenReturn(Flux.just(modelVote));
        when(redisTemplate.delete(anyString())).thenReturn(Mono.just(1L));

        consumer.scheduledFlush();

        verify(voteService, times(1)).saveAllFromDTO(any());
        verify(redisTemplate, times(1)).delete("votingId:userId");
    }

    @Test
    void scheduledFlush_shouldNotCallFlushBatch_whenBatchIsEmpty() {
        consumer.scheduledFlush();
        verifyNoInteractions(voteService);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void forceFlushForVotingReactive_shouldFlushOnlyVotesForGivenVotingId() {
        VoteRequestDTO vote1 = new VoteRequestDTO("votingId", "userId1", "SIM");
        VoteRequestDTO vote2 = new VoteRequestDTO("votingId", "userId2", "NAO");
        VoteRequestDTO voteOther = new VoteRequestDTO("otherVoting", "userId3", "SIM");
        consumer.receiveVote(vote1);
        consumer.receiveVote(vote2);
        consumer.receiveVote(voteOther);

        Vote modelVote1 = new Vote();
        modelVote1.setVotingId("votingId");
        modelVote1.setUserId("userId1");
        Vote modelVote2 = new Vote();
        modelVote2.setVotingId("votingId");
        modelVote2.setUserId("userId2");

        when(voteService.saveAllFromDTO(any())).thenReturn(Flux.just(modelVote1, modelVote2));
        when(redisTemplate.delete(anyString())).thenReturn(Mono.just(1L));

        StepVerifier.create(consumer.forceFlushForVotingReactive("votingId"))
                .verifyComplete();

        verify(voteService, times(1)).saveAllFromDTO(any());
        verify(redisTemplate, times(1)).delete("votingId:userId1");
        verify(redisTemplate, times(1)).delete("votingId:userId2");
        // O voto de otherVoting permanece no batch
    }

    @Test
    void forceFlushForVotingReactive_shouldReturnEmptyMono_whenNoVotesForVotingId() {
        VoteRequestDTO vote = new VoteRequestDTO("otherVoting", "userId", "SIM");
        consumer.receiveVote(vote);

        StepVerifier.create(consumer.forceFlushForVotingReactive("votingId"))
                .verifyComplete();

        verifyNoInteractions(voteService);
        verifyNoInteractions(redisTemplate);
    }
}

