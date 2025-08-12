package br.com.hahn.votacao.infrastructure.service;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.service.VoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
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
    void flushBatch_shouldNotCallService_whenBatchIsEmpty() {
        consumer.flushBatch();
        verifyNoInteractions(voteService);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void scheduledFlush_shouldNotCallFlushBatch_whenBatchIsEmpty() {
        consumer.scheduledFlush();
        verifyNoInteractions(voteService);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void forceFlushForVotingReactive_shouldReturnEmptyMono_whenNoVotesForVotingId() {
        VoteRequestDTO vote = new VoteRequestDTO("otherVoting", "userId", "SIM", "v1");
        consumer.receiveVote(vote);

        StepVerifier.create(consumer.forceFlushForVotingReactive("votingId"))
                .verifyComplete();

        verifyNoInteractions(voteService);
        verifyNoInteractions(redisTemplate);
    }
}

