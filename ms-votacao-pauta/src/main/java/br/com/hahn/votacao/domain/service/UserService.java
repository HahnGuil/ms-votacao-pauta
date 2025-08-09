package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.UserRequestDTO;
import br.com.hahn.votacao.domain.dto.response.UserResponseDTO;
import br.com.hahn.votacao.domain.model.User;
import br.com.hahn.votacao.domain.respository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDTO createUser(UserRequestDTO userRequestDTO){
        User user = userRepository.save(convertToDTO(userRequestDTO));

        return new UserResponseDTO(user.getUserId(), user.getUserCPF());
    }

    private User convertToDTO(UserRequestDTO userRequestDTO){
        User user = new User();

        user.setUserName(userRequestDTO.userName());
        user.setUserCPF(userRequestDTO.userCPF());

        return user;
    }
}
