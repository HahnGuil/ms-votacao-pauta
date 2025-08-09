package br.com.hahn.votacao.infrastructure.service;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.service.VoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Component
public class VoteBatchConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VoteBatchConsumer.class);

    private final VoteService voteService;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final List<VoteRequestDTO> batch = new ArrayList<>();
    private final Set<String> batchKeys = new HashSet<>();

    public VoteBatchConsumer(VoteService voteService, ReactiveStringRedisTemplate redisTemplate) {
        this.voteService = voteService;
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "vote-topic", groupId = "vote-group")
    public void receiveVote(VoteRequestDTO vote) {
        synchronized (batch) {
            String voteKey = vote.votingId() + ":" + vote.userId();

            if (batchKeys.contains(voteKey)) {
                logger.info("Duplicate vote ignored in current batch for votingId: {}, userCPF: {}",
                        vote.votingId(), vote.userId());
                return;
            }

            batch.add(vote);
            batchKeys.add(voteKey);
            logger.info("Accepted vote for votingId: {}, userCPF: {}", vote.votingId(), vote.userId());
        }
    }

    public void flushBatch() {
        if (!batch.isEmpty()) {
            List<VoteRequestDTO> batchCopy;
            int batchSize;

            synchronized (batch) {
                batchCopy = new ArrayList<>(batch);
                batchSize = batch.size();
                batch.clear();
                batchKeys.clear();
            }

            logger.info("Starting batch flush with {} votes", batchSize);

            Flux<VoteRequestDTO> voteFlux = Flux.fromIterable(batchCopy);
            voteService.saveAllFromDTO(voteFlux)
                    .flatMap(vote -> {
                        String key = vote.getVotingId() + ":" + vote.getUserId();
                        return redisTemplate.delete(key)
                                .doOnError(e -> logger.error("Erro ao remover chave do Redis para {}", key, e));
                    })
                    .subscribe(
                            null,
                            ex -> logger.error("Erro ao processar batch de {} votos", batchSize, ex),
                            () -> logger.info("Batch processado com sucesso. {} votos salvos e chaves Redis removidas", batchSize)
                    );
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
}