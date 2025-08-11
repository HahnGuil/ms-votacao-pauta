package br.com.hahn.validador.domain.dto.response;


import br.com.hahn.validador.domain.enums.CPFStatus;

public record CpfValidationResponseDTO(CPFStatus cpfStatus) {
}
