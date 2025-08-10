package br.com.hahn.msvalidadorcpf.api.exception;

import br.com.hahn.msvalidadorcpf.domain.exception.InvalidCpfException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleInvalidCpfException_shouldReturnNotFoundAndErrorMessage() {
        InvalidCpfException ex = new InvalidCpfException("CPF não existe");
        Mono<ResponseEntity<Map<String, String>>> resultMono = handler.handleInvalidCpfException(ex);
        ResponseEntity<Map<String, String>> response = resultMono.block();

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).containsEntry("error", "CPF inválido");
        assertThat(response.getBody()).containsEntry("message", "CPF não existe");
    }

    @Test
    void handleValidationException_shouldReturnBadRequestAndFieldError() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("cpfValidationRequestDTO", "cpf", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        WebExchangeBindException ex = mock(WebExchangeBindException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        Mono<ResponseEntity<Map<String, String>>> resultMono = handler.handleValidationException(ex);
        ResponseEntity<Map<String, String>> response = resultMono.block();

        Assertions.assertNotNull(response);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "Dados inválidos");
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().get("message")).contains("cpf: must not be blank");
    }

    @Test
    void handleValidationException_shouldReturnBadRequestAndDefaultMessageIfNoFieldError() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        WebExchangeBindException ex = mock(WebExchangeBindException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        Mono<ResponseEntity<Map<String, String>>> resultMono = handler.handleValidationException(ex);
        ResponseEntity<Map<String, String>> response = resultMono.block();

        Assertions.assertNotNull(response);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "Dados inválidos");
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody()).containsEntry("message", "Erro de validação");
    }
}

