package br.com.hahn.votacao.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de sucesso para votação")
public record VoteResponseDTO(
        @Schema(description = "Mensagem de confirmação", example = "Voto recebido com sucesso")
        String message
) {
}
