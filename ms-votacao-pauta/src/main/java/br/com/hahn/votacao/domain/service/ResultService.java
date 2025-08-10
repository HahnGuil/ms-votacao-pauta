package br.com.hahn.votacao.domain.service;


import br.com.hahn.votacao.domain.dto.ResultCreateDTO;
import br.com.hahn.votacao.domain.dto.response.ResultResponseDTO;
import br.com.hahn.votacao.domain.enums.VoteOption;
import br.com.hahn.votacao.domain.enums.VotingResult;
import br.com.hahn.votacao.domain.model.Result;
import br.com.hahn.votacao.domain.model.Vote;
import br.com.hahn.votacao.domain.model.Voting;
import br.com.hahn.votacao.domain.repository.ResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

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

    public Mono<ResultResponseDTO> createResult(String votingId) {
        resultServiceLogger.info("Iniciando cálculo do resultado para votingId: {}", votingId);

        Mono<Voting> votingMono = votingService.findById(votingId);
        Mono<List<Vote>> votesMono = voteService.findByVotingId(votingId).collectList();

        return Mono.zip(votingMono, votesMono)
                .flatMap(tuple -> {
                    Voting voting = tuple.getT1();
                    List<Vote> votesList = tuple.getT2();

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

    public Mono<ResultResponseDTO> getResult(String votingId) {
        resultServiceLogger.info("Buscando resultado para votingId: {}", votingId);

        return resultRepository.findById(votingId) // ← Agora funciona porque votingId é o @Id
                .map(result -> new ResultResponseDTO(
                        result.getVotingId(),
                        result.getVotingSubject(),
                        result.getTotalVotes(),
                        result.getVotingResult().toString()
                ))
                .switchIfEmpty(Mono.error(new RuntimeException("Result not found for voting: " + votingId)))
                .doOnSuccess(result -> resultServiceLogger.info("Resultado encontrado para votingId: {}", votingId))
                .doOnError(error -> resultServiceLogger.warn("Resultado não encontrado para votingId: {}", votingId));
    }

    private Result convertToResult(ResultCreateDTO resultCreateDTO) {
        Result result = new Result();
        result.setVotingId(resultCreateDTO.votingId()); // ← Agora é o @Id
        result.setVotingSubject(resultCreateDTO.votingSubject());
        result.setTotalVotes(resultCreateDTO.totalVotes());
        result.setVotingResult(resultCreateDTO.votingResult());
        return result;
    }

    private VotingResult calculateVotingResult(List<Vote> votes) {
        long simCount = votes.stream()
                .filter(vote -> vote.getVoteOption() == VoteOption.SIM)
                .count();
        long naoCount = votes.stream()
                .filter(vote -> vote.getVoteOption() == VoteOption.NAO)
                .count();

        return simCount > naoCount ? VotingResult.APROVADO : VotingResult.REPROVADO;
    }

}
