package br.com.hahn.votacao.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de entrada para um usuário")
public record UserRequestDTO(
        @Schema(description = "Nome do usuário", example = "João da Silva")
        String userName,
        @Schema(description = "CPF do usuário", example = "46997112355")
        String userCPF,
        @Schema(description = "Versão da API que está sendo consumida. É passado como PathVariable", example = "v1")
        String apiVersion) {
}
