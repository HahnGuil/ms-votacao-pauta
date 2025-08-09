package br.com.hahn.votacao.domain.respository;

import br.com.hahn.votacao.domain.model.Vote;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends MongoRepository<Vote, String> {
}
