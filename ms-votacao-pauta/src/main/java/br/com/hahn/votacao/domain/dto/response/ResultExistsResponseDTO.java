package br.com.hahn.votacao.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta contendo um booleano para avisar do fim da votação.
 * <p>
 * mostra se a votação terminou ou não
 *
 * @author HahnGuil
 * @since 1.0
 */
@Schema(description = "Boolean de saida para votação finalizada")
public record ResultExistsResponseDTO(
        @Schema(description = "Informa se a votação esta pronta ou não", example = "true ou false")
        boolean exists) {
}
