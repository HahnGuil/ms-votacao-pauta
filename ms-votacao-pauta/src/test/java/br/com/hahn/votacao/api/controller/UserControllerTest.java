package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.domain.dto.request.UserRequestDTO;
import br.com.hahn.votacao.domain.dto.response.UserResponseDTO;
import br.com.hahn.votacao.domain.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private UserService userService;
    private UserController userController;

    @BeforeEach
    void setUp() throws Exception {
        userService = mock(UserService.class);
        userController = new UserController(userService);

        java.lang.reflect.Field field = userController.getClass().getSuperclass().getDeclaredField("apiCurrentVersion");
        field.setAccessible(true);
        field.set(userController, "v1");
    }

    @Test
    void testCreateUser_ReturnsCreatedResponse() {
        // Arrange
        String version = "v1";
        UserRequestDTO requestDTO = new UserRequestDTO("João da Silva", "11111111111", version);
        UserResponseDTO responseDTO = new UserResponseDTO("some-id", "11111111111");

        // Mock do ServerWebExchange
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        RequestPath path = mock(RequestPath.class);

        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(path);
        when(path.value()).thenReturn("/api/v1/user/create-user");

        // Mock do service - vai receber o DTO com versão
        when(userService.createUser(any(UserRequestDTO.class)))
                .thenReturn(Mono.just(responseDTO));

        // Act
        Mono<ResponseEntity<UserResponseDTO>> result = userController.createUser(version, requestDTO);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.CREATED, response.getStatusCode());
                    assertEquals("some-id", response.getBody().userId());
                    assertEquals("11111111111", response.getBody().userCPF());
                })
                .verifyComplete();

        // Verify que o service foi chamado com um DTO que tem versão v1
        Mockito.verify(userService).createUser(argThat(dto ->
                "João da Silva".equals(dto.userName()) &&
                        "11111111111".equals(dto.userCPF()) &&
                        "v1".equals(dto.apiVersion())
        ));
    }
}
