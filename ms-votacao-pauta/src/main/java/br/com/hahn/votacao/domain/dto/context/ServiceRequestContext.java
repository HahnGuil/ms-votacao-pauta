package br.com.hahn.votacao.domain.dto.context;

/**
 * Contexto de requisição para serviços, encapsulando informações
 * essenciais para processamento de operações com versionamento de API.
 *
 * @param resourceId identificador único do recurso sendo processado
 * @param apiVersion versão da API a ser utilizada na operação
 * @author HahnGuil
 * @since 1.0
 */
public record ServiceRequestContext(String resourceId, String apiVersion) {

    /**
     * Criação de instâncias do contexto de requisição.
     *
     * @param resourceId identificador único do recurso
     * @param apiVersion versão da API
     * @return nova instância de ServiceRequestContext
     */
    public static ServiceRequestContext of(String resourceId, String apiVersion) {
        return new ServiceRequestContext(resourceId, apiVersion);
    }
}
