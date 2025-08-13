package br.com.hahn.votacao.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de entrada para uma votação")
public record VotingRequestDTO(
        @Schema(description = "Pauta da votação", example = "Aprovação de aumento na divisão de lucros")
        String subject,
        @Schema(description = "Tempo limite da votação em minutos. Se for nulo ou <= 0, será ajustado para 1 minuto", example = "5")
        Integer userDefinedExpirationDate,
        @Schema(description = "Versão da API que está sendo consumida. É passado como PathVariable", example = "v1")
        String apiVersion) {

    public VotingRequestDTO withApiVersion(String apiVersion) {
        return new VotingRequestDTO(this.subject, this.userDefinedExpirationDate, apiVersion);
    }
}
