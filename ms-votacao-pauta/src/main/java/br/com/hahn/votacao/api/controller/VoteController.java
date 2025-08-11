package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.api.controller.base.BaseController;
import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VoteResponseDTO;
import br.com.hahn.votacao.domain.service.VoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/vote")
public class VoteController extends BaseController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/{version}/{votingId}")
    public Mono<ResponseEntity<VoteResponseDTO>> vote(
            @PathVariable String version,
            @PathVariable String votingId, @RequestBody VoteRequestDTO voteRequestDTO) {
        VoteRequestDTO vote = new VoteRequestDTO(votingId, voteRequestDTO.userId(), voteRequestDTO.voteOption(), determineApiVersion(version));
        return voteService.sendVoteToQueue(vote)
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .body(new VoteResponseDTO("Voto recebido com sucesso")));
    }
}

