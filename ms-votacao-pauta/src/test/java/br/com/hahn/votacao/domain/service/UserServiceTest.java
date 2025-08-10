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

        when(userRepository.existsUserByuserCPF("12345678901")).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        Mono<UserResponseDTO> responseMono = userService.createUser(request);

        UserResponseDTO response = responseMono.block();
        assertNotNull(response);
        assertEquals("1", response.userId());
        assertEquals("12345678901", response.userCPF());
        verify(userRepository).existsUserByuserCPF("12345678901");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUser_ThrowsUserAlreadyExistsException() {
        UserRequestDTO request = new UserRequestDTO("Jane Doe", "98765432100");

        when(userRepository.existsUserByuserCPF("98765432100")).thenReturn(Mono.just(true));

        Mono<UserResponseDTO> responseMono = userService.createUser(request);

        assertThrows(UserAlreadyExistsException.class, responseMono::block);
        verify(userRepository, never()).save(any(User.class));
        verify(userRepository).existsUserByuserCPF("98765432100");
    }

    @Test
    void testCreateUser_BlankCPF() {
        UserRequestDTO request = new UserRequestDTO("Blank CPF", "");
        User user = new User();
        user.setUserId("2");
        user.setUserName("Blank CPF");
        user.setUserCPF("");

        when(userRepository.existsUserByuserCPF("")).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        Mono<UserResponseDTO> responseMono = userService.createUser(request);

        UserResponseDTO response = responseMono.block();
        assertNotNull(response);
        assertEquals("2", response.userId());
        assertEquals("", response.userCPF());
        verify(userRepository).existsUserByuserCPF("");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUser_BlankName() {
        UserRequestDTO request = new UserRequestDTO("", "11122233344");
        User user = new User();
        user.setUserId("3");
        user.setUserName("");
        user.setUserCPF("11122233344");

        when(userRepository.existsUserByuserCPF("11122233344")).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        Mono<UserResponseDTO> responseMono = userService.createUser(request);

        UserResponseDTO response = responseMono.block();
        assertNotNull(response);
        assertEquals("3", response.userId());
        assertEquals("11122233344", response.userCPF());
        verify(userRepository).existsUserByuserCPF("11122233344");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUser_NullCPF() {
        UserRequestDTO request = new UserRequestDTO("Null CPF", null);
        User user = new User();
        user.setUserId("4");
        user.setUserName("Null CPF");
        user.setUserCPF(null);

        when(userRepository.existsUserByuserCPF(null)).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        Mono<UserResponseDTO> responseMono = userService.createUser(request);

        UserResponseDTO response = responseMono.block();
        assertNotNull(response);
        assertEquals("4", response.userId());
        assertNull(response.userCPF());
        verify(userRepository).existsUserByuserCPF(null);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUser_NullName() {
        UserRequestDTO request = new UserRequestDTO(null, "55566677788");
        User user = new User();
        user.setUserId("5");
        user.setUserName(null);
        user.setUserCPF("55566677788");

        when(userRepository.existsUserByuserCPF("55566677788")).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        Mono<UserResponseDTO> responseMono = userService.createUser(request);

        UserResponseDTO response = responseMono.block();
        assertNotNull(response);
        assertEquals("5", response.userId());
        assertEquals("55566677788", response.userCPF());
        verify(userRepository).existsUserByuserCPF("55566677788");
        verify(userRepository).save(any(User.class));
    }
}
