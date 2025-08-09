package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.VotingRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VotingResponseDTO;
import br.com.hahn.votacao.domain.exception.InvalidFormatExpirationDate;
import br.com.hahn.votacao.domain.model.Voting;
import br.com.hahn.votacao.domain.repository.VotingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VotingServiceTest {

    @Mock
    private VotingRepository votingRepository;

    @InjectMocks
    private VotingService votingService;

    @Test
    void testCreateVoting_Success() {
        VotingRequestDTO request = new VotingRequestDTO("Assunto Teste", "5");
        Voting voting = new Voting();
        voting.setVotingId("abc123");
        voting.setSubject("Assunto Teste");
        voting.setOpenVotingDate(Instant.now());
        voting.setCloseVotingDate(Instant.now().plusSeconds(300));

        // Retorno reativo
        when(votingRepository.save(any(Voting.class))).thenReturn(Mono.just(voting));

        Mono<VotingResponseDTO> responseMono = votingService.createVoting(request);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("abc123", response.votingId());
                    assertEquals("/vote/abc123", response.voteUrl());
                    assertEquals(voting.getCloseVotingDate(), response.closeVotingDate());
                })
                .verifyComplete();

        verify(votingRepository, times(1)).save(any(Voting.class));
    }

    @Test
    void testCreateVoting_InvalidExpirationDate_ThrowsException() {
        VotingRequestDTO request = new VotingRequestDTO("Assunto Teste", "invalid");

        Mono<VotingResponseDTO> responseMono = votingService.createVoting(request);

        StepVerifier.create(responseMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof InvalidFormatExpirationDate &&
                                throwable.getMessage().equals("Invalid time format, poll timeout set to 1 minute.")
                )
                .verify();
    }

    @Test
    void testCreateVoting_NullExpirationDate_UsesDefault() {
        VotingRequestDTO request = new VotingRequestDTO("Assunto Teste", null);
        Voting voting = new Voting();
        voting.setVotingId("def456");
        voting.setSubject("Assunto Teste");
        voting.setOpenVotingDate(Instant.now());
        voting.setCloseVotingDate(voting.getOpenVotingDate().plusSeconds(60));

        when(votingRepository.save(any(Voting.class))).thenReturn(Mono.just(voting));

        Mono<VotingResponseDTO> responseMono = votingService.createVoting(request);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("def456", response.votingId());
                    assertEquals("/vote/def456", response.voteUrl());
                    assertEquals(voting.getCloseVotingDate(), response.closeVotingDate());
                })
                .verifyComplete();

        verify(votingRepository, times(1)).save(any(Voting.class));
    }
}

