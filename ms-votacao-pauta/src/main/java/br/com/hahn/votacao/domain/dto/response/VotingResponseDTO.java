package br.com.hahn.votacao.domain.dto.response;

import java.time.Instant;

public record VotingResponseDTO(String votingId, String voteUrl, Instant closeVotingDate, String resultUrl) {
}
