package br.com.hahn.votacao.domain.enums;

public enum VoteOption {
    SIM, NAO;

    public static VoteOption fromString(String value) {
        for (VoteOption option : VoteOption.values()) {
            if (option.name().equalsIgnoreCase(value)) {
                return option;
            }
        }
        throw new IllegalArgumentException("Invalid vote option: " + value);
    }
}
