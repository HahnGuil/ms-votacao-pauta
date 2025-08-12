package br.com.hahn.votacao.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuração de processamento assíncrono para eventos de domínio.
 * <p>
 * Define pool otimizado para eventos de votação com 2-5 threads
 * e capacidade de fila de 100 tarefas. Usado principalmente para
 * VotingClosedEvent e notificações que não devem bloquear execução.
 *
 * @author HahnGuil
 * @since 1.0
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 5;
    private static final int QUEUE_CAPACITY = 100;
    private static final String THREAD_PREFIX = "VotingEvent-";

    /**
     * Pool de threads para processamento assíncrono de eventos.
     * <p>
     * Configuração: 2-5 threads, fila de 100 tarefas.
     */
    @Bean(name = "eventTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(THREAD_PREFIX);
        executor.initialize();
        return executor;
    }
}