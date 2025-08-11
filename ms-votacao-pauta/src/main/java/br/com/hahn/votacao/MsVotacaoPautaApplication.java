package br.com.hahn.votacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
@EnableScheduling
public class MsVotacaoPautaApplication {

    private static final Logger logger = LoggerFactory.getLogger(MsVotacaoPautaApplication.class);
    // 🎨 VISUAL: Voltando com a string de separação original (melhor contraste)
    private static final String SEPARATOR_LINE = "============================================================";

    public static void main(String[] args) {
        try {
            logApplicationStarting();

            SpringApplication app = new SpringApplication(MsVotacaoPautaApplication.class);
            app.addListeners(new ApplicationStartupListener());
            app.run(args);

        } catch (Exception ex) {
            handleStartupException(ex);
            System.exit(1);
        }
    }

    private static void logApplicationStarting() {
        if (logger.isInfoEnabled()) {
            String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            logger.info("🚀 INICIANDO MS-VOTACAO-PAUTA");
            logger.info("📅 Data/Hora: {}", startTime);
            logger.info("👤 Usuário: HahnGuil");
        }
    }

    // 🎯 REFATORADO: Método principal simplificado (Complexidade Cognitiva reduzida)
    private static void handleStartupException(Exception ex) {
        if (!logger.isErrorEnabled()) {
            return;
        }

        logger.error("❌ FALHA CRÍTICA NA INICIALIZAÇÃO DA APLICAÇÃO!");
        logger.error(SEPARATOR_LINE);

        logSpecificErrorType(ex);
        logErrorDetails(ex);

        logger.error(SEPARATOR_LINE);
        logger.error("🛑 APLICAÇÃO FINALIZADA COM ERRO");
    }

    // 🔧 EXTRAÍDO: Lógica de identificação de tipo de erro
    private static void logSpecificErrorType(Exception ex) {
        if (ex.getCause() == null) {
            return;
        }

        String causeMessage = ex.getCause().getMessage();
        if (causeMessage == null) {
            return;
        }

        ErrorType errorType = identifyErrorType(causeMessage);
        logErrorByType(errorType);
    }

    // 🔧 EXTRAÍDO: Identificação do tipo de erro (sem aninhamento)
    private static ErrorType identifyErrorType(String causeMessage) {
        if (causeMessage.contains("Connection refused") || causeMessage.contains("connect timed out")) {
            return ErrorType.CONNECTION_ERROR;
        }
        if (causeMessage.contains("Authentication failed")) {
            return ErrorType.AUTHENTICATION_ERROR;
        }
        if (causeMessage.contains("Unknown database")) {
            return ErrorType.DATABASE_ERROR;
        }
        if (causeMessage.contains("kafka") || causeMessage.contains("Kafka")) {
            return ErrorType.KAFKA_ERROR;
        }
        if (causeMessage.contains("redis") || causeMessage.contains("Redis")) {
            return ErrorType.REDIS_ERROR;
        }
        return ErrorType.UNKNOWN_ERROR;
    }

    // 🔧 EXTRAÍDO: Log específico por tipo de erro
    private static void logErrorByType(ErrorType errorType) {
        switch (errorType) {
            case CONNECTION_ERROR:
                logger.error("🔌 ERRO DE CONEXÃO: Não foi possível conectar ao banco de dados");
                logger.error("💡 Verifique se o MongoDB está rodando e acessível");
                logger.error("🔧 Comando para iniciar MongoDB: docker run -d -p 27017:27017 mongo");
                break;
            case AUTHENTICATION_ERROR:
                logger.error("🔐 ERRO DE AUTENTICAÇÃO: Credenciais do banco de dados inválidas");
                logger.error("💡 Verifique as configurações de usuário/senha no application.yml");
                break;
            case DATABASE_ERROR:
                logger.error("🗄️ ERRO DE BANCO: Database não encontrado");
                logger.error("💡 Verifique se o database especificado existe");
                break;
            case KAFKA_ERROR:
                logger.error("📨 ERRO DO KAFKA: Não foi possível conectar ao Kafka");
                logger.error("💡 Verifique se o Kafka está rodando na porta configurada");
                break;
            case REDIS_ERROR:
                logger.error("🔴 ERRO DO REDIS: Não foi possível conectar ao Redis");
                logger.error("💡 Verifique se o Redis está rodando na porta configurada");
                break;
            case UNKNOWN_ERROR:
            default:
                // Será tratado no logErrorDetails
                break;
        }
    }

    // 🔧 EXTRAÍDO: Log dos detalhes do erro
    private static void logErrorDetails(Exception ex) {
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            ErrorType errorType = identifyErrorType(ex.getCause().getMessage());
            if (errorType == ErrorType.UNKNOWN_ERROR) {
                logger.error("⚠️ ERRO DESCONHECIDO: {}", ex.getCause().getMessage());
            }
        }
        logger.error("📋 Detalhes do erro: {}", ex.getMessage());
    }

    // 🆕 ENUM: Para organizar tipos de erro
    private enum ErrorType {
        CONNECTION_ERROR,
        AUTHENTICATION_ERROR,
        DATABASE_ERROR,
        KAFKA_ERROR,
        REDIS_ERROR,
        UNKNOWN_ERROR
    }

    // Listener para quando a aplicação inicializa com sucesso
    static class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {
        private static final Logger startupLogger = LoggerFactory.getLogger(ApplicationStartupListener.class);

        @Override
        public void onApplicationEvent(ApplicationReadyEvent event) {
            if (startupLogger.isInfoEnabled()) {
                try {
                    Environment env = event.getApplicationContext().getEnvironment();
                    String serverPort = env.getProperty("server.port", "8080");
                    String hostAddress = InetAddress.getLocalHost().getHostAddress();

                    // 🎨 VISUAL: Mantendo as linhas de separação originais
                    startupLogger.info(SEPARATOR_LINE);
                    startupLogger.info("✅ MS-VOTACAO-PAUTA INICIADO COM SUCESSO!");
                    startupLogger.info("🌐 Aplicação disponível em:");
                    startupLogger.info("   • Local:    http://localhost:{}", serverPort);
                    startupLogger.info("   • Network:  http://{}:{}", hostAddress, serverPort);
                    startupLogger.info("📊 Swagger:   http://localhost:{}/swagger-ui.html", serverPort);
                    startupLogger.info("🔍 Actuator: http://localhost:{}/actuator/health", serverPort);
                    startupLogger.info(SEPARATOR_LINE);
                    startupLogger.info("🎯 COMPONENTES ATIVOS:");

                    // Verificar componentes ativos
                    if (isMongoConnected(event)) {
                        startupLogger.info("   ✅ MongoDB: Conectado");
                    } else {
                        startupLogger.warn("   ⚠️ MongoDB: Status desconhecido");
                    }

                    startupLogger.info("   ✅ Scheduler: Ativo (verificação de votações)");
                    startupLogger.info("   ✅ Event Handler: Ativo (processamento de resultados)");
                    startupLogger.info(SEPARATOR_LINE);

                } catch (UnknownHostException e) {
                    startupLogger.warn("Não foi possível determinar o endereço da rede: {}", e.getMessage());
                }
            }
        }

        private boolean isMongoConnected(ApplicationReadyEvent event) {
            try {
                return !event.getApplicationContext().getBeansOfType(org.springframework.data.mongodb.core.ReactiveMongoTemplate.class).isEmpty();
            } catch (Exception _) {
                return false;
            }
        }
    }


}