package br.com.hahn.votacao.infrastructure.event;

import br.com.hahn.votacao.domain.VotingClosedEvent;
import br.com.hahn.votacao.domain.service.ResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

/**
 * Manipulador de eventos de domínio relacionados ao ciclo de vida das votações.
 * <p>
 * Responsável por processar eventos críticos do sistema de votação de forma
 * assíncrona, garantindo que operações custosas (como cálculo de resultados)
 * não bloqueiem o fluxo principal da aplicação. Implementa padrão Event-Driven
 * Architecture para desacoplamento entre domínios.
 * <p>
 * EVENTOS TRATADOS:
 * - VotingClosedEvent: Acionado quando votação expira ou é fechada manualmente
 * <p>
 * PROCESSAMENTO ASSÍNCRONO:
 * - Execução via thread pool eventTaskExecutor (AsyncConfig)
 * - Publisher/Subscriber pattern com tratamento de erros robusto
 * - Logging detalhado para auditoria e troubleshooting
 * <p>
 * ARQUITETURA DE EVENTOS:
 * VotingService → publish(VotingClosedEvent) → VotingEventHandler → ResultService
 * <p>
 * INTEGRAÇÃO COM SCHEDULER:
 * - Eventos podem ser acionados por VotingScheduler (expiração automática)
 * - Também acionados por fechamento manual via API
 * - Thread pool compartilhado para todos os eventos assíncronos
 * <p>
 * MONITORAMENTO:
 * - Logs de início e fim de processamento
 * - Tracking de erros com context do evento
 * - Métricas de performance via thread naming
 *
 * @author HahnGuil
 * @since 1.0
 */
@Component
public class VotingEventHandler {

    private static final Logger votingEventHandlerLogger = LoggerFactory.getLogger(VotingEventHandler.class);

    private final ResultService resultService;

    /**
     * Construtor que injeta dependências necessárias para processamento de eventos.
     *
     * @param resultService serviço responsável por cálculo e persistência de resultados
     */
    public VotingEventHandler(ResultService resultService) {
        this.resultService = resultService;
    }

    /**
     * Processa evento de fechamento de votação para cálculo automático de resultado.
     * <p>
     * Acionado automaticamente quando uma votação é encerrada, seja por
     * expiração de tempo ou fechamento manual. Executa cálculo de resultado de
     * forma assíncrona para não bloquear operações críticas do sistema.
     * <p>
     * FLUXO DE PROCESSAMENTO:
     * 1. Recebe evento com ID da votação encerrada
     * 2. Aciona ResultService para cálculo reativo do resultado
     * 3. Executa em thread pool dedicado (eventTaskExecutor)
     * 4. Processa resultado ou erro de forma assíncrona
     * 5. Registra logs detalhados para auditoria
     * <p>
     * EXECUÇÃO ASSÍNCRONA:
     * - Thread Pool: eventTaskExecutor configurado em AsyncConfig
     * - Scheduler: Schedulers.boundedElastic() para operações I/O
     * - Non-blocking: Não bloqueia thread que publicou o evento
     * <p>
     * TRATAMENTO DE ERROS:
     * - Captura erros durante cálculo de resultado
     * - Logs detalhados com context do evento falho
     * - Não propaga exceções para não quebrar publisher
     * - Mantém sistema resiliente a falhas pontuais
     * <p>
     * CENÁRIOS DE ACIONAMENTO:
     * - Scheduler automático: Votação expira por timeout
     * - API manual: Administrador fecha votação via endpoint
     * - Batch processing: Processamento em lote de votações expiradas
     * <p>
     * LOGGING E AUDITORIA:
     * - Log de recebimento do evento com votingId
     * - Log de sucesso com resultado calculado
     * - Log de erro com stack trace para debugging
     * - Thread naming para identificação em monitoring
     * <p>
     * PERFORMANCE:
     * - Processamento reativo via Project Reactor
     * - Thread pool dimensionado para workload esperado
     * - Sem bloqueio de threads principais da aplicação
     *
     * @param event evento contendo ID da votação que foi encerrada
     */
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
