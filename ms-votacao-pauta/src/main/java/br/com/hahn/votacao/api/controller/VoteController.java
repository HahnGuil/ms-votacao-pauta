package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VoteResponseDTO;
import br.com.hahn.votacao.domain.service.VoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/vote")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/{votingId}")
    public Mono<ResponseEntity<VoteResponseDTO>> vote(@PathVariable String votingId, @RequestBody VoteRequestDTO voteRequestDTO) {
        VoteRequestDTO vote = new VoteRequestDTO(votingId, voteRequestDTO.userId(), voteRequestDTO.voteOption());
        return voteService.sendVoteToQueue(vote)
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .body(new VoteResponseDTO("Voto recebido com sucesso")));
    }
}
