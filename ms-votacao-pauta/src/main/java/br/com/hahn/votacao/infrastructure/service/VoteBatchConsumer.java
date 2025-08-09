package br.com.hahn.votacao.infrastructure.service;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.service.VoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class VoteBatchConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VoteBatchConsumer.class);

    private final VoteService voteService;
    private final StringRedisTemplate redisTemplate;
    private final List<VoteRequestDTO> batch = new ArrayList<>();

    public VoteBatchConsumer(VoteService voteService, StringRedisTemplate redisTemplate) {
        this.voteService = voteService;
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "vote-topic", groupId = "vote-group")
    public void receiveVote(VoteRequestDTO vote) {
        synchronized (batch) {
            batch.add(vote);
            logger.info("Reveived voto for votingId: {}", vote.votingId());
            if (batch.size() >= 30) {
                logger.info("Batch size reached 30. Flushing batch to dataBase");
                flushBatch();
            }
        }
    }

    public void scheduledFlush() {
        synchronized (batch) {
            if (!batch.isEmpty()) {
                logger.info("Scheduled flush triggered. Flushing {} votes to database", batch.size());
                flushBatch();
            }
        }
    }

    private void flushBatch() {
        voteService.saveAllFromDTO(batch);
        batch.forEach(vote -> redisTemplate.opsForValue().increment("vote-count:" + vote.votingId()));
        batch.clear();
    }
}
