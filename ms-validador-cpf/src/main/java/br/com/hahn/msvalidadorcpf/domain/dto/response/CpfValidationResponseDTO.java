package br.com.hahn.msvalidadorcpf.domain.dto.response;

import br.com.hahn.msvalidadorcpf.domain.enums.CPFStatus;

public record CpfValidationResponseDTO(CPFStatus cpfStatus) {
}
