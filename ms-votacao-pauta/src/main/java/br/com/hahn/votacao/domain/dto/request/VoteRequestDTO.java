package br.com.hahn.votacao.domain.dto.request;

public record VoteRequestDTO(String votingId, String userId, String voteOption) {
}
