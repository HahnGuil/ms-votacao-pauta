package br.com.hahn.votacao.api.controller.base;


import org.springframework.beans.factory.annotation.Value;

public abstract class BaseController {

    @Value("${api.current.version}")
    private String apiCurrentVersion;

    @Value("${api.legacy.version}")
    private String apiLegacyVersion;

    @Value("${api.legacy.enabled}")
    private boolean apiLegacyEnabled;

    protected String determineApiVersion(String pathVersion) {
        // Validar se é a versão atual
        if (apiCurrentVersion.equals(pathVersion)) {
            return pathVersion;
        }

        // Validar se é versão legacy e se está habilitada
        if (apiLegacyEnabled && apiLegacyVersion.equals(pathVersion)) {
            return pathVersion;
        }

        // Retorna versão atual como padrão
        return apiCurrentVersion;
    }
}
