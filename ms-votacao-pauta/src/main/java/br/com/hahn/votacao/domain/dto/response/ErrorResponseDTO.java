package br.com.hahn.votacao.domain.dto.response;

import java.time.Instant;

public record ErrorResponseDTO(String message, Instant timestamp) {
}
