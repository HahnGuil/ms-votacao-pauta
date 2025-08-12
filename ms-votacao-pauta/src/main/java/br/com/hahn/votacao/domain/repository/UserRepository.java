package br.com.hahn.votacao.domain.repository;

import br.com.hahn.votacao.domain.model.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repository para operações de persistência de usuários.
 *
 * Fornece operações específicas para validação de CPF
 * e gerenciamento de elegibilidade para votação.
 */
@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {

    /**
     * Verifica se já existe usuário com o CPF informado.
     *
     * @param userCPF CPF a ser verificado
     * @return true se CPF já está cadastrado, false caso contrário
     */
    Mono<Boolean> existsUserByuserCPF(String userCPF);
}
