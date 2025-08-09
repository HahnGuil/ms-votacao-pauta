package br.com.hahn.votacao.domain.respository;

import br.com.hahn.votacao.domain.model.Voting;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VotingRepository extends MongoRepository<Voting, String> {
}
