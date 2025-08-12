package br.com.hahn.votacao.domain.dto.response;

import br.com.hahn.votacao.domain.enums.CpfStatus;

/**
 * Resposta da validação de CPF junto a serviços externos.
 *
 * Encapsula o resultado da verificação de elegibilidade do CPF
 * para participação no sistema de votação.
 *
 * @param status resultado da validação (ABLE_TO_VOTE/UNABLE_TO_VOTE)
 * @author HahnGuil
 * @since 1.0
 */
public record CpfValidationResponseDTO(CpfStatus status) {
}
