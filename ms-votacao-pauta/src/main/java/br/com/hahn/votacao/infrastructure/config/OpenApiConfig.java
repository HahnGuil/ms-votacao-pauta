package br.com.hahn.votacao.infrastructure.config;

import io.swagger.v3.oas.models.Operation;
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
    public OperationCustomizer globalResponsesCustomizer() {
        return (Operation operation, org.springframework.web.method.HandlerMethod handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            responses.addApiResponse("400", new ApiResponse().description("Requisição inválida"));
            responses.addApiResponse("401", new ApiResponse().description("Não autorizado"));
            responses.addApiResponse("403", new ApiResponse().description("Proibido"));
            responses.addApiResponse("404", new ApiResponse().description("Recurso não encontrado"));
            responses.addApiResponse("500", new ApiResponse().description("Erro interno do servidor"));
            return operation;
        };
    }
}
