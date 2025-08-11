package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.context.ServiceRequestContext;
import br.com.hahn.votacao.domain.enums.VoteOption;
import br.com.hahn.votacao.domain.enums.VotingResult;
import br.com.hahn.votacao.domain.exception.ResultNotReadyException;
import br.com.hahn.votacao.domain.exception.VotingNotFoundException;
import br.com.hahn.votacao.domain.model.Result;
import br.com.hahn.votacao.domain.model.Vote;
import br.com.hahn.votacao.domain.model.Voting;
import br.com.hahn.votacao.domain.repository.ResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ResultServiceTest {

    private ResultRepository resultRepository;
    private VoteService voteService;
    private VotingService votingService;
    private ResultService resultService;

    @BeforeEach
    void setUp() {
        resultRepository = mock(ResultRepository.class);
        voteService = mock(VoteService.class);
        votingService = mock(VotingService.class);
        resultService = new ResultService(resultRepository, voteService, votingService);
    }

    @Test
    void testIsResultAvailableTrue() {
        String votingId = "v1";
        ServiceRequestContext ctx = new ServiceRequestContext(votingId, "v1");

        Result result = new Result();
        when(resultRepository.findById(votingId)).thenReturn(Mono.just(result));

        StepVerifier.create(resultService.isResultAvailable(ctx))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testIsResultAvailableFalse() {
        String votingId = "v2";
        ServiceRequestContext ctx = new ServiceRequestContext(votingId, "v1");

        when(resultRepository.findById(votingId)).thenReturn(Mono.empty());

        StepVerifier.create(resultService.isResultAvailable(ctx))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void testGetResultFound() {
        String votingId = "v3";
        ServiceRequestContext ctx = new ServiceRequestContext(votingId, "v1");

        Result result = new Result();
        result.setVotingId(votingId);
        result.setVotingSubject("Assunto");
        result.setTotalVotes(10);
        result.setVotingResult(VotingResult.APROVADO);

        when(resultRepository.findById(votingId)).thenReturn(Mono.just(result));
        // Correction: mock votingService.findById to return Mono.empty()
        when(votingService.findById(votingId)).thenReturn(Mono.empty());

        StepVerifier.create(resultService.getResult(ctx))
                .expectNextMatches(dto -> dto.votingId().equals(votingId)
                        && dto.votingSubject().equals("Assunto")
                        && dto.totalVotes() == 10
                        && dto.votingResult().equals("APROVADO"))
                .verifyComplete();
    }

    @Test
    void testGetResultNotFoundVotingActive() {
        String votingId = "v4";
        ServiceRequestContext ctx = new ServiceRequestContext(votingId, "v1");

        when(resultRepository.findById(votingId)).thenReturn(Mono.empty());

        Voting voting = new Voting();
        voting.setVotingSatus(true);
        voting.setCloseVotingDate(LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant());
        when(votingService.findById(votingId)).thenReturn(Mono.just(voting));

        StepVerifier.create(resultService.getResult(ctx))
                .expectError(ResultNotReadyException.class)
                .verify();
    }

    @Test
    void testGetResultNotFoundVotingClosed() {
        String votingId = "v5";
        ServiceRequestContext ctx = new ServiceRequestContext(votingId, "v1");

        when(resultRepository.findById(votingId)).thenReturn(Mono.empty());

        Voting voting = new Voting();
        voting.setVotingSatus(false);
        voting.setCloseVotingDate(LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant());
        when(votingService.findById(votingId)).thenReturn(Mono.just(voting));

        StepVerifier.create(resultService.getResult(ctx))
                .expectError(ResultNotReadyException.class)
                .verify();
    }

    @Test
    void testGetResultVotingNotFound() {
        String votingId = "v6";
        ServiceRequestContext ctx = new ServiceRequestContext(votingId, "v1");

        when(resultRepository.findById(votingId)).thenReturn(Mono.empty());
        when(votingService.findById(votingId)).thenReturn(Mono.empty());

        StepVerifier.create(resultService.getResult(ctx))
                .expectError(VotingNotFoundException.class)
                .verify();
    }

    @Test
    void testCreateResultVotingNotFound() {
        String votingId = "v7";
        when(votingService.findById(votingId)).thenReturn(Mono.empty());

        StepVerifier.create(resultService.createResult(votingId))
                .expectError(VotingNotFoundException.class)
                .verify();
    }

    @Test
    void testCreateResultVotingActive() {
        String votingId = "v8";
        Voting voting = new Voting();
        voting.setVotingSatus(true);
        voting.setCloseVotingDate(LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant());
        when(votingService.findById(votingId)).thenReturn(Mono.just(voting));

        StepVerifier.create(resultService.createResult(votingId))
                .expectError(ResultNotReadyException.class)
                .verify();
    }

    @Test
    void testCreateResultAlreadyExists() {
        String votingId = "v9";
        Voting voting = new Voting();
        voting.setVotingSatus(false);
        voting.setSubject("Teste");
        when(votingService.findById(votingId)).thenReturn(Mono.just(voting));

        Result result = new Result();
        result.setVotingId(votingId);
        result.setVotingSubject("Teste");
        result.setTotalVotes(5);
        result.setVotingResult(VotingResult.APROVADO);

        when(resultRepository.findById(votingId)).thenReturn(Mono.just(result));
        // Fix: mock voteService to return an empty Flux
        when(voteService.findByVotingId(votingId)).thenReturn(Flux.empty());

        StepVerifier.create(resultService.createResult(votingId))
                .expectNextMatches(dto -> dto.votingId().equals(votingId)
                        && dto.votingSubject().equals("Teste")
                        && dto.totalVotes() == 5
                        && dto.votingResult().equals("APROVADO"))
                .verifyComplete();
    }

    @Test
    void testCreateResultCalculateAndSave() {
        String votingId = "v10";
        Voting voting = new Voting();
        voting.setVotingSatus(false);
        voting.setSubject("Assunto");
        when(votingService.findById(votingId)).thenReturn(Mono.just(voting));
        when(resultRepository.findById(votingId)).thenReturn(Mono.empty());

        Vote vote1 = new Vote();
        vote1.setVoteOption(VoteOption.SIM);
        Vote vote2 = new Vote();
        vote2.setVoteOption(VoteOption.NAO);
        Vote vote3 = new Vote();
        vote3.setVoteOption(VoteOption.SIM);

        List<Vote> votes = Arrays.asList(vote1, vote2, vote3);
        when(voteService.findByVotingId(votingId)).thenReturn(Flux.fromIterable(votes));

        Result savedResult = new Result();
        savedResult.setVotingId(votingId);
        savedResult.setVotingSubject("Assunto");
        savedResult.setTotalVotes(3);
        savedResult.setVotingResult(VotingResult.APROVADO);

        when(resultRepository.save(any(Result.class))).thenReturn(Mono.just(savedResult));

        StepVerifier.create(resultService.createResult(votingId))
                .expectNextMatches(dto -> dto.votingId().equals(votingId)
                        && dto.votingSubject().equals("Assunto")
                        && dto.totalVotes() == 3
                        && dto.votingResult().equals("APROVADO"))
                .verifyComplete();
    }

    @Test
    void testCalculateVotingResultEmptyVotes() {
        List<Vote> votes = Collections.emptyList();
        VotingResult result = resultService.calculateVotingResult(votes);
        assertEquals(VotingResult.REPROVADO, result);
    }
}

