package br.com.hahn.votacao.api.controller;

import br.com.hahn.votacao.api.controller.base.BaseController;
import br.com.hahn.votacao.domain.dto.request.UserRequestDTO;
import br.com.hahn.votacao.domain.dto.response.UserResponseDTO;
import br.com.hahn.votacao.domain.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


/**
 * Controller responsavel por gerenciar operações relacionadas ao usuário do sistema de votação.
 *
 * @author HahnGuil
 * @since 1.0
 */
@RestController
@RequestMapping("/user")
public class UserController extends BaseController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Cria um novo usuário
     *
     * Adapta a versão da API recebida no parametro de versionamento e colcoa em um DTO para o service consumir.
     *
     * @param version versão da API que está sendo utilizada current ou legacy
     * @param userRequestDTO dados do usuário que esta sendo criado (nome e cpf)
     * @return ResponseEntity com os dados do usuário criado (userId e CPF) e status 201.
     */
    @PostMapping("/{version}/create-user")
    public Mono<ResponseEntity<UserResponseDTO>> createUser(
            @PathVariable String version, @RequestBody UserRequestDTO userRequestDTO) {

        UserRequestDTO versionedDTO = new UserRequestDTO(userRequestDTO.userName(), userRequestDTO.userCPF(), determineApiVersion(version));

        return userService.createUser(versionedDTO)
                .map(userResponseDTO -> new ResponseEntity<>(userResponseDTO, HttpStatus.CREATED));
    }

}
