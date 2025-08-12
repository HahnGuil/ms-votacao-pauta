package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.ResultCreateDTO;
import br.com.hahn.votacao.domain.dto.context.ServiceRequestContext;
import br.com.hahn.votacao.domain.dto.response.ResultResponseDTO;
import br.com.hahn.votacao.domain.enums.VoteOption;
import br.com.hahn.votacao.domain.enums.VotingResult;
import br.com.hahn.votacao.domain.exception.ResultNotReadyException;
import br.com.hahn.votacao.domain.exception.VotingNotFoundException;
import br.com.hahn.votacao.domain.model.Result;
import br.com.hahn.votacao.domain.model.Vote;
import br.com.hahn.votacao.domain.model.Voting;
import br.com.hahn.votacao.domain.repository.ResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service para processamento e consulta de resultados de votações.
 * <p>
 * Calcula resultados baseado em maioria simples (SIM > NÃO = APROVADO)
 * e gerencia ciclo de vida desde validação até persistência.
 *
 * @author HahnGuil
 * @since 1.0
 */
@Service
public class ResultService {

    private static final Logger resultServiceLogger = LoggerFactory.getLogger(ResultService.class);

    private final ResultRepository resultRepository;
    private final VoteService voteService;
    private final VotingService votingService;

    public ResultService(ResultRepository resultRepository, VoteService voteService, VotingService votingService) {
        this.resultRepository = resultRepository;
        this.voteService = voteService;
        this.votingService = votingService;
    }

    /**
     * Recupera resultado consolidado de uma votação.
     *
     * @param requestContext contexto contendo ID da votação
     * @return dados do resultado
     * @throws VotingNotFoundException se votação não existir
     * @throws ResultNotReadyException se resultado ainda não disponível
     */
    public Mono<ResultResponseDTO> getResult(ServiceRequestContext requestContext) {
        resultServiceLogger.info("Buscando resultado para votingId: {}", requestContext.resourceId());

        return resultRepository.findById(requestContext.resourceId())
                .map(result -> new ResultResponseDTO(
                        result.getVotingId(),
                        result.getVotingSubject(),
                        result.getTotalVotes(),
                        result.getVotingResult().toString()
                ))
                .switchIfEmpty(checkVotingStatusAndThrowAppropriateException(requestContext.resourceId()))
                .doOnSuccess(result -> resultServiceLogger.info("Resultado encontrado para votingId: {}", requestContext.resourceId()))
                .doOnError(error -> resultServiceLogger.debug("Problema ao buscar resultado para votingId: {}", requestContext.resourceId()));
    }

    /**
     * Verifica se resultado está disponível para consulta.
     */
    public Mono<Boolean> isResultAvailable(ServiceRequestContext requestContext) {
        return resultRepository.findById(requestContext.resourceId())
                .map(result -> true)
                .defaultIfEmpty(false)
                .doOnNext(exists -> resultServiceLogger.debug("Resultado disponível para votingId {}: {}", requestContext.resourceId(), exists));
    }

    /**
     * Calcula e persiste resultado de votação encerrada.
     *
     * @param votingId ID da votação para processar
     * @return resultado calculado
     * @throws VotingNotFoundException se votação não existir
     * @throws ResultNotReadyException se votação ainda ativa
     */
    public Mono<ResultResponseDTO> createResult(String votingId) {
        resultServiceLogger.info("Iniciando cálculo do resultado para votingId: {}", votingId);

        return votingService.findById(votingId)
                .switchIfEmpty(Mono.error(new VotingNotFoundException("Voting not found with ID: " + votingId)))
                .flatMap(voting -> {
                    if (voting.isVotingSatus()) {
                        return Mono.error(new ResultNotReadyException(
                                "Result not ready yet. Voting is still active and will close at: " + voting.getCloseVotingDate()
                        ));
                    }

                    return resultRepository.findById(votingId)
                            .flatMap(existingResult -> {
                                resultServiceLogger.info("Resultado já existe para votingId: {}", votingId);
                                return Mono.just(new ResultResponseDTO(
                                        existingResult.getVotingId(),
                                        existingResult.getVotingSubject(),
                                        existingResult.getTotalVotes(),
                                        existingResult.getVotingResult().toString()
                                ));
                            })
                            .switchIfEmpty(calculateAndSaveResult(votingId, voting));
                });
    }

    /**
     * Calcula resultado final e persiste na base.
     */
    private Mono<ResultResponseDTO> calculateAndSaveResult(String votingId, Voting voting) {
        Mono<List<Vote>> votesMono = voteService.findByVotingId(votingId).collectList();

        return votesMono.flatMap(votesList -> {
                    String votingSubject = voting.getSubject();
                    Integer totalVotes = votesList.size();
                    VotingResult resultVoting = calculateVotingResult(votesList);

                    resultServiceLogger.info("Resultado calculado - Votação: {}, Total: {}, Resultado: {}",
                            votingId, totalVotes, resultVoting);

                    ResultCreateDTO resultCreateDTO = new ResultCreateDTO(
                            votingId, votingSubject, totalVotes, resultVoting
                    );

                    Result result = convertToResult(resultCreateDTO);
                    return resultRepository.save(result);
                })
                .map(savedResult -> new ResultResponseDTO(
                        savedResult.getVotingId(),
                        savedResult.getVotingSubject(),
                        savedResult.getTotalVotes(),
                        savedResult.getVotingResult().toString()
                ))
                .doOnSuccess(result -> resultServiceLogger.info("Resultado salvo com sucesso para votingId: {}", votingId))
                .doOnError(error -> resultServiceLogger.error("Erro ao calcular resultado para votingId: {}", votingId, error));
    }

    /**
     * Verifica status da votação e lança exceção apropriada quando resultado não existe.
     *
     * @param votingId ID da votação para verificar
     * @return nunca retorna - sempre lança exceção
     * @throws VotingNotFoundException se votação não existir
     * @throws ResultNotReadyException se votação ativa ou resultado sendo processado
     */
    private Mono<ResultResponseDTO> checkVotingStatusAndThrowAppropriateException(String votingId) {
        return votingService.findById(votingId)
                .<ResultResponseDTO>flatMap(voting -> {
                    if (voting.isVotingSatus()) {
                        return Mono.error(new ResultNotReadyException(
                                "Result not ready yet. Voting is still active and will close at: " + voting.getCloseVotingDate()
                        ));
                    } else {
                        return Mono.error(new ResultNotReadyException(
                                "Result is being processed. Voting ended at: " + voting.getCloseVotingDate() +
                                        ". Please try again in a few moments."
                        ));
                    }
                })
                .switchIfEmpty(Mono.error(new VotingNotFoundException("Voting not found with ID: " + votingId)));
    }

    /**
     * Converte DTO em entidade Result.
     */
    Result convertToResult(ResultCreateDTO resultCreateDTO) {
        Result result = new Result();
        result.setVotingId(resultCreateDTO.votingId());
        result.setVotingSubject(resultCreateDTO.votingSubject());
        result.setTotalVotes(resultCreateDTO.totalVotes());
        result.setVotingResult(resultCreateDTO.votingResult());
        return result;
    }

    /**
     * Calcula resultado baseado em maioria simples.
     * SIM > NÃO = APROVADO, caso contrário REPROVADO.
     */
    VotingResult calculateVotingResult(List<Vote> votes) {
        if (votes.isEmpty()) {
            resultServiceLogger.info("Nenhum voto encontrado, resultado padrão: REPROVADO");
            return VotingResult.REPROVADO;
        }

        long simCount = votes.stream()
                .filter(vote -> vote.getVoteOption() == VoteOption.SIM)
                .count();
        long naoCount = votes.stream()
                .filter(vote -> vote.getVoteOption() == VoteOption.NAO)
                .count();

        return simCount > naoCount ? VotingResult.APROVADO : VotingResult.REPROVADO;
    }
}