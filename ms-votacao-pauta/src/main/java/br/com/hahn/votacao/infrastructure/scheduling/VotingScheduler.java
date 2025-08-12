package br.com.hahn.votacao.infrastructure.scheduling;

import br.com.hahn.votacao.domain.VotingClosedEvent;
import br.com.hahn.votacao.domain.model.Voting;
import br.com.hahn.votacao.domain.service.VotingService;
import br.com.hahn.votacao.infrastructure.service.VoteBatchConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Agendador responsável pelo gerenciamento automático do ciclo de vida das votações.
 * <p>
 * [Mantém a documentação existente...]
 */
@Component
public class VotingScheduler {

    private static final Logger votingSchedulerLogger = LoggerFactory.getLogger(VotingScheduler.class);
    private static final String CRON_EVERY_MINUTE = "0 * * * * *";
    private static final int DEFAULT_TOTAL_VOTES = 0;

    private final VotingService votingService;
    private final VoteBatchConsumer voteBatchConsumer;
    private final ApplicationEventPublisher eventPublisher;

    public VotingScheduler(VotingService votingService, VoteBatchConsumer voteBatchConsumer, ApplicationEventPublisher eventPublisher) {
        this.votingService = votingService;
        this.voteBatchConsumer = voteBatchConsumer;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Monitora e encerra automaticamente votações com prazo expirado.
     * <p>
     * [Mantém a documentação existente...]
     */
    @Scheduled(cron = CRON_EVERY_MINUTE)
    public void checkAndCloseExpiredVotings() {
        votingSchedulerLogger.info("Buscando votações com prazo expirado para encerrar.");

        findExpiredVotings()
                .flatMap(this::processExpiredVoting)
                .flatMap(this::closeVoting)
                .subscribe(
                        this::publishVotingClosedEvent,
                        this::handleProcessingError
                );
    }

    /**
     * Busca votações que expiraram e estão ativas.
     *
     * @return flux de votações expiradas
     */
    private Flux<Voting> findExpiredVotings() {
        return votingService.findAllVotings()
                .filter(this::isVotingExpired);
    }

    /**
     * Verifica se uma votação está expirada.
     *
     * @param voting votação a ser verificada
     * @return true se a votação está ativa e expirada
     */
    private boolean isVotingExpired(Voting voting) {
        return voting.isVotingSatus() && voting.getCloseVotingDate().isBefore(Instant.now());
    }

    /**
     * Processa votos pendentes antes do encerramento da votação.
     *
     * @param voting votação a ser processada
     * @return mono da votação após processamento
     */
    private Mono<Voting> processExpiredVoting(Voting voting) {
        votingSchedulerLogger.info("Votação {} expirou. Processando votos pendentes antes de encerrar.", voting.getVotingId());

        return voteBatchConsumer.forceFlushForVotingReactive(voting.getVotingId())
                .then(Mono.just(voting));
    }

    /**
     * Encerra a votação atualizando seu status e persistindo.
     *
     * @param voting votação a ser encerrada
     * @return mono da votação encerrada
     */
    private Mono<Voting> closeVoting(Voting voting) {
        voting.setVotingSatus(false);
        votingSchedulerLogger.info("Encerrando votação: {}", voting.getVotingId());
        return votingService.saveVoting(voting);
    }

    /**
     * Publica evento de votação encerrada.
     *
     * @param savedVoting votação que foi encerrada
     */
    private void publishVotingClosedEvent(Voting savedVoting) {
        votingSchedulerLogger.info("Votação {} encerrada com sucesso", savedVoting.getVotingId());

        VotingClosedEvent event = createVotingClosedEvent(savedVoting);
        eventPublisher.publishEvent(event);
    }

    /**
     * Cria evento de votação encerrada.
     *
     * @param voting votação encerrada
     * @return evento de votação encerrada
     */
    private VotingClosedEvent createVotingClosedEvent(Voting voting) {
        return new VotingClosedEvent(
                voting.getVotingId(),
                voting.getSubject(),
                Instant.now(),
                DEFAULT_TOTAL_VOTES
        );
    }

    /**
     * Trata erros durante processamento de votações expiradas.
     *
     * @param error erro ocorrido
     */
    private void handleProcessingError(Throwable error) {
        votingSchedulerLogger.error("Erro ao encerrar votação expirada: {}", error.getMessage(), error);
    }
}
