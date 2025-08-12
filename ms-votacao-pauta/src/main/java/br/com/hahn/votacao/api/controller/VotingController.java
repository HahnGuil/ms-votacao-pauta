package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.api.controller.base.BaseController;
import br.com.hahn.votacao.domain.dto.request.VotingRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VotingResponseDTO;
import br.com.hahn.votacao.domain.service.VotingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller responsavel pelas operações relacionadas a votação
 *
 * @author HahnGuil
 * @since 1.0
 */
@RestController
@RequestMapping("/voting")
public class VotingController extends BaseController {

    private final VotingService votingService;

    public VotingController(VotingService votingService) {
        this.votingService = votingService;
    }

    /**
     * Realiza a criação de uma votação para uma pauta
     *
     * @param version versão da API recebida no parâmetro, é colocada no votingRequestDTO para o envio para o service
     * @param votingRequestDTO Dados para criação da votação, contendo o assunto e o tempo de expiração da votação
     * @return votingResponseDTO contendo o votindId, a url para receber os votos, o tempo de expiração da votação e a url para consultar o resultado
     */
    @PostMapping("/{version}/create-voting")
    public Mono<ResponseEntity<VotingResponseDTO>> createVoting(
            @PathVariable String version,
            @RequestBody VotingRequestDTO votingRequestDTO){

        return votingService.createVoting(
                votingRequestDTO.withApiVersion(determineApiVersion(version))
        ).map(votingResponseDTO -> ResponseEntity.status(HttpStatus.CREATED).body(votingResponseDTO));
    }

}



