package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.domain.dto.request.UserRequestDTO;
import br.com.hahn.votacao.domain.dto.response.UserResponseDTO;
import br.com.hahn.votacao.domain.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

class UserControllerTest {

    private UserService userService;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        userController = new UserController(userService);
    }

    @Test
    void testCreateUser_ReturnsCreatedResponse() {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO("João da Silva", "11111111111");
        UserResponseDTO responseDTO = new UserResponseDTO("João da Silva", "11111111111");
        Mockito.when(userService.createUser(any(UserRequestDTO.class)))
                .thenReturn(Mono.just(responseDTO));

        // Act
        Mono<ResponseEntity<UserResponseDTO>> result = userController.createUser(requestDTO);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.CREATED, response.getStatusCode());
                    assertEquals(responseDTO, response.getBody());
                })
                .verifyComplete();

        Mockito.verify(userService).createUser(requestDTO);
    }
}
