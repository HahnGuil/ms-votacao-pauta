package br.com.hahn.votacao.infrastructure.scheduling;

import br.com.hahn.votacao.infrastructure.service.VoteBatchConsumer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VoteBatchScheduler {

    private final VoteBatchConsumer voteBatchConsumer;

    public VoteBatchScheduler(VoteBatchConsumer voteBatchConsumer) {
        this.voteBatchConsumer = voteBatchConsumer;
    }

    @Scheduled(fixedRate = 30000)
    public void scheduledFlush() {
        voteBatchConsumer.scheduledFlush();
    }
}
