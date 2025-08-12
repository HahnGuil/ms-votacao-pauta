package br.com.hahn.votacao.domain.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Representa uma pauta de votação com período definido.
 * <p>
 * votingSatus: true = ativa, false = encerrada.
 * Período: openVotingDate até closeVotingDate.
 */
@Document
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Voting {

    @Id
    private String votingId;
    private String subject;
    private Instant openVotingDate;
    private Instant closeVotingDate;
    private boolean votingSatus;
}
