package br.com.hahn.votacao.domain.repository;

import br.com.hahn.votacao.domain.model.Vote;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository para operações de persistência de votos.
 *
 * Fornece consultas específicas para validação de voto único
 * e apuração de resultados por votação.
 */
@Repository
public interface VoteRepository extends ReactiveMongoRepository<Vote, String> {

    /**
     * Busca voto específico de um usuário em uma votação.
     *
     * Usado para validar se usuário já votou e evitar duplicação.
     *
     * @param votingId ID da votação
     * @param userId ID do usuário
     * @return voto existente ou vazio se não encontrado
     */
    Mono<Vote> findByVotingIdAndUserId(String votingId, String userId);

    /**
     * Recupera todos os votos de uma votação específica.
     *
     * Utilizado no processo de apuração e cálculo do resultado final.
     *
     * @param votingId ID da votação
     * @return fluxo com todos os votos da votação
     */
    Flux<Vote> findByVotingId(String votingId);
}
