package br.com.hahn.votacao.domain.dto.response;

public record ResultResponseDTO(String votingId, String votingSubject, Integer totalVotes, String votingResult) {
}