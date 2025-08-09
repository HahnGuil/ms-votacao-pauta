package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.domain.dto.request.UserRequestDTO;
import br.com.hahn.votacao.domain.dto.response.UserResponseDTO;
import br.com.hahn.votacao.domain.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void testCreateUser_ReturnsCreatedResponse() {
        // Arrange
        UserRequestDTO mockRequest = new UserRequestDTO("João da Silva", "11111111111");
        UserResponseDTO mockResponse = new UserResponseDTO("João da Silva", "11111111111");
        when(userService.createUser(mockRequest)).thenReturn(mockResponse);

        // Act
        Mono<ResponseEntity<UserResponseDTO>> resultMono = userController.createUser(mockRequest);

        // Assert
        ResponseEntity<UserResponseDTO> response = resultMono.block();
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(userService, times(1)).createUser(mockRequest);
    }
}

