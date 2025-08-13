package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.api.controller.base.BaseController;
import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VoteResponseDTO;
import br.com.hahn.votacao.domain.service.VoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller responsavel por gerenciar as operações de Votos
 *
 * @author HahnGuil
 * @since 1.0
 */
@RestController
@RequestMapping("/vote")
public class VoteController extends BaseController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    /**
     * Realiza o voto do usuário na votação informada
     * <p>
     * Coloca a versão da API recebida por parametro no DTO para o serviço
     *
     * @param version versão da API que esta sendo utilizada
     * @param votingId id da votação que seta sendo votada a pauta
     * @param voteRequestDTO DTO que passa os dados do usuário para o service, com os camopos votingID, userId, voteOptin, apiVersion
     * @return retorna uma mensagem em String confirmando o voto do usuário com um código 201
     */
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

