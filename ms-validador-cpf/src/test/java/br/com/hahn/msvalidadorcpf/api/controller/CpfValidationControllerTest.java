package br.com.hahn.msvalidadorcpf.api.controller;

import br.com.hahn.msvalidadorcpf.domain.dto.request.CpfValidationRequestDTO;
import br.com.hahn.msvalidadorcpf.domain.dto.response.CpfValidationResponseDTO;
import br.com.hahn.msvalidadorcpf.domain.enums.CPFStatus;
import br.com.hahn.msvalidadorcpf.domain.service.CpfValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

class CpfValidationControllerTest {

    private CpfValidationService cpfValidationService;
    private CpfValidationController controller;

    @BeforeEach
    void setUp() {
        cpfValidationService = Mockito.mock(CpfValidationService.class);
        controller = new CpfValidationController(cpfValidationService);
    }

    @Test
    void validateCpf_shouldReturnResponseDTO_whenCpfIsValid() {
        String cpf = "12345678901";
        CpfValidationRequestDTO request = new CpfValidationRequestDTO(cpf);
        CpfValidationResponseDTO responseDTO = new CpfValidationResponseDTO(CPFStatus.ABLE_TO_VOTE);

        Mockito.when(cpfValidationService.validateCpf(anyString()))
                .thenReturn(Mono.just(responseDTO));

        Mono<CpfValidationResponseDTO> resultMono = controller.validateCpf(request);
        CpfValidationResponseDTO result = resultMono.block();

        assertThat(result).isNotNull();
        assertThat(result.cpfStatus()).isEqualTo(CPFStatus.ABLE_TO_VOTE);
    }

    @Test
    void validateCpf_shouldReturnResponseDTO_whenCpfIsInvalid() {
        String cpf = "00000000000";
        CpfValidationRequestDTO request = new CpfValidationRequestDTO(cpf);
        CpfValidationResponseDTO responseDTO = new CpfValidationResponseDTO(CPFStatus.UNABLE_TO_VOTE);

        Mockito.when(cpfValidationService.validateCpf(anyString()))
                .thenReturn(Mono.just(responseDTO));

        Mono<CpfValidationResponseDTO> resultMono = controller.validateCpf(request);
        CpfValidationResponseDTO result = resultMono.block();

        assertThat(result).isNotNull();
        assertThat(result.cpfStatus()).isEqualTo(CPFStatus.UNABLE_TO_VOTE);
    }
}

