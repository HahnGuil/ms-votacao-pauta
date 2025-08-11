package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.api.controller.base.BaseController;
import br.com.hahn.votacao.domain.dto.context.ServiceRequestContext;
import br.com.hahn.votacao.domain.dto.response.ResultResponseDTO;
import br.com.hahn.votacao.domain.service.ResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/result")
public class ResultController extends BaseController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping("/{version}/{votingId}")
    public Mono<ResultResponseDTO> getResult(@PathVariable String version, @PathVariable String votingId) {
        ServiceRequestContext context = ServiceRequestContext.of(votingId, version);
        return resultService.getResult(context);
    }

    @GetMapping("/{version}/{votingId}/exists")
    public Mono<ResponseEntity<Boolean>> resultExists(@PathVariable String version, @PathVariable String votingId) {
        ServiceRequestContext context = ServiceRequestContext.of(votingId, version);
        return resultService.isResultAvailable(context).map(ResponseEntity::ok);
    }


}
