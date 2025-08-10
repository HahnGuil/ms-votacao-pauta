package br.com.hahn.votacao.domain.enums;

import java.util.Optional;

public enum VotingResult {

    APROVADO, REPROVADO;

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