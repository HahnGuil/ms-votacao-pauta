package br.com.hahn.votacao.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta da criação de nova votação com informações de acesso.
 * <p>
 * Contém URLs geradas dinamicamente e metadados da sessão
 * de votação criada para facilitar integração do cliente.
 *
 * @author HahnGuil
 * @since 1.0
 */
public record VotingResponseDTO(
        @Schema(description = "id da votação que foi criada", example = "6898ff38e855e877c1127394")
        String votingId,
        @Schema(description = "Url da votação que foi criada", example = "http://localhost:2500/api/votacao/v1/vote/6899504c8175afcbc9a5b0f3")
        String voteUrl,
        @Schema(description = "Hora que a votação criada será fechada", example = "11/08/2025 23:25")
        String closeVotingDate,
        @Schema(description = "URL para verificar o resultado quando a votação acabar", example = "http://localhost:2500/api/votacao/v1/result/689a7b088d19273ee6070d52")
        String resultUrl) {
}
