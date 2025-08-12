package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.api.controller.base.BaseController;
import br.com.hahn.votacao.domain.dto.context.ServiceRequestContext;
import br.com.hahn.votacao.domain.dto.response.ResultResponseDTO;
import br.com.hahn.votacao.domain.service.ResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller responsavel por gerencia as operações relacionadas ao resultado das votações,
 * inclui consulta de resultado e verificação de disponibilidade
 *
 * @author HahnGuil
 * @since 1.0
 */

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

    /**
     * Verifica se o resultado da votação já esta disponível para consulta
     *
     * @param version versão da API a ser utilizada, current ou legacy
     * @param votingId id identificador da votação recebido após criar uma votação
     * @return Mono contendo um boolean indicando se o resultado existe ou não.
     */
    @GetMapping("/{version}/{votingId}/exists")
    public Mono<ResponseEntity<Boolean>> resultExists(@PathVariable String version, @PathVariable String votingId) {
        ServiceRequestContext context = ServiceRequestContext.of(votingId, version);
        return resultService.isResultAvailable(context).map(ResponseEntity::ok);
    }


}
