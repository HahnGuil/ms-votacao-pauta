package br.com.hahn.votacao.domain.dto.response;

import br.com.hahn.votacao.domain.enums.CpfStatus;

public record CpfValidationResponseDTO(CpfStatus status) {
}
