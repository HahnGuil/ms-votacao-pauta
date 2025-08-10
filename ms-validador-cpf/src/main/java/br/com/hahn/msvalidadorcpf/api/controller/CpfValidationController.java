package br.com.hahn.msvalidadorcpf.api.controller;


import br.com.hahn.msvalidadorcpf.domain.dto.request.CpfValidationRequestDTO;
import br.com.hahn.msvalidadorcpf.domain.dto.response.CpfValidationResponseDTO;
import br.com.hahn.msvalidadorcpf.domain.service.CpfValidationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/cpf")
public class CpfValidationController {

    private final CpfValidationService cpfValidationService;

    public CpfValidationController(CpfValidationService cpfValidationService) {
        this.cpfValidationService = cpfValidationService;
    }

    @PostMapping("/validate")
    @ResponseStatus(HttpStatus.OK)
    public Mono<CpfValidationResponseDTO> validateCpf(@Valid @RequestBody CpfValidationRequestDTO request) {
        return cpfValidationService.validateCpf(request.cpf());
    }
}
