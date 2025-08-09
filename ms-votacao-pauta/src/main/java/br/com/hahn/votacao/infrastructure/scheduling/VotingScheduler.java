package br.com.hahn.votacao.infrastructure.scheduling;

import br.com.hahn.votacao.domain.service.VotingService;
import br.com.hahn.votacao.infrastructure.service.VoteBatchConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class VotingScheduler {

    private static final Logger votingSchedulerLogger = LoggerFactory.getLogger(VotingScheduler.class);

    private final VotingService votingService;
    private final VoteBatchConsumer voteBatchConsumer;

    public VotingScheduler(VotingService votingService, VoteBatchConsumer voteBatchConsumer) {
        this.votingService = votingService;
        this.voteBatchConsumer = voteBatchConsumer;
    }

    @Scheduled(cron = "0 * * * * *")
    public void checkAndCloseExpiredVotings() {
        votingSchedulerLogger.info("Buscando votações com prazo expirado para encerrar.");

        votingService.findAllVotings()
                .filter(voting -> voting.isVotingSatus() && voting.getCloseVotingDate().isBefore(Instant.now()))
                .flatMap(voting -> {
                    votingSchedulerLogger.info("Votação {} expirou. Processando votos pendentes antes de encerrar.", voting.getVotingId());

                    return voteBatchConsumer.forceFlushForVotingReactive(voting.getVotingId())
                            .then(Mono.just(voting));
                })
                .flatMap(voting -> {
                    voting.setVotingSatus(false);
                    votingSchedulerLogger.info("Encerrando votação: {}", voting.getVotingId());
                    return votingService.saveVoting(voting);
                })
                .subscribe(
                        savedVoting -> votingSchedulerLogger.info("Votação {} encerrada com sucesso", savedVoting.getVotingId()),
                        error -> votingSchedulerLogger.error("Erro ao encerrar votação", error)
                );
    }


}
