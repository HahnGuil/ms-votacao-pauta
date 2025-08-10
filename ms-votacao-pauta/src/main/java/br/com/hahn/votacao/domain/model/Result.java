package br.com.hahn.votacao.domain.model;

import br.com.hahn.votacao.domain.enums.VotingResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Result {

    @Id
    private String votingId;
    private String votingSubject;
    private Integer totalVotes;
    private VotingResult votingResult;
}
