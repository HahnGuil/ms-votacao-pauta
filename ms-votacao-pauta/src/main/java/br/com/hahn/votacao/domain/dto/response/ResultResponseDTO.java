package br.com.hahn.votacao.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta contendo resultado consolidado de uma votação encerrada.
 *
 * Fornece dados finais processados após o término do período
 * de votação, incluindo totalização e decisão final.
 *
 * @author HahnGuil
 * @since 1.0
 */
@Schema(description = "Dados de entrada para um Result")
public record ResultResponseDTO(
        @Schema(description = "Id da votação que quer consultar o resultado", example = "689a7b088d19273ee6070d52")
        String votingId,
        @Schema(description = "Pauta da votação que foi realizada", example = "Aumento no incentivo ao marketing esportivo")
        String votingSubject,
        @Schema(description = "Total de votos válidos recebidos na votação", example = "300")
        Integer totalVotes,
        @Schema(description = "Resultado final se a pauta foi aprovada ou não", example = "APROVADO ou REPROVADO")
        String votingResult) {
}