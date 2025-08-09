package br.com.hahn.votacao.infrastructure.service;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.service.VoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Component
public class VoteBatchConsumer {

    private static final Logger voteBatcConsumerLogger = LoggerFactory.getLogger(VoteBatchConsumer.class);

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
                voteBatcConsumerLogger.info("Usuário já votou, voto ignorado. CPF do usuário: {}, userCPF: {}",
                        vote.votingId(), vote.userId());
                return;
            }

            batch.add(vote);
            batchKeys.add(voteKey);
            voteBatcConsumerLogger.info("Aceito o voto do usuário com CPF: {}, userCPF: {}", vote.votingId(), vote.userId());
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

            voteBatcConsumerLogger.info("Flushing dos votos dos usuários {} total de votos", batchSize);

            Flux<VoteRequestDTO> voteFlux = Flux.fromIterable(batchCopy);
            voteService.saveAllFromDTO(voteFlux)
                    .flatMap(vote -> {
                        String key = vote.getVotingId() + ":" + vote.getUserId();
                        return redisTemplate.delete(key)
                                .doOnError(e -> voteBatcConsumerLogger.error("Erro ao remover chave do Redis para {}", key, e));
                    })
                    .subscribe(
                            null,
                            ex -> voteBatcConsumerLogger.error("Erro ao processar batch de {} votos", batchSize, ex),
                            () -> voteBatcConsumerLogger.info("Batch processado com sucesso. {} votos salvos e chaves Redis removidas", batchSize)
                    );
        }
    }

    public void scheduledFlush() {
        synchronized (batch) {
            if (!batch.isEmpty()) {
                voteBatcConsumerLogger.info("Disparada a ação de flush via Scheduled. Flushing {} votes to database", batch.size());
                flushBatch();
            }
        }
    }

    public Mono<Void> forceFlushForVotingReactive(String votingId) {
        return Mono.fromCallable(() -> {
                    synchronized (batch) {
                        List<VoteRequestDTO> votesForVoting = batch.stream()
                                .filter(vote -> votingId.equals(vote.votingId()))
                                .toList(); // Substituído .collect(Collectors.toList())

                        if (!votesForVoting.isEmpty()) {
                            voteBatcConsumerLogger.info("Forçando o flush após o fechamento da votação. {} votos encontrados para votingId: {}",
                                    votesForVoting.size(), votingId);

                            // Remove os votos dessa votação do batch atual
                            batch.removeIf(vote -> votingId.equals(vote.votingId()));
                            votesForVoting.forEach(vote -> {
                                String key = vote.votingId() + ":" + vote.userId();
                                batchKeys.remove(key);
                            });
                        }

                        return votesForVoting;
                    }
                })
                .flatMap(votesForVoting -> processVotesReactively(votesForVoting, votingId));
    }

    private Mono<Void> processVotesReactively(List<VoteRequestDTO> votes, String votingId) {
        if (votes.isEmpty()) {
            voteBatcConsumerLogger.info("Nenhum voto pendente encontrado para a votação: {}", votingId);
            return Mono.empty();
        }

        return Flux.fromIterable(votes)
                .as(voteService::saveAllFromDTO)
                .flatMap(vote -> {
                    String key = vote.getVotingId() + ":" + vote.getUserId();
                    return redisTemplate.delete(key)
                            .doOnError(e -> voteBatcConsumerLogger.error("Erro ao remover chave do Redis para {}", key, e))
                            .onErrorReturn(0L); // Continua mesmo se falhar ao deletar do Redis
                })
                .then()
                .doOnSuccess(unused -> voteBatcConsumerLogger.info("Votos da votação {} processados com sucesso no encerramento", votingId))
                .doOnError(ex -> voteBatcConsumerLogger.error("Erro ao processar votos da votação {} no encerramento", votingId, ex));
    }
}