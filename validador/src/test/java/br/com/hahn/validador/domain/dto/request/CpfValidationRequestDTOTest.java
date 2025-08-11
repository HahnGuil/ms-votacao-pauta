package br.com.hahn.validador.domain.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

    @Test
    void shouldFailValidation_whenCpfHasInvalidLengthOrChars() {
        CpfValidationRequestDTO shortDto = new CpfValidationRequestDTO("1234567890");
        Set<ConstraintViolation<CpfValidationRequestDTO>> shortViolations = validator.validate(shortDto);
        assertThat(shortViolations).isNotEmpty();
        assertThat(shortViolations.iterator().next().getMessage()).contains("CPF deve conter exatamente 11 dígitos");

        CpfValidationRequestDTO longDto = new CpfValidationRequestDTO("123456789012");
        Set<ConstraintViolation<CpfValidationRequestDTO>> longViolations = validator.validate(longDto);
        assertThat(longViolations).isNotEmpty();
        assertThat(longViolations.iterator().next().getMessage()).contains("CPF deve conter exatamente 11 dígitos");

        CpfValidationRequestDTO alphaDto = new CpfValidationRequestDTO("abcdefghijk");
        Set<ConstraintViolation<CpfValidationRequestDTO>> alphaViolations = validator.validate(alphaDto);
        assertThat(alphaViolations).isNotEmpty();
        assertThat(alphaViolations.iterator().next().getMessage()).contains("CPF deve conter exatamente 11 dígitos");
    }
}

