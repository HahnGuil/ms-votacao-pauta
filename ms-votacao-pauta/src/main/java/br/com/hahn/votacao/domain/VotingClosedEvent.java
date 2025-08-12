package br.com.hahn.votacao.domain;

import java.time.Instant;

/**
 * Evento de domínio disparado quando uma votação é encerrada.
 * <p>
 * Utilizado para comunicação assíncrona entre bounded contexts,
 * permitindo que outros componentes do sistema reajam ao fechamento
 * de votações sem acoplamento direto.
 *
 * @param votingId ID único da votação encerrada
 * @param votingSubject assunto/título da votação
 * @param closedAt timestamp exato do encerramento
 * @param totalVotes total de votos computados na votação
 *
 * @author HahnGuil
 * @since 1.0
 */
public record VotingClosedEvent(String votingId, String votingSubject, Instant closedAt, Integer totalVotes) {
}
