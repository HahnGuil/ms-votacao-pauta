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
    public Mono<ResultResponseDTO> getResult(@PathVariable String votingId) {
        return resultService.getResult(votingId);
    }

    @GetMapping("/{votingId}/exists")
    public Mono<ResponseEntity<Boolean>> resultExists(@PathVariable String votingId) {
        return resultService.isResultAvailable(votingId).map(ResponseEntity::ok);
    }


}
