package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.UserRequestDTO;
import br.com.hahn.votacao.domain.dto.response.UserResponseDTO;
import br.com.hahn.votacao.domain.exception.UserAlreadyExistsException;
import br.com.hahn.votacao.domain.model.User;
import br.com.hahn.votacao.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    @Test
    void createUser_shouldCreateNewUser_whenUserDoesNotExist() {
        UserRequestDTO requestDTO = new UserRequestDTO("John Doe", "12345678901");
        User user = new User();
        user.setUserName(requestDTO.userName());
        user.setUserCPF(requestDTO.userCPF());
        user.setUserId("generatedId");

        when(userRepository.existsUserByuserCPF(requestDTO.userCPF())).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        Mono<UserResponseDTO> result = userService.createUser(requestDTO);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("generatedId", response.userId());
                    assertEquals("12345678901", response.userCPF());
                })
                .verifyComplete();

        verify(userRepository).existsUserByuserCPF(requestDTO.userCPF());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("John Doe", userCaptor.getValue().getUserName());
        assertEquals("12345678901", userCaptor.getValue().getUserCPF());
    }

    @Test
    void createUser_shouldThrowException_whenUserAlreadyExists() {
        UserRequestDTO requestDTO = new UserRequestDTO("Jane Doe", "98765432100");

        when(userRepository.existsUserByuserCPF(requestDTO.userCPF())).thenReturn(Mono.just(true));

        Mono<UserResponseDTO> result = userService.createUser(requestDTO);

        StepVerifier.create(result)
                .expectErrorSatisfies(ex -> {
                    assertTrue(ex instanceof UserAlreadyExistsException);
                    assertTrue(ex.getMessage().contains(requestDTO.userCPF()));
                })
                .verify();

        verify(userRepository).existsUserByuserCPF(requestDTO.userCPF());
        verify(userRepository, never()).save(any());
    }

    @Test
    void convertToCollection_shouldConvertDTOToUser() throws Exception {
        UserRequestDTO requestDTO = new UserRequestDTO("Alice", "11122233344");

        Method method = UserService.class.getDeclaredMethod("convertToCollection", UserRequestDTO.class);
        method.setAccessible(true);
        User user = (User) method.invoke(userService, requestDTO);

        assertEquals("Alice", user.getUserName());
        assertEquals("11122233344", user.getUserCPF());
    }

    @Test
    void findById_shouldReturnUser_whenUserExists() {
        User user = new User();
        user.setUserId("userId");
        user.setUserName("Test User");
        user.setUserCPF("12345678901");

        when(userRepository.findById("userId")).thenReturn(Mono.just(user));

        Mono<User> result = userService.findById("userId");

        StepVerifier.create(result)
                .assertNext(foundUser -> {
                    assertEquals("userId", foundUser.getUserId());
                    assertEquals("Test User", foundUser.getUserName());
                    assertEquals("12345678901", foundUser.getUserCPF());
                })
                .verifyComplete();

        verify(userRepository).findById("userId");
    }

    @Test
    void findById_shouldReturnEmpty_whenUserDoesNotExist() {
        when(userRepository.findById("userId")).thenReturn(Mono.empty());

        Mono<User> result = userService.findById("userId");

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();

        verify(userRepository).findById("userId");
    }
}
