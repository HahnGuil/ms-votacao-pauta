package br.com.hahn.votacao.domain.enums;

import java.util.Optional;

/**
 * Resposta da criação de nova votação com informações de acesso.
 *
 * Contém URLs geradas dinamicamente e metadados da sessão
 * de votação criada para facilitar integração do cliente.
 *
 * @author HahnGuil
 * @since 1.0
 */
public enum VotingResult {
    /** Proposta foi aprovada pela maioria dos votos */
    APROVADO,
    /** Proposta foi rejeitada pela maioria dos votos */
    REPROVADO;

    /**
     * Converte string para VotingResult de forma segura.
     *
     * @param value string a ser convertida
     * @return Optional contendo o resultado ou vazio se inválido
     */
    public static Optional<VotingResult> fromString(String value) {
        if (value == null) {
            return Optional.empty();
        }

        for (VotingResult option : VotingResult.values()) {
            if (option.name().equalsIgnoreCase(value)) {
                return Optional.of(option);
            }
        }
        return Optional.empty();
    }
}