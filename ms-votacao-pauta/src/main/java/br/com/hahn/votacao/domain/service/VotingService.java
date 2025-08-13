package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.VotingRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VotingResponseDTO;
import br.com.hahn.votacao.domain.exception.InvalidFormatExpirationDate;
import br.com.hahn.votacao.domain.exception.VotingExpiredException;
import br.com.hahn.votacao.domain.exception.VotingNotFoundException;
import br.com.hahn.votacao.domain.model.Voting;
import br.com.hahn.votacao.domain.repository.VotingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Service responsável pelo gerenciamento do ciclo de vida das votações.
 * <p>
 * Controla criação, consulta e validação temporal das votações, incluindo
 * geração automática de URLs de acesso e cálculo de datas de expiração.
 * Implementa regras de negócio para determinar elegibilidade de votações
 * baseada em status e tempo.
 *
 * @author HahnGuil
 * @since 1.0
 */
@Service
public class VotingService {

    private static final Logger votingServiceLogger = LoggerFactory.getLogger(VotingService.class);
    private static final String VOTING_CONTEXT = "vote";
    private static final String RESULT_CONTEXT = "result";
    private static final String LOCALHOST = "http://localhost:";
    private static final long DEFAULT_EXPIRATION_MINUTES = 1L;
    private static final DateTimeFormatter RESPONSE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final VotingRepository votingRepository;



    @Value("${server.port}")
    private String serverPort;

    @Value("${spring.webflux.base-path}")
    private String apiContext;

    public VotingService(VotingRepository votingRepository) {
        this.votingRepository = votingRepository;
    }

    /**
     * Cria nova votação com URLs de acesso geradas automaticamente.
     * <p>
     * Processa dados de entrada, calcula data de expiração baseada em
     * tempo definido pelo usuário (padrão 1 minuto) e gera URLs para
     * votação e consulta de resultados.
     *
     * @param votingRequestDTO dados da votação a ser criada
     * @return dados da votação criada incluindo URLs de acesso
     * @throws InvalidFormatExpirationDate se formato do tempo for inválido
     */
    public Mono<VotingResponseDTO> createVoting(VotingRequestDTO votingRequestDTO) {
        votingServiceLogger.info("Criando uma nova votação");
        Voting voting = convertToCollection(votingRequestDTO);

        return votingRepository.save(voting)
                .map(savedVoting -> buildVotingResponse(votingRequestDTO.apiVersion(), savedVoting));
    }

    /**
     * Recupera todas as votações cadastradas no sistema.
     *
     * @return fluxo com todas as votações
     */
    public Flux<Voting> findAllVotings() {
        return votingRepository.findAll();
    }

    /**
     * Busca votação específica por ID.
     *
     * @param votingId ID da votação
     * @return votação encontrada ou vazio se não existir
     */
    public Mono<Voting> findById(String votingId) {
        return votingRepository.findById(votingId);
    }

    /**
     * Persiste votação no banco de dados.
     *
     * @param voting entidade de votação a ser salva
     * @return votação salva com dados atualizados
     */
    public Mono<Voting> saveVoting(Voting voting) {
        return votingRepository.save(voting);
    }

    /**
     * Valida se votação está elegível para receber votos.
     * <p>
     * **VALIDAÇÕES SEQUENCIAIS:**
     * 1. **Existência** - Verifica se votação existe no banco
     * 2. **Status ativo** - Confirma se votação não foi manualmente inativada
     * 3. **Tempo válido** - Valida se não passou da data de expiração
     * <p>
     * Ordem otimizada: validações mais baratas primeiro, depois temporal.
     *
     * @param votingId ID da votação a ser validada
     * @return completado se votação está elegível
     * @throws VotingNotFoundException se votação não existir
     * @throws VotingExpiredException se inativa ou expirada por tempo
     */
    public Mono<Void> validateExpireVotingTime(String votingId) {
        votingServiceLogger.info("Validando se a votação ainda está ativa");

        return findVotingOrThrow(votingId)
                .flatMap(this::validateVotingIsActive)
                .flatMap(this::validateVotingNotExpired)
                .then();
    }

    /**
     * Busca votação por ID ou lança exceção se não encontrada.
     * <p>
     * auxiliar reutilizável para busca com validação de existência.
     *
     * @param votingId ID da votação
     * @return votação encontrada
     * @throws VotingNotFoundException se votação não existir
     */
    private Mono<Voting> findVotingOrThrow(String votingId) {
        return votingRepository.findById(votingId)
                .switchIfEmpty(Mono.error(new VotingNotFoundException("Voting not found for this " + votingId)));
    }

    /**
     * Valida se votação está ativa (não foi manualmente desabilitada).
     * <p>
     * Primeira validação do pipeline de elegibilidade - verifica status
     * de ativação manual da votação.
     *
     * @param voting entidade da votação
     * @return mesma votação se ativa
     * @throws VotingExpiredException se votação está inativa
     */
    private Mono<Voting> validateVotingIsActive(Voting voting) {
        if (!voting.isVotingSatus()) {
            votingServiceLogger.warn("Votação está inativa: {}", voting.getVotingId());
            return Mono.error(new VotingExpiredException("This voting is inactive and no longer accepts votes."));
        }
        return Mono.just(voting);
    }

    /**
     * Valida se votação não expirou por tempo.
     * <p>
     * Segunda validação do pipeline - verifica se a data atual não
     * ultrapassou o timestamp de fechamento da votação.
     *
     * @param voting entidade da votação
     * @return mesma votação se não expirada
     * @throws VotingExpiredException se votação expirou por tempo
     */
    private Mono<Voting> validateVotingNotExpired(Voting voting) {
        Instant now = Instant.now();
        if (now.isAfter(voting.getCloseVotingDate())) {
            votingServiceLogger.warn("Votação expirou por tempo: {}", voting.getVotingId());
            return Mono.error(new VotingExpiredException("This voting has expired, you can no longer vote."));
        }

        votingServiceLogger.info("Votação válida e ativa: {}", voting.getVotingId());
        return Mono.just(voting);
    }

    /**
     * Constrói resposta de votação criada com URLs geradas.
     * <p>
     * Centraliza a criação do DTO de resposta incluindo geração
     * das URLs de votação e consulta de resultado.
     *
     * @param apiVersion versão da API
     * @param savedVoting votação salva no banco
     * @return DTO de resposta completo
     */
    private VotingResponseDTO buildVotingResponse(String apiVersion, Voting savedVoting) {
        String voteUrl = buildVoteUrl(apiVersion, savedVoting.getVotingId());
        String resultUrl = buildResultUrl(apiVersion, savedVoting.getVotingId());
        String formattedCloseDate = RESPONSE_DATE_FORMATTER.format(savedVoting.getCloseVotingDate());

        return new VotingResponseDTO(
                savedVoting.getVotingId(),
                voteUrl,
                formattedCloseDate,
                resultUrl
        );
    }

    /**
     * Constrói URL para endpoint de votação.
     *
     * @param apiVersion versão da API
     * @param votingId ID da votação
     * @return URL completa para votação
     */
    private String buildVoteUrl(String apiVersion, String votingId) {
        return LOCALHOST + serverPort + apiContext + "/" + VOTING_CONTEXT + "/" + apiVersion + "/" + votingId;
    }

    /**
     * Constrói URL para endpoint de resultado.
     *
     * @param apiVersion versão da API
     * @param votingId ID da votação
     * @return URL completa para consulta de resultado
     */
    private String buildResultUrl(String apiVersion, String votingId) {
        return LOCALHOST + serverPort + apiContext + "/" + RESULT_CONTEXT + "/" + apiVersion + "/" + votingId;
    }

    /**
     * Converte DTO de request para entidade de domínio.
     * <p>
     * Configura dados básicos da votação incluindo timestamps de abertura
     * e fechamento, com status inicial ativo.
     *
     * @param votingRequestDTO dados de entrada
     * @return entidade Voting configurada
     * @throws InvalidFormatExpirationDate se tempo de expiração for inválido
     */
    private Voting convertToCollection(VotingRequestDTO votingRequestDTO) {
        Instant openVotingDate = Instant.now();

        Voting voting = new Voting();
        voting.setSubject(votingRequestDTO.subject());
        voting.setOpenVotingDate(openVotingDate);
        voting.setCloseVotingDate(createExpirationDate(openVotingDate, votingRequestDTO.userDefinedExpirationDate()));
        voting.setVotingSatus(true);

        return voting;
    }

    /**
     * Calcula data de expiração da votação baseada em entrada do usuário.
     * <p>
     * **REGRAS DE NEGÓCIO:**
     * - Tempo padrão: 1 minuto se não especificado
     * - Valores <= 0: convertidos para padrão de 1 minuto
     * - Formato inválido: lança exceção específica
     * - Valor em branco/null: usa padrão
     *
     * @param openVotingDate timestamp de abertura da votação
     * @param userDefinedExpirationDate tempo em minutos definido pelo usuário
     * @return timestamp calculado para fechamento da votação
     * @throws InvalidFormatExpirationDate se formato do tempo for inválido
     */
    private Instant createExpirationDate(Instant openVotingDate, Integer userDefinedExpirationDate) {
        votingServiceLogger.info("Criando data de expiração para a votação");
        long minutes = parseExpirationMinutes(userDefinedExpirationDate);
        votingServiceLogger.info("Tempo de expiração definido (minutos): {}", minutes);
        return openVotingDate.plus(Duration.ofMinutes(minutes));
    }

    /**
     * Processa e valida tempo de expiração definido pelo usuário.
     * <p>
     * Aplica regras de negócio para conversão e validação do tempo,
     * incluindo fallback para valor padrão em casos de erro.
     *
     * @param userDefinedExpirationDate tempo em minutos como string
     * @return tempo validado em minutos
     * @throws InvalidFormatExpirationDate se formato for inválido
     */
    private long parseExpirationMinutes(Integer userDefinedExpirationDate) {
        if (userDefinedExpirationDate == null || userDefinedExpirationDate <= 0) {
            votingServiceLogger.warn("Tempo de expiração inválido ou nulo, ajustando para 1 minuto.");
            return DEFAULT_EXPIRATION_MINUTES;
        }
        return userDefinedExpirationDate;
    }
}


