package br.com.hahn.votacao.domain.enums;

/**
 * Status de elegibilidade de um CPF para participar de votações.
 *
 * Resultado da validação junto a serviços externos que verificam
 * se o CPF está apto a exercer o direito de voto.
 */
public enum CpfStatus {
    /** CPF válido e habilitado para votar */
    ABLE_TO_VOTE,
    /** CPF inválido ou impedido de votar */
    UNABLE_TO_VOTE
}
