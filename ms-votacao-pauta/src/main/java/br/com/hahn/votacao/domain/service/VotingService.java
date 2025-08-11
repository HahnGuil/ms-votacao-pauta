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

@Service
public class VotingService {

    private static final Logger votingServiceLogger = LoggerFactory.getLogger(VotingService.class);

    private final VotingRepository votingRepository;

    public VotingService(VotingRepository votingRepository) {
        this.votingRepository = votingRepository;
    }

    @Value("${server.port}")
    private String serverPort;

    @Value("${spring.webflux.base-path}")
    private String apiContext;



    private static final String VOTING_CONTEXT = "/vote";
    private static final String RESULT_CONTEXT = "/result";

    public Mono<VotingResponseDTO> createVoting(VotingRequestDTO votingRequestDTO) {
        votingServiceLogger.info("Criando uma nova votação");
        Voting voting = convertToCollection(votingRequestDTO);
        return votingRepository.save(voting)
                .map(savedVoting -> {
                    String voteUrl = "http://localhost:" + serverPort + apiContext + "/"  + votingRequestDTO.apiVersion() +  VOTING_CONTEXT + "/" + savedVoting.getVotingId();
                    String resultUrl = "http://localhost:" + serverPort + apiContext + "/" + votingRequestDTO.apiVersion() + RESULT_CONTEXT + "/" + savedVoting.getVotingId();
                    return new VotingResponseDTO(savedVoting.getVotingId(), voteUrl, savedVoting.getCloseVotingDate(), resultUrl);
                });
    }

    public Flux<Voting> findAllVotings() {
        return votingRepository.findAll();
    }

    public Mono<Voting> findById(String votingId) {
        return votingRepository.findById(votingId);
    }

    public Mono<Voting> saveVoting(Voting voting) {
        return votingRepository.save(voting);
    }

    public Mono<Void> validateExpireVotingTime(String votingId) {
        votingServiceLogger.info("Validando se a votação ainda está ativa");
        return votingRepository.findById(votingId)
                .switchIfEmpty(Mono.error(new VotingNotFoundException("Voting not found for this " + votingId)))
                .flatMap(voting -> {
                    // Primeiro verifica se a votação está ativa
                    if (!voting.isVotingSatus()) {
                        votingServiceLogger.warn("Votação está inativa: {}", votingId);
                        return Mono.error(new VotingExpiredException("This voting is inactive and no longer accepts votes."));
                    }

                    // Depois verifica se a votação não expirou por tempo
                    Instant now = Instant.now();
                    if (now.isAfter(voting.getCloseVotingDate())) {
                        votingServiceLogger.warn("Votação expirou por tempo: {}", votingId);
                        return Mono.error(new VotingExpiredException("This voting has expired, you can no longer vote."));
                    }

                    votingServiceLogger.info("Votação válida e ativa: {}", votingId);
                    return Mono.empty();
                });
    }

    private Voting convertToCollection(VotingRequestDTO votingRequestDTO){
        Instant openVotingDate = Instant.now();

        Voting voting = new Voting();
        voting.setSubject(votingRequestDTO.subject());
        voting.setOpenVotingDate(openVotingDate);
        voting.setCloseVotingDate(createExpirationDate(openVotingDate, votingRequestDTO.userDefinedExpirationDate()));
        voting.setVotingSatus(true);

        return voting;
    }

    private Instant createExpirationDate(Instant openVotingDate, String userDefinedExpirationDate) {
        votingServiceLogger.info("Criando data de expiração para a votação");
        final long defaultMinutes = 1L;
        long minutes = defaultMinutes;

        if (userDefinedExpirationDate != null && !userDefinedExpirationDate.isBlank()) {
            try {
                minutes = Long.parseLong(userDefinedExpirationDate);
                if (minutes <= 0) {
                    minutes = defaultMinutes;
                }
            } catch (NumberFormatException _) {
                throw new InvalidFormatExpirationDate("Invalid time format, poll timeout set to 1 minute.");
            }
        }

        return openVotingDate.plus(Duration.ofMinutes(minutes));
    }
}


