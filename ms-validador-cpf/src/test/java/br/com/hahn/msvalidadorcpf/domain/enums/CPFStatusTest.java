package br.com.hahn.msvalidadorcpf.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CPFStatusTest {

    @Test
    void shouldContainAbleToVote() {
        assertThat(CPFStatus.valueOf("ABLE_TO_VOTE")).isEqualTo(CPFStatus.ABLE_TO_VOTE);
    }

    @Test
    void shouldContainUnableToVote() {
        assertThat(CPFStatus.valueOf("UNABLE_TO_VOTE")).isEqualTo(CPFStatus.UNABLE_TO_VOTE);
    }

    @Test
    void shouldListAllValues() {
        assertThat(CPFStatus.values()).containsExactlyInAnyOrder(CPFStatus.ABLE_TO_VOTE, CPFStatus.UNABLE_TO_VOTE);
    }
}

