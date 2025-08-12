package br.com.hahn.votacao.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuração base do WebClient para clientes HTTP reativos.
 * <p>
 * Fornece builder padrão do WebClient para injeção em componentes
 * que fazem integrações HTTP (ex: CpfValidationClient).
 *
 * @author HahnGuil
 * @since 1.0
 */
@Configuration
public class WebClientConfig {

    /**
     * Bean do builder WebClient para criação de clientes HTTP reativos.
     *
     * @return builder configurado para WebClient
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
