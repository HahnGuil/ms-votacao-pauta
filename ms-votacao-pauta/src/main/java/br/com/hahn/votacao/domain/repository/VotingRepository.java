package br.com.hahn.votacao.domain.repository;

import br.com.hahn.votacao.domain.model.Voting;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VotingRepository extends ReactiveMongoRepository<Voting, String> {
}