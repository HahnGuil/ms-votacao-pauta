package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.VotingRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VotingResponseDTO;
import br.com.hahn.votacao.domain.exception.InvalidFormatExpirationDate;
import br.com.hahn.votacao.domain.model.Voting;
import br.com.hahn.votacao.domain.respository.VotingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        when(votingRepository.save(any(Voting.class))).thenReturn(voting);

        VotingResponseDTO response = votingService.createVoting(request);

        assertNotNull(response);
        assertEquals("abc123", response.votingId());
        assertEquals("/vote/abc123", response.voteUrl());
        assertEquals(voting.getCloseVotingDate(), response.closeVotingDate());
        verify(votingRepository, times(1)).save(any(Voting.class));
    }

    @Test
    void testCreateVoting_InvalidExpirationDate_ThrowsException() {
        VotingRequestDTO request = new VotingRequestDTO("Assunto Teste", "invalid");

        InvalidFormatExpirationDate exception = assertThrows(
            InvalidFormatExpirationDate.class,
            () -> votingService.createVoting(request)
        );
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

        when(votingRepository.save(any(Voting.class))).thenReturn(voting);

        VotingResponseDTO response = votingService.createVoting(request);

        assertNotNull(response);
        assertEquals("def456", response.votingId());
        assertEquals("/vote/def456", response.voteUrl());
        assertEquals(voting.getCloseVotingDate(), response.closeVotingDate());
        verify(votingRepository, times(1)).save(any(Voting.class));
    }
}

