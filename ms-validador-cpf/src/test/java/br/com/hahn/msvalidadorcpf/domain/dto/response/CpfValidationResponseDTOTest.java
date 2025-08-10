package br.com.hahn.msvalidadorcpf.domain.dto.response;

import br.com.hahn.msvalidadorcpf.domain.enums.CPFStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfValidationResponseDTOTest {

    @Test
    void shouldReturnAbleToVoteStatus() {
        CpfValidationResponseDTO dto = new CpfValidationResponseDTO(CPFStatus.ABLE_TO_VOTE);
        assertThat(dto.cpfStatus()).isEqualTo(CPFStatus.ABLE_TO_VOTE);
    }

    @Test
    void shouldReturnUnableToVoteStatus() {
        CpfValidationResponseDTO dto = new CpfValidationResponseDTO(CPFStatus.UNABLE_TO_VOTE);
        assertThat(dto.cpfStatus()).isEqualTo(CPFStatus.UNABLE_TO_VOTE);
    }
}

