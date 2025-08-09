package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.VotingRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VotingResponseDTO;
import br.com.hahn.votacao.domain.exception.InvalidFormatExpirationDate;
import br.com.hahn.votacao.domain.model.Voting;
import br.com.hahn.votacao.domain.respository.VotingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class VotingService {

    private final VotingRepository votingRepository;

    public VotingService(VotingRepository votingRepository) {
        this.votingRepository = votingRepository;
    }

    @Value("${server.port}")
    private String serverPort;

    private static final String VOTING_CONTEXT = "/vote";

    public VotingResponseDTO createVoting(VotingRequestDTO votingRequestDTO) {
        Voting voting = votingRepository.save(convertToCollection(votingRequestDTO));
        String voteUrl = "http://localhost:" + serverPort + VOTING_CONTEXT + voting.getVotingId();
        return new VotingResponseDTO(voting.getVotingId(), voteUrl, voting.getCloseVotingDate());
    }

    private Voting convertToCollection(VotingRequestDTO votingRequestDTO){
        Instant openVotingDate = Instant.now();

        Voting voting = new Voting();
        voting.setSubject(votingRequestDTO.subject());
        voting.setOpenVotingDate(openVotingDate);
        voting.setCloseVotingDate(createExpirationDate(openVotingDate, votingRequestDTO.userDefinedExpirationDate()));

        return voting;
    }

    private Instant createExpirationDate(Instant openVotingDate, String userDefinedExpirationDate) {
        final long defaultMinutes = 1L;
        long minutes = defaultMinutes;

        if (userDefinedExpirationDate != null && !userDefinedExpirationDate.isBlank()) {
            try {
                minutes = Long.parseLong(userDefinedExpirationDate);
                if (minutes <= 0) {
                    minutes = defaultMinutes;
                }
            } catch (NumberFormatException _) {
                throw new InvalidFormatExpirationDate("Invalid time format, poll timeout set to 1 minute.");
            }
        }

        return openVotingDate.plus(Duration.ofMinutes(minutes));
    }
}


