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

@Service
public class UserService {

    private static final Logger userServiceLogger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<UserResponseDTO> createUser(UserRequestDTO userRequestDTO) {

        userServiceLogger.info("Criando um novo usuário");

        User user = convertToCollection(userRequestDTO);
        return userRepository.existsByuserCPF(userRequestDTO.userCPF())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        userServiceLogger.info("Usuário já existe, exceção sendo lançada");
                        return Mono.error(new UserAlreadyExistsException("User already exists with CPF: " + userRequestDTO.userCPF()));
                    }
                    return userRepository.save(user)
                            .map(savedUser -> new UserResponseDTO(savedUser.getUserId(), savedUser.getUserCPF()));
                });
    }

    private User convertToCollection(UserRequestDTO userRequestDTO) {
        User user = new User();
        user.setUserName(userRequestDTO.userName());
        user.setUserCPF(userRequestDTO.userCPF());
        return user;
    }
}