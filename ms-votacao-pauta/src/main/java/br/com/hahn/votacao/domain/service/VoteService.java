package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.enums.CpfStatus;
import br.com.hahn.votacao.domain.enums.VoteOption;
import br.com.hahn.votacao.domain.exception.InvalidCpfException;
import br.com.hahn.votacao.domain.exception.UserAlreadyVoteException;
import br.com.hahn.votacao.domain.exception.UserNotFoundException;
import br.com.hahn.votacao.domain.exception.VotingExpiredException;
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

/**
 * Service responsável pelo processamento e validação de votos no sistema.
 * <p>
 * Implementa um pipeline de validações sequenciais para garantir integridade
 * do processo de votação, incluindo validação de tempo, duplicidade de votos,
 * elegibilidade de CPF e controle de concorrência via Redis.
 * <p>
 * Utiliza processamento assíncrono via Kafka para persistência em lote
 * dos votos validados.
 *
 * @author HahnGuil
 * @since 1.0
 */
@Service
public class VoteService {

    private static final Logger voteServiceLogger = LoggerFactory.getLogger(VoteService.class);

    private final KafkaTemplate<String, VoteRequestDTO> kafkaTemplate;
    private final VoteRepository voteRepository;
    private final VotingService votingService;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final UserService userService;
    private final CpfValidationClient cpfValidationClient;

    public VoteService(KafkaTemplate<String, VoteRequestDTO> kafkaTemplate, VoteRepository voteRepository,
                       VotingService votingService, ReactiveStringRedisTemplate redisTemplate,
                       UserService userService, CpfValidationClient cpfValidationClient) {
        this.kafkaTemplate = kafkaTemplate;
        this.voteRepository = voteRepository;
        this.votingService = votingService;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        this.cpfValidationClient = cpfValidationClient;
    }

    /**
     * Processa voto através de pipeline de validações sequenciais otimizado.
     * <p>
     * **ORDEM ESTRATÉGICA DAS VALIDAÇÕES:**
     * <p>
     * 1. **Votação válida** (validateExpireVotingTime)
     *    - PRIMEIRO: Falha rápida se votação inativa/expirada
     *    - Evita processamento desnecessário de 90% dos casos de erro
     *    - Operação mais barata (consulta local no banco)
     * <p>
     * 2. **Duplicidade no banco** (hasUserAlreadyVoted)
     *    - SEGUNDO: Verifica histórico de votos já persistidos
     *    - Consulta rápida por índice composto (votingId + userId)
     *    - Previne reprocessamento de votos já finalizados
     * <p>
     * 3. **Controle de concorrência** (Redis setIfAbsent)
     *    - TERCEIRO: Lock distribuído para evitar votos simultâneos
     *    - Proteção contra race conditions entre múltiplas instâncias
     *    - TTL de 5 minutos para cleanup automático
     * <p>
     * 4. **Validação de CPF** (validateUserCpf)
     *    - ÚLTIMO: Integração externa mais custosa
     *    - Só executa após todas as validações baratas passarem
     *    - Inclui busca de usuário + chamada para serviço externo
     * <p>
     * Esta ordem **minimiza custos** ao falhar rapidamente em cenários
     * mais prováveis e deixar operações custosas por último.
     *
     * @param voteRequestDTO dados do voto a ser processado
     * @return completado quando voto for enviado para fila com sucesso
     * @throws VotingExpiredException se votação inativa ou expirada
     * @throws UserAlreadyVoteException se usuário já votou
     * @throws UserNotFoundException se usuário não existir
     * @throws InvalidCpfException se CPF inválido ou não habilitado
     */
    public Mono<Void> sendVoteToQueue(VoteRequestDTO voteRequestDTO) {
        voteServiceLogger.info("Iniciando validações para enviar votos para fila");

        return votingService.validateExpireVotingTime(voteRequestDTO.votingId())
                .then(validateUserNotAlreadyVoted(voteRequestDTO))
                .then(processVoteWithConcurrencyControl(voteRequestDTO));
    }

    /**
     * Verifica se usuário já registrou voto na votação específica.
     * <p>
     * Consulta otimizada por índice composto para verificação
     * rápida de duplicidade no histórico persistido.
     *
     * @param votingId ID da votação
     * @param userId ID do usuário
     * @return true se já votou, false caso contrário
     */
    public Mono<Boolean> hasUserAlreadyVoted(String votingId, String userId) {
        voteServiceLogger.info("Verificando se o usuário já votou");
        return voteRepository.findByVotingIdAndUserId(votingId, userId)
                .hasElement();
    }

    /**
     * Persiste lote de votos convertidos de DTOs.
     * <p>
     * Utilizado pelo consumer Kafka para persistência em massa
     * dos votos já validados e processados.
     *
     * @param voteRequestDTOs fluxo de DTOs a serem persistidos
     * @return fluxo de votos salvos
     */
    public Flux<Vote> saveAllFromDTO(Flux<VoteRequestDTO> voteRequestDTOs) {
        return voteRequestDTOs
                .map(this::convertToCollection)
                .collectList()
                .flatMapMany(voteRepository::saveAll);
    }

    /**
     * Recupera todos os votos de uma votação para apuração.
     *
     * @param votingId ID da votação
     * @return fluxo com todos os votos da votação
     */
    public Flux<Vote> findByVotingId(String votingId) {
        voteServiceLogger.info("Buscando todos os votos para a votação: {}", votingId);
        return voteRepository.findByVotingId(votingId);
    }

    /**
     * Valida se usuário já votou e retorna erro caso tenha votado.
     * <p>
     * Wrapper privado para hasUserAlreadyVoted que converte o resultado
     * booleano em erro quando necessário, mantendo o pipeline limpo.
     *
     * @param voteRequestDTO dados do voto
     * @return completado se usuário não votou ainda
     * @throws UserAlreadyVoteException se usuário já votou
     */
    private Mono<Void> validateUserNotAlreadyVoted(VoteRequestDTO voteRequestDTO) {
        return hasUserAlreadyVoted(voteRequestDTO.votingId(), voteRequestDTO.userId())
                .flatMap(alreadyVoted -> {
                    if (Boolean.TRUE.equals(alreadyVoted)) {
                        voteServiceLogger.warn("Usuário já votou, está salvo no banco!");
                        return Mono.error(new UserAlreadyVoteException("User has already voted"));
                    }
                    return Mono.empty();
                });
    }

    /**
     * Aplica controle de concorrência via Redis e processa voto se conseguir lock.
     * <p>
     * Utiliza setIfAbsent para garantir que apenas uma instância processe
     * o voto de um usuário específico em uma votação. Lock tem TTL de 5
     * minutos para cleanup automático em caso de falha.
     *
     * @param voteRequestDTO dados do voto
     * @return completado quando voto for processado
     * @throws UserAlreadyVoteException se não conseguir lock (voto em processamento)
     */
    private Mono<Void> processVoteWithConcurrencyControl(VoteRequestDTO voteRequestDTO) {
        String lockKey = buildLockKey(voteRequestDTO.votingId(), voteRequestDTO.userId());

        return redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "pending", Duration.ofMinutes(5))
                .flatMap(lockAcquired -> {
                    if (Boolean.TRUE.equals(lockAcquired)) {
                        return validateCpfAndSendToQueue(voteRequestDTO);
                    }
                    voteServiceLogger.info("Lançado a exceção para usuário que já votou");
                    return Mono.error(new UserAlreadyVoteException("User has already voted (pending in batch)"));
                });
    }

    /**
     * Valida CPF do usuário e envia voto para fila Kafka.
     * <p>
     * Último passo do pipeline de validação: executa validação externa
     * custosa do CPF e, se bem-sucedida, envia o voto para processamento
     * assíncrono via Kafka.
     *
     * @param voteRequestDTO dados do voto
     * @return completado quando voto for enviado para fila
     * @throws UserNotFoundException se usuário não existir
     * @throws InvalidCpfException se CPF inválido ou não habilitado
     */
    private Mono<Void> validateCpfAndSendToQueue(VoteRequestDTO voteRequestDTO) {
        return validateUserCpf(voteRequestDTO.userId())
                .doOnSuccess(ignored -> sendVoteToKafka(voteRequestDTO))
                .then();
    }

    /**
     * Envia voto para tópico Kafka para processamento assíncrono.
     * <p>
     * Operação fire-and-forget que delega o processamento em lote
     * para o consumer Kafka, melhorando performance e throughput.
     *
     * @param voteRequestDTO dados do voto validado
     */
    private void sendVoteToKafka(VoteRequestDTO voteRequestDTO) {
        voteServiceLogger.info("Enviando voto para a fila para usuário: {} na votação: {}",
                voteRequestDTO.userId(), voteRequestDTO.votingId());
        kafkaTemplate.send("vote-topic", voteRequestDTO);
    }

    /**
     * Constrói chave do lock Redis para controle de concorrência.
     * <p>
     * Formato: {votingId}:{userId} para garantir unicidade por
     * combinação usuário-votação.
     *
     * @param votingId ID da votação
     * @param userId ID do usuário
     * @return chave formatada para Redis
     */
    private String buildLockKey(String votingId, String userId) {
        return votingId + ":" + userId;
    }

    /**
     * Valida elegibilidade do CPF do usuário para votação.
     * <p>
     * Pipeline interno: busca usuário → extrai CPF → valida externamente.
     * Integração com serviço externo para verificar status de habilitação.
     *
     * @param userId ID do usuário a ser validado
     * @return completado se CPF válido e habilitado
     * @throws UserNotFoundException se usuário não existir
     * @throws InvalidCpfException se CPF inválido ou não habilitado
     */
    private Mono<Void> validateUserCpf(String userId) {
        voteServiceLogger.info("Iniciando validação de CPF para usuário: {}", userId);

        return getUserCpf(userId)
                .flatMap(this::validateCpfWithExternalService)
                .then();
    }

    /**
     * Busca CPF do usuário no banco de dados.
     * <p>
     * Inclui validação de existência do usuário e presença do CPF,
     * retornando erros específicos para cada cenário de falha.
     *
     * @param userId ID do usuário
     * @return CPF do usuário
     * @throws UserNotFoundException se usuário não existir
     * @throws InvalidCpfException se CPF não estiver cadastrado
     */
    private Mono<String> getUserCpf(String userId) {
        voteServiceLogger.info("Buscando CPF do usuário no banco: {}", userId);

        return userService.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Usuário não encontrado: " + userId)))
                .handle((user, sink) -> {
                    if (user.getUserCPF() == null || user.getUserCPF().trim().isEmpty()) {
                        sink.error(new InvalidCpfException("CPF não cadastrado para o usuário"));
                        return;
                    }
                    voteServiceLogger.info("CPF encontrado para o usuário: {}", userId);
                    sink.next(user.getUserCPF());
                });
    }

    /**
     * Valida CPF através de serviço externo.
     * <p>
     * Verifica se o CPF está habilitado para votação através de
     * integração com sistema externo de validação.
     *
     * @param cpf CPF a ser validado
     * @return completado se CPF habilitado
     * @throws InvalidCpfException se CPF não habilitado para votar
     */
    private Mono<Void> validateCpfWithExternalService(String cpf) {
        voteServiceLogger.info("CPF encontrado para o usuário, validando no serviço externo");

        return cpfValidationClient.validateCpf(cpf)
                .flatMap(cpfResponse -> {
                    if (cpfResponse.status() == CpfStatus.UNABLE_TO_VOTE) {
                        voteServiceLogger.warn("CPF não habilitado para votar");
                        return Mono.error(new InvalidCpfException("CPF não habilitado para votar"));
                    }
                    voteServiceLogger.info("CPF validado com sucesso - Status: {}", cpfResponse.status());
                    return Mono.empty();
                });
    }

    /**
     * Converte DTO de request para entidade de domínio.
     * <p>
     * Mapeamento simples entre camadas para persistência no banco.
     * Inclui conversão do voteOption de String para enum.
     *
     * @param voteRequestDTO dados de entrada
     * @return entidade Vote configurada
     */
    private Vote convertToCollection(VoteRequestDTO voteRequestDTO) {
        Vote vote = new Vote();
        vote.setVotingId(voteRequestDTO.votingId());
        vote.setUserId(voteRequestDTO.userId());
        vote.setVoteOption(VoteOption.fromString(voteRequestDTO.voteOption()));
        return vote;
    }
}
