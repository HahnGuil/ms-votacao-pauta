package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.api.controller.base.BaseController;
import br.com.hahn.votacao.domain.dto.request.VotingRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VotingResponseDTO;
import br.com.hahn.votacao.domain.service.VotingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/voting")
public class VotingController extends BaseController {

    private final VotingService votingService;

    public VotingController(VotingService votingService) {
        this.votingService = votingService;
    }

    @PostMapping("/{version}/create-voting")
    public Mono<ResponseEntity<VotingResponseDTO>> createVoting(
            @PathVariable String version,
            @RequestBody VotingRequestDTO votingRequestDTO){

        return votingService.createVoting(
                votingRequestDTO.withApiVersion(determineApiVersion(version))
        ).map(votingResponseDTO -> ResponseEntity.status(HttpStatus.CREATED).body(votingResponseDTO));
    }

}



