package br.com.hahn.votacao.infrastructure.event;

import br.com.hahn.votacao.domain.VotingClosedEvent;
import br.com.hahn.votacao.domain.service.ResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

@Component
public class VotingEventHandler {

    private static final Logger votingEventHandlerLogger = LoggerFactory.getLogger(VotingEventHandler.class);

    private final ResultService resultService;

    public VotingEventHandler(ResultService resultService) {
        this.resultService = resultService;
    }

    @EventListener
    @Async("eventTaskExecutor")
    public void handleVotingClosed(VotingClosedEvent event) {
        votingEventHandlerLogger.info("Evento de votação encerrada recebido para votingId: {}", event.votingId());

        resultService.createResult(event.votingId())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        result -> votingEventHandlerLogger.info("Resultado calculado automaticamente para votação {}: {}",
                                event.votingId(), result),
                        error -> votingEventHandlerLogger.error("Erro ao calcular resultado automaticamente para votação {}",
                                event.votingId(), error)
                );
    }
}
