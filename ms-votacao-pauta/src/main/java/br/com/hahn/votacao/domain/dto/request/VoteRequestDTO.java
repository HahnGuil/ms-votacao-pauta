package br.com.hahn.votacao.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para registrar um voto")
public record VoteRequestDTO(
        @Schema(description = "Identificador da votação", example = "12345")
        String votingId,

        @Schema(description = "Identificador do usuário", example = "67890")
        String userId,

        @Schema(description = "Opção de voto (SIM ou NÃO)", example = "SIM")
        String voteOption,

        @Schema(description = "Versão da API utilizada", example = "1.0")
        String apiVersion
) {
}
