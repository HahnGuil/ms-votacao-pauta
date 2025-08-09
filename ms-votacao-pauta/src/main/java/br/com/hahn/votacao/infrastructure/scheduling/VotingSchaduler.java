package br.com.hahn.votacao.infrastructure.scheduling;

import br.com.hahn.votacao.domain.service.VotingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class VotingSchaduler {

    private static final Logger logger = LoggerFactory.getLogger(VotingSchaduler.class);

    private final VotingService votingService;

    public VotingSchaduler(VotingService votingService) {
        this.votingService = votingService;
    }

    @Scheduled(cron = "0 * * * * *") // every minute
    public void checkAndCloseExpiredVotings() {
        logger.info("Iniciando varredura de votações para encerrar");
        votingService.findAllVotings()
                .filter(voting -> voting.isVotingSatus() && voting.getCloseVotingDate().isBefore(Instant.now()))
                .flatMap(voting -> {
                    voting.setVotingSatus(false);
                    return votingService.saveVoting(voting);
                })
                .subscribe();
    }


}
