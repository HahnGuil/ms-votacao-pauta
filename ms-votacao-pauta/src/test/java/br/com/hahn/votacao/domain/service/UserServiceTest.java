package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.UserRequestDTO;
import br.com.hahn.votacao.domain.dto.response.UserResponseDTO;
import br.com.hahn.votacao.domain.exception.UserAlreadyExistsException;
import br.com.hahn.votacao.domain.model.User;
import br.com.hahn.votacao.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void testCreateUser_Success() {
        UserRequestDTO request = new UserRequestDTO("John Doe", "12345678901");
        User user = new User();
        user.setUserId("1");
        user.setUserName("John Doe");
        user.setUserCPF("12345678901");

        when(userRepository.existsByuserCPF("12345678901")).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        Mono<UserResponseDTO> responseMono = userService.createUser(request);

        UserResponseDTO response = responseMono.block();
        assertNotNull(response);
        assertEquals("1", response.userId());
        assertEquals("12345678901", response.userCPF());
        verify(userRepository).save(any(User.class));
        verify(userRepository).existsByuserCPF("12345678901");
    }

    @Test
    void testCreateUser_ThrowsUserAlreadyExistsException() {
        UserRequestDTO request = new UserRequestDTO("Jane Doe", "98765432100");

        when(userRepository.existsByuserCPF("98765432100")).thenReturn(Mono.just(true));

        Mono<UserResponseDTO> responseMono = userService.createUser(request);

        assertThrows(UserAlreadyExistsException.class, responseMono::block);
        verify(userRepository, never()).save(any(User.class));
        verify(userRepository).existsByuserCPF("98765432100");
    }
}

