package br.com.hahn.votacao.infrastructure.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error";
    private static final String APPLICATION_JSON = "application/json";
    private static final String INTERNAL_SERVER_ERROR_DESCRIPTION = "Erro interno do servidor";
    private static final String STRING_TYPE = "string";
    private static final String OBJECT_TYPE = "object";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Votação Pauta")
                        .description("API para gerenciamento de votações, usuários e resultados. As informações trafegadas são confidenciais. Timeout padrão: 5000 milissegundos.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Guilherme Hahn")
                                .email("guilherme.f.h@hotmail.com")
                                .url("https://github.com/HahnGuil")
                        )
                )
                .servers(List.of(
                        new Server().url("http://localhost:2500/api/votacao").description("Servidor de desenvolvimento")
                ));
    }

    @Bean
    public OperationCustomizer specificResponsesCustomizer() {
        return (Operation operation, org.springframework.web.method.HandlerMethod handlerMethod) -> {
            ApiResponses responses = operation.getResponses();

            // Remove parâmetros automáticos para limpar a documentação
            if (operation.getParameters() != null) {
                operation.getParameters().clear();
            }

            // Delegar customização baseada no controller
            customizeOperationByController(responses, handlerMethod);

            return operation;
        };
    }

    private void customizeOperationByController(ApiResponses responses, org.springframework.web.method.HandlerMethod handlerMethod) {
        String controllerName = handlerMethod.getMethod().getDeclaringClass().getSimpleName();
        String methodName = handlerMethod.getMethod().getName();

        switch (controllerName) {
            case "VoteController":
                customizeVoteControllerResponses(responses, methodName);
                break;
            case "VotingController":
                customizeVotingControllerResponses(responses, methodName);
                break;
            case "UserController":
                customizeUserControllerResponses(responses, methodName);
                break;
            case "ResultController":
                customizeResultControllerResponses(responses, methodName);
                break;
            default:
                // Não há customização específica para outros controllers
                break;
        }
    }

    private void customizeVoteControllerResponses(ApiResponses responses, String methodName) {
        if ("vote".equals(methodName)) {
            addVoteEndpointResponses(responses);
        }
    }

    private void customizeVotingControllerResponses(ApiResponses responses, String methodName) {
        if ("createVoting".equals(methodName)) {
            addCreateVotingResponses(responses);
        }
    }

    private void customizeUserControllerResponses(ApiResponses responses, String methodName) {
        if ("createUser".equals(methodName)) {
            addUserEndpointResponses(responses);
        }
    }

    private void customizeResultControllerResponses(ApiResponses responses, String methodName) {
        if ("getResult".equals(methodName)) {
            addResultEndpointResponses(responses);
        } else if ("resultExists".equals(methodName)) {
            addResultExistsResponses(responses);
        }
    }

    private void addVoteEndpointResponses(ApiResponses responses) {
        // Resposta de sucesso
        responses.addApiResponse("201", new ApiResponse()
                .description("Voto registrado com sucesso")
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createVoteResponseSchema()))));

        responses.addApiResponse("403", new ApiResponse()
                .description("Usuário já votou ou votação expirada")
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createErrorSchema("User has already voted")))));

        responses.addApiResponse("404", new ApiResponse()
                .description("Votação não encontrada, usuário não encontrado ou CPF inválido")
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createErrorSchema("CPF não habilitado para votar")))));

        responses.addApiResponse("500", new ApiResponse()
                .description(INTERNAL_SERVER_ERROR_DESCRIPTION)
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createErrorSchema(INTERNAL_SERVER_ERROR_MESSAGE)))));
    }

    private void addCreateVotingResponses(ApiResponses responses) {
        // Resposta de sucesso
        responses.addApiResponse("201", new ApiResponse()
                .description("Votação criada com sucesso")
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createVotingResponseSchema()))));

        responses.addApiResponse("400", new ApiResponse()
                .description("Formato de data de expiração inválido")
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createErrorSchema("Invalid time format, poll timeout set to 1 minute.")))));

        responses.addApiResponse("500", new ApiResponse()
                .description(INTERNAL_SERVER_ERROR_DESCRIPTION)
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createErrorSchema(INTERNAL_SERVER_ERROR_MESSAGE)))));
    }

    private void addUserEndpointResponses(ApiResponses responses) {
        // Resposta de sucesso
        responses.addApiResponse("201", new ApiResponse()
                .description("Usuário criado com sucesso")
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createUserResponseSchema()))));

        // Resposta de conflito
        responses.addApiResponse("409", new ApiResponse()
                .description("Usuário já existe com este CPF")
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createErrorSchema("User already exists with CPF: 12345678901")))));

        // Erro interno
        responses.addApiResponse("500", new ApiResponse()
                .description(INTERNAL_SERVER_ERROR_DESCRIPTION)
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createErrorSchema(INTERNAL_SERVER_ERROR_MESSAGE)))));
    }

    private void addResultEndpointResponses(ApiResponses responses) {
        // Resposta de sucesso para getResult
        responses.addApiResponse("200", new ApiResponse()
                .description("Resultado da votação obtido com sucesso")
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createResultResponseSchema()))));

        responses.addApiResponse("202", new ApiResponse()
                .description("Resultado ainda não está pronto")
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createErrorSchema("Result not ready yet")))));

        responses.addApiResponse("404", new ApiResponse()
                .description("Votação não encontrada ou resultado não encontrado")
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createErrorSchema("Voting not found")))));

        responses.addApiResponse("500", new ApiResponse()
                .description(INTERNAL_SERVER_ERROR_DESCRIPTION)
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createErrorSchema(INTERNAL_SERVER_ERROR_MESSAGE)))));
    }

    private void addResultExistsResponses(ApiResponses responses) {
        responses.addApiResponse("200", new ApiResponse()
                .description("Verificação de existência do resultado")
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(new Schema<Boolean>().type("boolean").example(true)))));

        responses.addApiResponse("500", new ApiResponse()
                .description(INTERNAL_SERVER_ERROR_DESCRIPTION)
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(APPLICATION_JSON,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(createErrorSchema(INTERNAL_SERVER_ERROR_MESSAGE)))));
    }

    private Schema<Object> createErrorSchema(String exampleMessage) {
        Schema<Object> schema = new Schema<>();
        schema.type(OBJECT_TYPE);
        schema.addProperty("message", new Schema<String>().type(STRING_TYPE).example(exampleMessage));
        schema.addProperty("timestamp", new Schema<String>().type(STRING_TYPE).format("date-time"));
        return schema;
    }

    private Schema<Object> createUserResponseSchema() {
        Schema<Object> schema = new Schema<>();
        schema.type(OBJECT_TYPE);
        schema.addProperty("userId", new Schema<String>()
                .type(STRING_TYPE)
                .example("507f1f77bcf86cd799439011")
                .description("ID único do usuário criado"));
        schema.addProperty("userCPF", new Schema<String>()
                .type(STRING_TYPE)
                .example("12345678901")
                .description("CPF do usuário"));
        return schema;
    }

    private Schema<Object> createVoteResponseSchema() {
        Schema<Object> schema = new Schema<>();
        schema.type(OBJECT_TYPE);
        schema.addProperty("message", new Schema<String>()
                .type(STRING_TYPE)
                .example("Voto recebido com sucesso")
                .description("Mensagem de confirmação"));
        return schema;
    }

    private Schema<Object> createVotingResponseSchema() {
        Schema<Object> schema = new Schema<>();
        schema.type(OBJECT_TYPE);
        schema.addProperty("votingId", new Schema<String>()
                .type(STRING_TYPE)
                .example("6898ff38e855e877c1127394")
                .description("ID da votação que foi criada"));
        schema.addProperty("voteUrl", new Schema<String>()
                .type(STRING_TYPE)
                .example("http://localhost:2500/api/votacao/v1/vote/6899504c8175afcbc9a5b0f3")
                .description("URL da votação que foi criada"));
        schema.addProperty("closeVotingDate", new Schema<String>()
                .type(STRING_TYPE)
                .format("date-time")
                .example("2025-08-11T23:25:44.076618Z")
                .description("Hora que a votação criada será fechada"));
        schema.addProperty("resultUrl", new Schema<String>()
                .type(STRING_TYPE)
                .example("http://localhost:2500/api/votacao/v1/result/6898ff38e855e877c1127394")
                .description("URL para verificar o resultado quando a votação acabar"));
        return schema;
    }

    private Schema<Object> createResultResponseSchema() {
        Schema<Object> schema = new Schema<>();
        schema.type(OBJECT_TYPE);
        schema.addProperty("votingId", new Schema<String>()
                .type(STRING_TYPE)
                .example("689a7b088d19273ee6070d52")
                .description("Id da votação que quer consultar o resultado"));
        schema.addProperty("votingSubject", new Schema<String>()
                .type(STRING_TYPE)
                .example("Aumento no incentivo ao marketing esportivo")
                .description("Pauta da votação que foi realizada"));
        schema.addProperty("totalVotes", new Schema<Integer>()
                .type("integer")
                .example(300)
                .description("Total de votos válidos recebidos na votação"));
        schema.addProperty("votingResult", new Schema<String>()
                .type(STRING_TYPE)
                .example("APROVADO")
                .description("Resultado final se a pauta foi aprovada ou não"));
        return schema;
    }
}