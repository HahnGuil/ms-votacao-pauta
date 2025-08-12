package br.com.hahn.votacao.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resposta de erro padrão")
public record ErrorResponseDTO(
        @Schema(description = "Mensagem de erro", example = "User has already voted")
        String message,

        @Schema(description = "Timestamp do erro", example = "2024-01-15T10:30:00Z")
        Instant timestamp
) {
}
