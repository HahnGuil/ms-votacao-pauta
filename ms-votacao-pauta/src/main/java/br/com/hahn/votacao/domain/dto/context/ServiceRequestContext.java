package br.com.hahn.votacao.domain.dto.context;

public record ServiceRequestContext(String resourceId, String apiVersion) {

    public static ServiceRequestContext of(String resourceId, String apiVersion) {
        return new ServiceRequestContext(resourceId, apiVersion);
    }
}
