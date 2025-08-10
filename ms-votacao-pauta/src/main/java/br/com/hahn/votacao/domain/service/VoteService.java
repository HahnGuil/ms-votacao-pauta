package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.enums.VoteOption;
import br.com.hahn.votacao.domain.exception.UserAlreadyVoteException;
import br.com.hahn.votacao.domain.model.Vote;
import br.com.hahn.votacao.domain.repository.VoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class VoteService {

    private static final Logger voteServiceLogger = LoggerFactory.getLogger(VoteService.class);

    private final KafkaTemplate<String, VoteRequestDTO> kafkaTemplate;
    private final VoteRepository voteRepository;
    private final VotingService votingService;
    private final ReactiveStringRedisTemplate redisTemplate;

    public VoteService(KafkaTemplate<String, VoteRequestDTO> kafkaTemplate, VoteRepository voteRepository, VotingService votingService, ReactiveStringRedisTemplate redisTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.voteRepository = voteRepository;
        this.votingService = votingService;
        this.redisTemplate = redisTemplate;
    }

    public Mono<Void> sendVoteToQueue(VoteRequestDTO voteRequestDTO) {
        voteServiceLogger.info("Inciando envio de votos para a fila");
        String key = voteRequestDTO.votingId() + ":" + voteRequestDTO.userId();
        return hasUserAlreadyVoted(voteRequestDTO.votingId(), voteRequestDTO.userId())
                .flatMap(alreadyVoted -> {
                    if (Boolean.TRUE.equals(alreadyVoted)) {
                        voteServiceLogger.warn("Usuário já votou, esta salvo no banco!");
                        return Mono.error(new UserAlreadyVoteException("User has already voted"));
                    }
                    return redisTemplate.opsForValue()
                            .setIfAbsent(key, "pending", Duration.ofMinutes(5))
                            .flatMap(isNew -> {
                                if (Boolean.TRUE.equals(isNew)) {
                                    return votingService.validateExpireVotingTime(voteRequestDTO.votingId())
                                            .then(Mono.fromRunnable(() -> kafkaTemplate.send("vote-topic", voteRequestDTO)))
                                            .then();
                                }
                                voteServiceLogger.info("Lançado a exceção para usuário que já votou");
                                return Mono.error(new UserAlreadyVoteException("User has already voted (pending in batch)"));
                            });
                });
    }

    public Mono<Boolean> hasUserAlreadyVoted(String votingId, String userId) {
        voteServiceLogger.info("Verificando se o usuário já votou");
        return voteRepository.findByVotingIdAndUserId(votingId, userId)
                .hasElement();
    }

    public Flux<Vote> saveAllFromDTO(Flux<VoteRequestDTO> voteRequestDTOs) {
        return voteRequestDTOs
                .map(this::convertToCollection)
                .collectList()
                .flatMapMany(voteRepository::saveAll);
    }

    public Flux<Vote> findByVotingId(String votingId) {
        voteServiceLogger.info("Buscando todos os votos para a votação: {}", votingId);
        return voteRepository.findByVotingId(votingId);
    }



    private Vote convertToCollection(VoteRequestDTO voteRequestDTO) {
        Vote vote = new Vote();
        vote.setVotingId(voteRequestDTO.votingId());
        vote.setUserId(voteRequestDTO.userId());
        vote.setVoteOption(VoteOption.fromString(voteRequestDTO.voteOption()));
        return vote;
    }
}
