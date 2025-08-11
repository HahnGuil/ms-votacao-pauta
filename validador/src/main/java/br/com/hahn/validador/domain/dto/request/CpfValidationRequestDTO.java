package br.com.hahn.validador.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CpfValidationRequestDTO(
        @NotBlank(message = "CPF não pode ser vazio")
        @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos")
        String cpf
){
}
