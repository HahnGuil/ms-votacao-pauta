package br.com.hahn.msvalidadorcpf.domain.service;

import br.com.hahn.msvalidadorcpf.domain.dto.response.CpfValidationResponseDTO;
import br.com.hahn.msvalidadorcpf.domain.enums.CPFStatus;
import br.com.hahn.msvalidadorcpf.domain.exception.InvalidCpfException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfValidationServiceTest {

    private CpfValidationService service;
    private Random mockRandom;

    @BeforeEach
    void setUp() throws Exception {
        service = new CpfValidationService();
        mockRandom = Mockito.mock(Random.class);

        // Injetar mockRandom via reflection
        Field randomField = CpfValidationService.class.getDeclaredField("random");
        randomField.setAccessible(true);
        randomField.set(service, mockRandom);
    }

    @Test
    void validateCpf_shouldThrowInvalidCpfException_whenFormatIsInvalid() {
        String invalidCpf = "1234567890"; // 10 dígitos
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.0);

        Mono<CpfValidationResponseDTO> result = service.validateCpf(invalidCpf);

        assertThatThrownBy(result::block)
                .isInstanceOf(InvalidCpfException.class)
                .hasMessageContaining("Invalid CPF format");
    }

    @Test
    void validateCpf_shouldThrowInvalidCpfException_whenAllDigitsAreEqual() {
        String invalidCpf = "11111111111";
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.0);

        Mono<CpfValidationResponseDTO> result = service.validateCpf(invalidCpf);

        assertThatThrownBy(result::block)
                .isInstanceOf(InvalidCpfException.class)
                .hasMessageContaining("Invalid CPF format");
    }

    @Test
    void validateCpf_shouldThrowInvalidCpfException_whenRandomRejectsCpf() {
        String validCpf = "12345678901";
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.2); // < 0.3, rejeita
        Mono<CpfValidationResponseDTO> result = service.validateCpf(validCpf);

        assertThatThrownBy(result::block)
                .isInstanceOf(InvalidCpfException.class)
                .hasMessageContaining("Invalid CPF");
    }

    @Test
    void validateCpf_shouldReturnAbleToVote_whenRandomReturnsTrue() {
        String validCpf = "12345678901";
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.5); // > 0.3, aceita
        Mockito.when(mockRandom.nextBoolean()).thenReturn(true);

        Mono<CpfValidationResponseDTO> result = service.validateCpf(validCpf);
        CpfValidationResponseDTO dto = result.block();

        assertThat(dto).isNotNull();
        assertThat(dto.cpfStatus()).isEqualTo(CPFStatus.ABLE_TO_VOTE);
    }

    @Test
    void validateCpf_shouldReturnUnableToVote_whenRandomReturnsFalse() {
        String validCpf = "12345678901";
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.5); // > 0.3, aceita
        Mockito.when(mockRandom.nextBoolean()).thenReturn(false);

        Mono<CpfValidationResponseDTO> result = service.validateCpf(validCpf);
        CpfValidationResponseDTO dto = result.block();

        assertThat(dto).isNotNull();
        assertThat(dto.cpfStatus()).isEqualTo(CPFStatus.UNABLE_TO_VOTE);
    }

    @Test
    void isValidCpfFormat_shouldReturnFalse_whenCpfIsNull(){
        // Teste indireto via validateCpf
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.0);
        Mono<CpfValidationResponseDTO> result = service.validateCpf(null);

        assertThatThrownBy(result::block)
                .isInstanceOf(InvalidCpfException.class)
                .hasMessageContaining("Invalid CPF format");
    }

    @Test
    void maskCpf_shouldReturnMasked_whenCpfIsValid() throws Exception {
        // Teste indireto via validateCpf (verifica log, mas aqui só cobre o método)
        String masked = service.getClass()
                .getDeclaredMethod("maskCpf", String.class)
                .invoke(service, "12345678901")
                .toString();
        assertThat(masked).isEqualTo("123.***.***-01");
    }

    @Test
    void maskCpf_shouldReturnDefaultMask_whenCpfIsNullOrInvalid() throws Exception {
        String maskedNull = service.getClass()
                .getDeclaredMethod("maskCpf", String.class)
                .invoke(service, (Object) null)
                .toString();
        assertThat(maskedNull).isEqualTo("***.***.***-**");

        String maskedShort = service.getClass()
                .getDeclaredMethod("maskCpf", String.class)
                .invoke(service, "123")
                .toString();
        assertThat(maskedShort).isEqualTo("***.***.***-**");
    }
}

