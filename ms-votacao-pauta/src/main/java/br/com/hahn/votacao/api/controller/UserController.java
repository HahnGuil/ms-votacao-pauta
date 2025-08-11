package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.api.controller.base.BaseController;
import br.com.hahn.votacao.domain.dto.request.UserRequestDTO;
import br.com.hahn.votacao.domain.dto.response.UserResponseDTO;
import br.com.hahn.votacao.domain.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/user")
public class UserController extends BaseController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/{version}/create-user")
    public Mono<ResponseEntity<UserResponseDTO>> createUser(
            @PathVariable String version, @RequestBody UserRequestDTO userRequestDTO) {

        UserRequestDTO versionedDTO = new UserRequestDTO(userRequestDTO.userName(), userRequestDTO.userCPF(), determineApiVersion(version));

        return userService.createUser(versionedDTO)
                .map(userResponseDTO -> new ResponseEntity<>(userResponseDTO, HttpStatus.CREATED));
    }

}
