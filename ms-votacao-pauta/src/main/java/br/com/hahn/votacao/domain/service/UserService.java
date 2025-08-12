package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.UserRequestDTO;
import br.com.hahn.votacao.domain.dto.response.UserResponseDTO;
import br.com.hahn.votacao.domain.exception.UserAlreadyExistsException;
import br.com.hahn.votacao.domain.model.User;
import br.com.hahn.votacao.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service responsável pelo gerenciamento de usuários do sistema de votação.
 * <p>
 * Controla criação e consulta de usuários, garantindo unicidade de CPF
 * e conversão adequada entre DTOs e entidades de domínio.
 *
 * @author HahnGuil
 * @since 1.0
 */
@Service
public class UserService {

    private static final Logger userServiceLogger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Cria novo usuário validando unicidade do CPF.
     * <p>
     * Verifica se já existe usuário com o CPF informado antes de
     * persistir os dados, garantindo regra de negócio de CPF único.
     *
     * @param userRequestDTO dados do usuário a ser criado
     * @return dados do usuário criado com ID gerado
     * @throws UserAlreadyExistsException se CPF já estiver cadastrado
     */
    public Mono<UserResponseDTO> createUser(UserRequestDTO userRequestDTO) {

        userServiceLogger.info("Criando um novo usuário - API Version: {}", userRequestDTO.apiVersion());

        User user = convertToCollection(userRequestDTO);
        return userRepository.existsUserByuserCPF(userRequestDTO.userCPF())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        userServiceLogger.info("Usuário já existe, exceção sendo lançada");
                        return Mono.error(new UserAlreadyExistsException("User already exists with CPF: " + userRequestDTO.userCPF()));
                    }

                    return userRepository.save(user)
                            .map(savedUser -> new UserResponseDTO(
                                    savedUser.getUserId(), savedUser.getUserCPF()));
                });
    }

    /**
     * Busca usuário por ID.
     *
     * @param userId ID do usuário a ser localizado
     * @return usuário encontrado ou vazio se não existir
     */
    public Mono<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    /**
     * Converte DTO de request para entidade de domínio.
     *
     * @param userRequestDTO dados de entrada
     * @return entidade User configurada
     */
    private User convertToCollection(UserRequestDTO userRequestDTO) {
        User user = new User();
        user.setUserName(userRequestDTO.userName());
        user.setUserCPF(userRequestDTO.userCPF());
        return user;
    }
}