package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.VotingRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VotingResponseDTO;
import br.com.hahn.votacao.domain.exception.InvalidFormatExpirationDate;
import br.com.hahn.votacao.domain.exception.VotingExpiredException;
import br.com.hahn.votacao.domain.exception.VotingNotFoundException;
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
                    assertEquals("http://localhost:null/vote/abc123", response.voteUrl());
                    assertEquals(voting.getCloseVotingDate(), response.closeVotingDate());
                })
                .verifyComplete();

        verify(votingRepository, times(1)).save(any(Voting.class));
    }

    @Test
    void testCreateVoting_InvalidExpirationDate_ThrowsException() {
        VotingRequestDTO request = new VotingRequestDTO("Assunto Teste", "invalid");

        InvalidFormatExpirationDate exception = assertThrows(InvalidFormatExpirationDate.class, () -> {
            votingService.createVoting(request);
        });

        assertEquals("Invalid time format, poll timeout set to 1 minute.", exception.getMessage());
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
                    assertEquals("http://localhost:null/vote/def456", response.voteUrl());
                    assertEquals(voting.getCloseVotingDate(), response.closeVotingDate());
                })
                .verifyComplete();

        verify(votingRepository, times(1)).save(any(Voting.class));
    }

    @Test
    void testCreateVoting_ZeroExpirationDate_UsesDefault() {
        VotingRequestDTO request = new VotingRequestDTO("Assunto Teste", "0");
        Voting voting = new Voting();
        voting.setVotingId("zeroId");
        voting.setSubject("Assunto Teste");
        voting.setOpenVotingDate(Instant.now());
        voting.setCloseVotingDate(voting.getOpenVotingDate().plusSeconds(60));

        when(votingRepository.save(any(Voting.class))).thenReturn(Mono.just(voting));

        Mono<VotingResponseDTO> responseMono = votingService.createVoting(request);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("zeroId", response.votingId());
                    assertEquals("http://localhost:null/vote/zeroId", response.voteUrl());
                    assertEquals(voting.getCloseVotingDate(), response.closeVotingDate());
                })
                .verifyComplete();

        verify(votingRepository, times(1)).save(any(Voting.class));
    }

    @Test
    void testCreateVoting_NegativeExpirationDate_UsesDefault() {
        VotingRequestDTO request = new VotingRequestDTO("Assunto Teste", "-10");
        Voting voting = new Voting();
        voting.setVotingId("negId");
        voting.setSubject("Assunto Teste");
        voting.setOpenVotingDate(Instant.now());
        voting.setCloseVotingDate(voting.getOpenVotingDate().plusSeconds(60));

        when(votingRepository.save(any(Voting.class))).thenReturn(Mono.just(voting));

        Mono<VotingResponseDTO> responseMono = votingService.createVoting(request);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("negId", response.votingId());
                    assertEquals("http://localhost:null/vote/negId", response.voteUrl());
                    assertEquals(voting.getCloseVotingDate(), response.closeVotingDate());
                })
                .verifyComplete();

        verify(votingRepository, times(1)).save(any(Voting.class));
    }

    @Test
    void testCreateVoting_BlankExpirationDate_UsesDefault() {
        VotingRequestDTO request = new VotingRequestDTO("Assunto Teste", "   ");
        Voting voting = new Voting();
        voting.setVotingId("blankId");
        voting.setSubject("Assunto Teste");
        voting.setOpenVotingDate(Instant.now());
        voting.setCloseVotingDate(voting.getOpenVotingDate().plusSeconds(60));

        when(votingRepository.save(any(Voting.class))).thenReturn(Mono.just(voting));

        Mono<VotingResponseDTO> responseMono = votingService.createVoting(request);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("blankId", response.votingId());
                    assertEquals("http://localhost:null/vote/blankId", response.voteUrl());
                    assertEquals(voting.getCloseVotingDate(), response.closeVotingDate());
                })
                .verifyComplete();

        verify(votingRepository, times(1)).save(any(Voting.class));
    }

    @Test
    void testValidateExpireVotingTime_NotExpired() {
        Voting voting = new Voting();
        voting.setVotingId("notExpiredId");
        voting.setCloseVotingDate(Instant.now().plusSeconds(120));

        when(votingRepository.findById("notExpiredId")).thenReturn(Mono.just(voting));

        Mono<Void> result = votingService.validateExpireVotingTime("notExpiredId");

        StepVerifier.create(result)
                .verifyComplete();

        verify(votingRepository, times(1)).findById("notExpiredId");
    }

    @Test
    void testValidateExpireVotingTime_Expired_ThrowsException() {
        Voting voting = new Voting();
        voting.setVotingId("expiredId");
        voting.setCloseVotingDate(Instant.now().minusSeconds(10));

        when(votingRepository.findById("expiredId")).thenReturn(Mono.just(voting));

        Mono<Void> result = votingService.validateExpireVotingTime("expiredId");

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof VotingExpiredException &&
                                throwable.getMessage().equals("This voting has expired, you can no longer vote.")
                )
                .verify();

        verify(votingRepository, times(1)).findById("expiredId");
    }

    @Test
    void testValidateExpireVotingTime_VotingNotFound_ThrowsException() {
        when(votingRepository.findById("notFoundId")).thenReturn(Mono.empty());

        Mono<Void> result = votingService.validateExpireVotingTime("notFoundId");

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof VotingNotFoundException &&
                                throwable.getMessage().equals("Voting not found for this notFoundId")
                )
                .verify();

        verify(votingRepository, times(1)).findById("notFoundId");
    }
}
