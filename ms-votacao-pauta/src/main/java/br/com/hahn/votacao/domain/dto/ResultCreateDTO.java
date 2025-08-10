package br.com.hahn.votacao.domain.dto;

import br.com.hahn.votacao.domain.enums.VotingResult;

public record ResultCreateDTO(String votingId, String votingSubject, Integer totalVotes, VotingResult votingResult) {

}

