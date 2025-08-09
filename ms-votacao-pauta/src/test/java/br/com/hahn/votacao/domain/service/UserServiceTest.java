package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.UserRequestDTO;
import br.com.hahn.votacao.domain.dto.response.UserResponseDTO;
import br.com.hahn.votacao.domain.exception.UserAlreadyExistsException;
import br.com.hahn.votacao.domain.model.User;
import br.com.hahn.votacao.domain.respository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userRepository.existsByuserCPF("12345678901")).thenReturn(false);

        UserResponseDTO response = userService.createUser(request);

        assertNotNull(response);
        assertEquals("1", response.userId());
        assertEquals("12345678901", response.userCPF());
        verify(userRepository).save(any(User.class));
        verify(userRepository).existsByuserCPF("12345678901");
    }

    @Test
    void testCreateUser_ThrowsUserAlreadyExistsException() {
        UserRequestDTO request = new UserRequestDTO("Jane Doe", "98765432100");
        User user = new User();
        user.setUserId("2");
        user.setUserName("Jane Doe");
        user.setUserCPF("98765432100");

        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userRepository.existsByuserCPF("98765432100")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(request));
        verify(userRepository).save(any(User.class));
        verify(userRepository).existsByuserCPF("98765432100");
    }
}

