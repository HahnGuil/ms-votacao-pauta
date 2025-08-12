package br.com.hahn.votacao.infrastructure.scheduling;

import br.com.hahn.votacao.infrastructure.service.VoteBatchConsumer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agendador responsável pelo flush periódico do buffer de votos.
 * </p>
 * Executa processamento em lote a cada 30 segundos para garantir que votos
 * não fiquem indefinidamente no buffer, complementando o flush automático
 * baseado em tamanho de lote.
 *
 * @author HahnGuil
 * @since 1.0
 */
@Component
public class VoteBatchScheduler {

    private static final long FLUSH_INTERVAL_MS = 30_000L;

    private final VoteBatchConsumer voteBatchConsumer;

    /**
     * Construtor que injeta o consumidor de lotes.
     *
     * @param voteBatchConsumer serviço responsável pelo processamento em lote
     */
    public VoteBatchScheduler(VoteBatchConsumer voteBatchConsumer) {
        this.voteBatchConsumer = voteBatchConsumer;
    }

    /**
     * Executa flush periódico do buffer de votos a cada 30 segundos.
     * </p>
     * Estratégia híbrida que combina flush por tempo + volume para otimizar
     * latência e throughput. Garante SLA máximo de 30 segundos para processamento.
     */
    @Scheduled(fixedRate = FLUSH_INTERVAL_MS)
    public void scheduledFlush() {
        voteBatchConsumer.scheduledFlush();
    }
}
