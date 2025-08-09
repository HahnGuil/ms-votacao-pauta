package br.com.hahn.votacao.domain.model;

import br.com.hahn.votacao.domain.enums.VoteOption;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;

@CompoundIndex(name = "unique_vote_for_user", def = "{'votingId': 1, 'userId': 1}", unique = true)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Vote {
    @Id
    private String voteId;
    private String votingId;
    private String userId;
    private VoteOption voteOption;
}
