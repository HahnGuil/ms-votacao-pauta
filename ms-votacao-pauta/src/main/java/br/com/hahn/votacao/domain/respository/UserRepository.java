package br.com.hahn.votacao.domain.respository;

import br.com.hahn.votacao.domain.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    boolean existsByuserCPF(String userCPF);
}
