package br.com.hahn.votacao.domain;

import java.time.Instant;

public record VotingClosedEvent(String votingId, String votingSubject, Instant closedAt, Integer totalVotes) {
}
