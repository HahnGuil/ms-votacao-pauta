package br.com.hahn.votacao.domain.repository;

import br.com.hahn.votacao.domain.model.Result;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultRepository extends ReactiveMongoRepository<Result, String> {

}
