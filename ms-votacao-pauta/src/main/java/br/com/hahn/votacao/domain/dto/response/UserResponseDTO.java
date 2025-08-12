package br.com.hahn.votacao.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de Sucesso do usuário")
public record UserResponseDTO(String userId, String userCPF) {
}
