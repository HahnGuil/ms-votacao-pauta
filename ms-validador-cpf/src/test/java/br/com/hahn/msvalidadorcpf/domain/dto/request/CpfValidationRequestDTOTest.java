package br.com.hahn.msvalidadorcpf.domain.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CpfValidationRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldPassValidation_whenCpfIsValid() {
        CpfValidationRequestDTO dto = new CpfValidationRequestDTO("12345678901");
        Set<ConstraintViolation<CpfValidationRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "'', 'CPF não pode ser vazio'",
            ", 'CPF não pode ser vazio'",
            "'1234567890', 'CPF deve conter exatamente 11 dígitos'",
            "'123456789012', 'CPF deve conter exatamente 11 dígitos'",
            "'abcdefghijk', 'CPF deve conter exatamente 11 dígitos'"
    }, nullValues = {"null"})
    void shouldFailValidation_whenCpfIsInvalid(String cpf, String expectedMessage) {
        CpfValidationRequestDTO dto = new CpfValidationRequestDTO(cpf);
        Set<ConstraintViolation<CpfValidationRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains(expectedMessage);
    }
}

