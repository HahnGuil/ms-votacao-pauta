package br.com.hahn.votacao.domain.repository;

import br.com.hahn.votacao.domain.model.Vote;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface VoteRepository extends ReactiveMongoRepository<Vote, String> {

    Mono<Vote> findByVotingIdAndUserId(String votingId, String userId);

    Flux<Vote> findByVotingId(String votingId);
}
