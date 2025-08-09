package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.domain.dto.request.VotingRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VotingResponseDTO;
import br.com.hahn.votacao.domain.service.VotingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/voting")
public class VotingController {

    private final VotingService votingService;

    public VotingController(VotingService votingService) {
        this.votingService = votingService;
    }

    @PostMapping("/create-voting")
    public Mono<ResponseEntity<VotingResponseDTO>> createVoting(@RequestBody VotingRequestDTO votingRequestDTO){
        return votingService.createVoting(votingRequestDTO)
                .map(votingResponseDTO -> ResponseEntity.status(HttpStatus.CREATED).body(votingResponseDTO));
    }

}
