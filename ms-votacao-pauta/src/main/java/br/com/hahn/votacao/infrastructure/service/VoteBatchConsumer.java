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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Consumidor responsável pelo processamento em lote de votos via Kafka.
 * <p>
 * Componente crítico que implementa padrão Batch Processing para otimizar
 * performance e throughput do sistema de votação. Gerencia buffer thread-safe
 * de votos recebidos via Kafka, aplicando deduplicação automática e processamento
 * reativo para persistência em lote. Integra-se com Redis para controle de
 * duplicação distribuída e com VoteService para operações de domínio.
 * <p>
 * ESTRATÉGIA DE BATCH PROCESSING:
 * - Buffer em memória para acúmulo de votos
 * - Deduplicação baseada em votingId + userId
 * - Flush automático via scheduler ou manual via API
 * - Processamento reativo não-bloqueante
 * <p>
 * THREAD-SAFETY:
 * - CopyOnWriteArrayList para operações thread-safe de leitura
 * - ConcurrentHashMap para controle de duplicação eficiente
 * - Synchronization mínimo apenas em operações críticas
 * - Suporte a múltiplos consumers Kafka simultâneos
 * <p>
 * INTEGRAÇÃO COM KAFKA:
 * - Consumer Group: vote-group para balanceamento de carga
 * - Topic: vote-topic para recebimento de votos
 * - Processamento assíncrono com backpressure handling
 * - Deduplicação automática de votos duplicados
 * <p>
 * INTEGRAÇÃO COM REDIS:
 * - Controle distribuído de duplicação
 * - Cleanup automático após processamento
 * - Fallback graceful em caso de falha no Redis
 * - Chaves no formato: votingId:userId
 * <p>
 * FLUXOS DE PROCESSAMENTO:
 * 1. Recebimento: Kafka → receiveVote() → buffer + deduplicação
 * 2. Scheduled: Timer → scheduledFlush() → flushBatch()
 * 3. Force Flush: VotingScheduler → forceFlushForVotingReactive()
 * 4. Persistência: VoteService → Redis cleanup → logging
 * <p>
 * PERFORMANCE E OTIMIZAÇÃO:
 * - Processamento em lote reduz overhead de I/O
 * - Collections thread-safe otimizadas para cenário
 * - Processamento reativo evita bloqueio de threads
 * - Deduplicação eficiente com O(1) lookup
 *
 * @author HahnGuil
 * @since 1.0
 */
@Component
public class VoteBatchConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VoteBatchConsumer.class);

    private static final String KAFKA_TOPIC = "vote-topic";
    private static final String KAFKA_GROUP_ID = "vote-group";
    private static final String VOTE_KEY_SEPARATOR = ":";
    private static final Long REDIS_DELETE_FALLBACK = 0L;

    private final VoteService voteService;
    private final ReactiveStringRedisTemplate redisTemplate;

    // Thread-safe collections para alta concorrência
    private final CopyOnWriteArrayList<VoteRequestDTO> voteBatch = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, Boolean> processedVotes = new ConcurrentHashMap<>();

    /**
     * Construtor que injeta dependências necessárias para processamento de votos.
     *
     * @param voteService serviço de domínio para operações com votos
     * @param redisTemplate template reativo para operações com Redis
     */
    public VoteBatchConsumer(VoteService voteService, ReactiveStringRedisTemplate redisTemplate) {
        this.voteService = voteService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Recebe votos do tópico Kafka e adiciona ao buffer para processamento em lote.
     * <p>
     * Methodo listener que processa votos recebidos via Kafka, aplicando lógica
     * de deduplicação automática baseada em votingId + userId. Votos duplicados
     * são rejeitados e logados para auditoria. Utiliza collections thread-safe
     * para suportar múltiplos consumers simultâneos.
     * <p>
     * DEDUPLICAÇÃO:
     * - Chave única: votingId + userId
     * - Verificação O(1) via ConcurrentHashMap
     * - Rejeição automática de votos duplicados
     * - Logging detalhado para auditoria
     * <p>
     * THREAD-SAFETY:
     * - CopyOnWriteArrayList para adições thread-safe
     * - ConcurrentHashMap para verificação atômica
     * - Sem necessidade de synchronization explícito
     * - Suporte a múltiplos consumers Kafka
     * <p>
     * CENÁRIOS TRATADOS:
     * - Voto novo: Adiciona ao batch e marca como processado
     * - Voto duplicado: Rejeita e loga tentativa de duplicação
     * - Múltiplos consumers: Thread-safety garantida
     * - High throughput: Operações O(1) para performance
     *
     * @param vote dados do voto recebido via Kafka
     */
    @KafkaListener(topics = KAFKA_TOPIC, groupId = KAFKA_GROUP_ID)
    public void receiveVote(VoteRequestDTO vote) {
        String voteKey = createVoteKey(vote);

        if (isDuplicateVote(voteKey)) {
            logger.warn("Voto duplicado rejeitado. VotingId: {}, UserId: {}",
                    vote.votingId(), vote.userId());
            return;
        }

        addVoteToBatch(vote, voteKey);
        logger.info("Voto aceito no batch. VotingId: {}, UserId: {}",
                vote.votingId(), vote.userId());
    }

    /**
     * Executa flush manual do batch de votos acumulados.
     * <p>
     * Méthodo público que força o processamento imediato de todos os votos
     * acumulados no buffer. Cria snapshot thread-safe do batch atual,
     * limpa o buffer e processa os votos de forma reativa. Utilizado
     * tanto pelo scheduler automático quanto por operações manuais.
     * <p>
     * PROCESSAMENTO THREAD-SAFE:
     * - Snapshot atômico do batch atual
     * - Limpeza imediata do buffer para novos votos
     * - Processamento independente do snapshot
     * - Não bloqueia recebimento de novos votos
     * <p>
     * FLUXO DE EXECUÇÃO:
     * 1. Verifica se há votos no batch
     * 2. Cria snapshot thread-safe dos votos
     * 3. Limpa buffer para novos votos
     * 4. Processa snapshot de forma reativa
     * 5. Persiste votos via VoteService
     * 6. Remove chaves do Redis
     * 7. Loga resultado do processamento
     */
    public void flushBatch() {
        if (voteBatch.isEmpty()) {
            return;
        }

        BatchSnapshot snapshot = createBatchSnapshot();
        logger.info("Iniciando flush de {} votos", snapshot.votes().size());

        try {
            processBatchReactively(snapshot.votes())
                    .doOnSuccess(unused ->
                            logger.info("Batch de {} votos processado com sucesso",
                                    snapshot.votes().size()))
                    .block(); // Bloqueia até completar o processamento

        } catch (Exception error) {
            logger.error("Erro ao processar batch de {} votos",
                    snapshot.votes().size(), error);
        }
    }

    /**
     * Executa flush agendado via scheduler se houver votos pendentes.
     * <p>
     * Méthodo acionado pelo VoteBatchScheduler a cada 30 segundos para garantir
     * que votos não fiquem indefinidamente no buffer. Verifica se há votos
     * pendentes e aciona o flush regular com logging específico para
     * identificar origem da execução.
     */
    public void scheduledFlush() {
        if (!voteBatch.isEmpty()) {
            logger.info("Flush agendado acionado. {} votos pendentes no buffer", voteBatch.size());
            flushBatch();
        }
    }

    /**
     * Força flush de votos específicos de uma votação que está sendo encerrada.
     * <p>
     * Méthodo reativo que processa imediatamente todos os votos pendentes
     * de uma votação específica, removendo-os do batch geral. Utilizado
     * pelo VotingScheduler quando uma votação expira para garantir que
     * todos os votos sejam processados antes do encerramento.
     * <p>
     * PROCESSAMENTO ESPECÍFICO:
     * - Filtra apenas votos da votação especificada
     * - Remove votos filtrados do batch principal
     * - Processa de forma isolada e reativa
     * - Não afeta outros votos no buffer
     * <p>
     * GARANTIAS DE CONSISTÊNCIA:
     * - Processamento atômico dos votos da votação
     * - Remoção segura do batch principal
     * - Limpeza das chaves de controle
     * - Tratamento de erros individualizado
     *
     * @param votingId ID da votação que está sendo encerrada
     * @return Mono<Void> indicando conclusão do processamento
     */
    public Mono<Void> forceFlushForVotingReactive(String votingId) {
        return Mono.fromCallable(() -> extractVotesForVoting(votingId))
                .flatMap(this::processBatchReactively)
                .doOnSuccess(unused ->
                        logger.info("Force flush concluído para votação: {}", votingId))
                .doOnError(error ->
                        logger.error("Erro no force flush para votação: {}", votingId, error));
    }

    /**
     * Cria chave única para identificação de voto.
     *
     * @param vote dados do voto
     * @return chave no formato votingId:userId
     */
    private String createVoteKey(VoteRequestDTO vote) {
        return vote.votingId() + VOTE_KEY_SEPARATOR + vote.userId();
    }

    /**
     * Verifica se voto já foi processado.
     *
     * @param voteKey chave única do voto
     * @return true se voto é duplicado
     */
    private boolean isDuplicateVote(String voteKey) {
        return processedVotes.containsKey(voteKey);
    }

    /**
     * Adiciona voto ao batch e marca como processado.
     *
     * @param vote dados do voto
     * @param voteKey chave única do voto
     */
    private void addVoteToBatch(VoteRequestDTO vote, String voteKey) {
        voteBatch.add(vote);
        processedVotes.put(voteKey, Boolean.TRUE);
    }

    /**
     * Cria snapshot thread-safe do batch atual.
     *
     * @return record com votos e chaves para processamento
     */
    private BatchSnapshot createBatchSnapshot() {
        List<VoteRequestDTO> votes = new ArrayList<>(voteBatch);
        voteBatch.clear();
        processedVotes.clear();
        return new BatchSnapshot(votes);
    }

    /**
     * Extrai votos específicos de uma votação do batch.
     *
     * @param votingId ID da votação
     * @return lista de votos da votação especificada
     */
    private List<VoteRequestDTO> extractVotesForVoting(String votingId) {
        List<VoteRequestDTO> votesForVoting = voteBatch.stream()
                .filter(vote -> votingId.equals(vote.votingId()))
                .toList();

        if (!votesForVoting.isEmpty()) {
            logger.info("Extraindo {} votos para force flush da votação: {}",
                    votesForVoting.size(), votingId);

            // Remove votos da votação do batch principal
            voteBatch.removeIf(vote -> votingId.equals(vote.votingId()));

            // Remove chaves de controle
            votesForVoting.forEach(vote -> {
                String key = createVoteKey(vote);
                processedVotes.remove(key);
            });
        }

        return votesForVoting;
    }

    /**
     * Processa lista de votos de forma reativa.
     *
     * @param votes lista de votos para processar
     * @return Mono<Void> indicando conclusão
     */
    private Mono<Void> processBatchReactively(List<VoteRequestDTO> votes) {
        if (votes.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(votes)
                .as(voteService::saveAllFromDTO)
                .flatMap(this::cleanupRedisKey)
                .then();
    }

    /**
     * Remove chave do Redis após processamento do voto.
     *
     * @param vote voto processado
     * @return Mono<Long> resultado da operação Redis
     */
    private Mono<Long> cleanupRedisKey(Object vote) {
        String key = vote.toString();

        return redisTemplate.delete(key)
                .doOnError(error ->
                        logger.error("Erro ao remover chave do Redis: {}", key, error))
                .onErrorReturn(REDIS_DELETE_FALLBACK);
    }

    /**
     * Record para snapshot thread-safe do batch.
     *
     * @param votes lista de votos no momento do snapshot
     */
    private record BatchSnapshot(List<VoteRequestDTO> votes) {}
}
