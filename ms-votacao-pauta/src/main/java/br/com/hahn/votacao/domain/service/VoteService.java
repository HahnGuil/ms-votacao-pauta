package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.enums.CpfStatus;
import br.com.hahn.votacao.domain.enums.VoteOption;
import br.com.hahn.votacao.domain.exception.InvalidCpfException;
import br.com.hahn.votacao.domain.exception.UserAlreadyVoteException;
import br.com.hahn.votacao.domain.exception.UserNotFoundException;
import br.com.hahn.votacao.domain.model.Vote;
import br.com.hahn.votacao.domain.repository.VoteRepository;
import br.com.hahn.votacao.infrastructure.client.CpfValidationClient;
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
    private final UserService userService;
    private final CpfValidationClient cpfValidationClient;

    public VoteService(KafkaTemplate<String, VoteRequestDTO> kafkaTemplate, VoteRepository voteRepository, VotingService votingService, ReactiveStringRedisTemplate redisTemplate, UserService userService, CpfValidationClient cpfValidationClient) {
        this.kafkaTemplate = kafkaTemplate;
        this.voteRepository = voteRepository;
        this.votingService = votingService;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        this.cpfValidationClient = cpfValidationClient;
    }

    public Mono<Void> sendVoteToQueue(VoteRequestDTO voteRequestDTO) {
        voteServiceLogger.info("Iniciando validações para enviar votos para fila");

        // 1. Validar se a votação ainda é válida (status ativo + não expirada)
        return votingService.validateExpireVotingTime(voteRequestDTO.votingId())
                // 2. Só executa se a primeira validação passou - Validar se o usuário já votou
                .then(Mono.defer(() -> hasUserAlreadyVoted(voteRequestDTO.votingId(), voteRequestDTO.userId())))
                .flatMap(alreadyVoted -> {
                    if (Boolean.TRUE.equals(alreadyVoted)) {
                        voteServiceLogger.warn("Usuário já votou, está salvo no banco!");
                        return Mono.error(new UserAlreadyVoteException("User has already voted"));
                    }

                    String key = voteRequestDTO.votingId() + ":" + voteRequestDTO.userId();
                    return redisTemplate.opsForValue()
                            .setIfAbsent(key, "pending", Duration.ofMinutes(5))
                            .flatMap(isNew -> {
                                if (Boolean.TRUE.equals(isNew)) {
                                    // 3. Validar se o CPF é válido
                                    return validateUserCpf(voteRequestDTO.userId())
                                            // 4. Enviar os votos para a fila
                                            .doOnSuccess(ignored -> {
                                                voteServiceLogger.info("Enviando voto para a fila para usuário: {} na votação: {}",
                                                        voteRequestDTO.userId(), voteRequestDTO.votingId());
                                                kafkaTemplate.send("vote-topic", voteRequestDTO);
                                            })
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

    private Mono<Void> validateUserCpf(String userId) {
        voteServiceLogger.info("Iniciando validação de CPF para usuário: {}", userId);

        return getUserCpf(userId)
                .flatMap(cpf -> {
                    voteServiceLogger.info("CPF encontrado para o usuário, validando no serviço externo");
                    return cpfValidationClient.validateCpf(cpf);
                })
                .flatMap(cpfResponse -> {
                    if (cpfResponse.status() == CpfStatus.UNABLE_TO_VOTE) {
                        voteServiceLogger.warn("CPF não habilitado para votar para usuário: {}", userId);
                        return Mono.error(new InvalidCpfException("CPF não habilitado para votar"));
                    }
                    voteServiceLogger.info("CPF validado com sucesso para usuário: {} - Status: {}",
                            userId, cpfResponse.status());
                    return Mono.empty();
                })
                .then();
    }

    private Mono<String> getUserCpf(String userId) {
        voteServiceLogger.info("Buscando CPF do usuário no banco: {}", userId);

        return userService.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Usuário não encontrado: " + userId)))
                .map(user -> {
                    if (user.getUserCPF() == null || user.getUserCPF().trim().isEmpty()) {
                        throw new InvalidCpfException("CPF não cadastrado para o usuário");
                    }
                    voteServiceLogger.info("CPF encontrado para o usuário: {}", userId);
                    return user.getUserCPF();
                });
    }



    private Vote convertToCollection(VoteRequestDTO voteRequestDTO) {
        Vote vote = new Vote();
        vote.setVotingId(voteRequestDTO.votingId());
        vote.setUserId(voteRequestDTO.userId());
        vote.setVoteOption(VoteOption.fromString(voteRequestDTO.voteOption()));
        return vote;
    }
}
