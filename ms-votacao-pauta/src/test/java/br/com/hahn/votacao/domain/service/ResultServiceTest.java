package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.ResultCreateDTO;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class ResultServiceTest {

    private ResultRepository resultRepository;
    private VoteService voteService;
    private VotingService votingService;
    private ResultService resultService;

    @BeforeEach
    void setUp() {
        resultRepository = Mockito.mock(ResultRepository.class);
        voteService = Mockito.mock(VoteService.class);
        votingService = Mockito.mock(VotingService.class);
        resultService = new ResultService(resultRepository, voteService, votingService);
    }

    @Test
    void testCreateResult_VotingNotFound() {
        Mockito.when(votingService.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(resultService.createResult("id"))
            .expectError(VotingNotFoundException.class)
            .verify();
    }

    @Test
    void testCreateResult_VotingStillActive() {
        Voting voting = new Voting();
        voting.setVotingSatus(true);
        voting.setCloseVotingDate(Instant.now());
        Mockito.when(votingService.findById(anyString())).thenReturn(Mono.just(voting));

        StepVerifier.create(resultService.createResult("id"))
            .expectError(ResultNotReadyException.class)
            .verify();
    }

    @Test
    void testCreateResult_ExistingResult() {
        Voting voting = new Voting();
        voting.setVotingSatus(false);
        voting.setSubject("subject");
        Mockito.when(votingService.findById(anyString())).thenReturn(Mono.just(voting));

        Result result = new Result();
        result.setVotingId("id");
        result.setVotingSubject("subject");
        result.setTotalVotes(10);
        result.setVotingResult(VotingResult.APROVADO);

        Mockito.when(resultRepository.findById(anyString())).thenReturn(Mono.just(result));
        // Add missing mock for voteService
        Mockito.when(voteService.findByVotingId(anyString())).thenReturn(Flux.empty());

        StepVerifier.create(resultService.createResult("id"))
                .assertNext(response -> {
                    assertEquals("id", response.votingId());
                    assertEquals("subject", response.votingSubject());
                    assertEquals(10, response.totalVotes());
                    assertEquals("APROVADO", response.votingResult());
                })
                .verifyComplete();
    }

    @Test
    void testCreateResult_CalculateAndSaveResult_Aprovado() {
        Voting voting = new Voting();
        voting.setVotingSatus(false);
        voting.setSubject("subject");
        Mockito.when(votingService.findById(anyString())).thenReturn(Mono.just(voting));
        Mockito.when(resultRepository.findById(anyString())).thenReturn(Mono.empty());

        Vote vote1 = new Vote();
        vote1.setVoteOption(VoteOption.SIM);
        Vote vote2 = new Vote();
        vote2.setVoteOption(VoteOption.NAO);
        Vote vote3 = new Vote();
        vote3.setVoteOption(VoteOption.SIM);

        List<Vote> votes = Arrays.asList(vote1, vote2, vote3);
        Mockito.when(voteService.findByVotingId(anyString())).thenReturn(Flux.fromIterable(votes));

        ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
        Mockito.when(resultRepository.save(resultCaptor.capture())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(resultService.createResult("id"))
            .assertNext(response -> {
                assertEquals("id", response.votingId());
                assertEquals("subject", response.votingSubject());
                assertEquals(3, response.totalVotes());
                assertEquals("APROVADO", response.votingResult());
            })
            .verifyComplete();

        Result savedResult = resultCaptor.getValue();
        assertEquals(VotingResult.APROVADO, savedResult.getVotingResult());
    }

    @Test
    void testCreateResult_CalculateAndSaveResult_Reprovado_EmptyVotes() {
        Voting voting = new Voting();
        voting.setVotingSatus(false);
        voting.setSubject("subject");
        Mockito.when(votingService.findById(anyString())).thenReturn(Mono.just(voting));
        Mockito.when(resultRepository.findById(anyString())).thenReturn(Mono.empty());
        Mockito.when(voteService.findByVotingId(anyString())).thenReturn(Flux.empty());

        Mockito.when(resultRepository.save(any(Result.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(resultService.createResult("id"))
            .assertNext(response -> {
                assertEquals("REPROVADO", response.votingResult());
                assertEquals(0, response.totalVotes());
            })
            .verifyComplete();
    }

    @Test
    void testGetResult_ResultExists() {
        Result result = new Result();
        result.setVotingId("id");
        result.setVotingSubject("subject");
        result.setTotalVotes(5);
        result.setVotingResult(VotingResult.REPROVADO);

        Mockito.when(resultRepository.findById(anyString())).thenReturn(Mono.just(result));
        // Add missing mock for votingService
        Mockito.when(votingService.findById(anyString())).thenReturn(Mono.just(new Voting()));

        StepVerifier.create(resultService.getResult("id"))
                .assertNext(response -> {
                    assertEquals("id", response.votingId());
                    assertEquals("subject", response.votingSubject());
                    assertEquals(5, response.totalVotes());
                    assertEquals("REPROVADO", response.votingResult());
                })
                .verifyComplete();
    }

    @Test
    void testGetResult_ResultNotExists_VotingStillActive() {
        Mockito.when(resultRepository.findById(anyString())).thenReturn(Mono.empty());
        Voting voting = new Voting();
        voting.setVotingSatus(true);
        voting.setCloseVotingDate(Instant.now());
        Mockito.when(votingService.findById(anyString())).thenReturn(Mono.just(voting));

        StepVerifier.create(resultService.getResult("id"))
            .expectError(ResultNotReadyException.class)
            .verify();
    }

    @Test
    void testGetResult_ResultNotExists_VotingEnded() {
        Mockito.when(resultRepository.findById(anyString())).thenReturn(Mono.empty());
        Voting voting = new Voting();
        voting.setVotingSatus(false);
        voting.setCloseVotingDate(Instant.now());
        Mockito.when(votingService.findById(anyString())).thenReturn(Mono.just(voting));

        StepVerifier.create(resultService.getResult("id"))
            .expectError(ResultNotReadyException.class)
            .verify();
    }

    @Test
    void testGetResult_ResultNotExists_VotingNotFound() {
        Mockito.when(resultRepository.findById(anyString())).thenReturn(Mono.empty());
        Mockito.when(votingService.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(resultService.getResult("id"))
            .expectError(VotingNotFoundException.class)
            .verify();
    }

    @Test
    void testIsResultAvailable_True() {
        Result result = new Result();
        Mockito.when(resultRepository.findById(anyString())).thenReturn(Mono.just(result));

        StepVerifier.create(resultService.isResultAvailable("id"))
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void testIsResultAvailable_False() {
        Mockito.when(resultRepository.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(resultService.isResultAvailable("id"))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void testConvertToResult() {
        ResultCreateDTO dto = new ResultCreateDTO("id", "subject", 2, VotingResult.APROVADO);
        Result result = resultService.convertToResult(dto);

        assertEquals("id", result.getVotingId());
        assertEquals("subject", result.getVotingSubject());
        assertEquals(2, result.getTotalVotes());
        assertEquals(VotingResult.APROVADO, result.getVotingResult());
    }

    @Test
    void testCalculateVotingResult_Aprovado() {
        Vote vote1 = new Vote();
        vote1.setVoteOption(VoteOption.SIM);
        Vote vote2 = new Vote();
        vote2.setVoteOption(VoteOption.NAO);
        Vote vote3 = new Vote();
        vote3.setVoteOption(VoteOption.SIM);

        List<Vote> votes = Arrays.asList(vote1, vote2, vote3);
        VotingResult result = resultService.calculateVotingResult(votes);

        assertEquals(VotingResult.APROVADO, result);
    }

    @Test
    void testCalculateVotingResult_Reprovado() {
        Vote vote1 = new Vote();
        vote1.setVoteOption(VoteOption.NAO);
        Vote vote2 = new Vote();
        vote2.setVoteOption(VoteOption.NAO);
        Vote vote3 = new Vote();
        vote3.setVoteOption(VoteOption.SIM);

        List<Vote> votes = Arrays.asList(vote1, vote2, vote3);
        VotingResult result = resultService.calculateVotingResult(votes);

        assertEquals(VotingResult.REPROVADO, result);
    }

    @Test
    void testCalculateVotingResult_EmptyList() {
        List<Vote> votes = Collections.emptyList();
        VotingResult result = resultService.calculateVotingResult(votes);

        assertEquals(VotingResult.REPROVADO, result);
    }
}

