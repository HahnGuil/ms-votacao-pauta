package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.domain.dto.response.ResultResponseDTO;
import br.com.hahn.votacao.domain.service.ResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/result")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping("/{votingId}")
    public Mono<ResponseEntity<ResultResponseDTO>> getResult(@PathVariable String votingId) {
        return resultService.getResult(votingId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @GetMapping("/{votingId}/exists")
    public Mono<ResponseEntity<Boolean>> resultExists(@PathVariable String votingId) {
        return resultService.getResult(votingId)
                .map(result -> ResponseEntity.ok(true))
                .onErrorReturn(ResponseEntity.ok(false));
    }


}
