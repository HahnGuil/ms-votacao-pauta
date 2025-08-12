package br.com.hahn.votacao.domain.dto;

import br.com.hahn.votacao.domain.enums.VotingResult;

/**
 * DTO para criação de resultados consolidados de votação.
 *
 * Transporta dados processados após o encerramento de uma votação,
 * incluindo totalizações e resultado final calculado.
 *
 * @param votingId identificador único da votação
 * @param votingSubject assunto/tema da votação realizada
 * @param totalVotes quantidade total de votos computados
 * @param votingResult resultado final da votação (APROVADO/REJEITADO/EMPATE)
 * @author HahnGuil
 * @since 1.0
 */
public record ResultCreateDTO(String votingId, String votingSubject, Integer totalVotes, VotingResult votingResult) {

}

