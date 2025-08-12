package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.VotingRequestDTO;
import br.com.hahn.votacao.domain.dto.response.VotingResponseDTO;
import br.com.hahn.votacao.domain.exception.InvalidFormatExpirationDate;
import br.com.hahn.votacao.domain.exception.VotingExpiredException;
import br.com.hahn.votacao.domain.exception.VotingNotFoundException;
import br.com.hahn.votacao.domain.model.Voting;
import br.com.hahn.votacao.domain.repository.VotingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VotingServiceTest {

    private VotingRepository votingRepository;
    private VotingService votingService;

    @BeforeEach
    void setUp() {
        votingRepository = mock(VotingRepository.class);
        votingService = new VotingService(votingRepository);
        ReflectionTestUtils.setField(votingService, "serverPort", "8080");
        ReflectionTestUtils.setField(votingService, "apiContext", "");
    }

    @Test
    void createVoting_shouldSaveAndReturnResponse() {
        VotingRequestDTO dto = new VotingRequestDTO("subject", "5", "v1");
        Voting voting = new Voting();
        voting.setVotingId("id123");
        voting.setSubject("subject");
        voting.setCloseVotingDate(Instant.now().plusSeconds(300));
        when(votingRepository.save(any(Voting.class))).thenReturn(Mono.just(voting));

        Mono<VotingResponseDTO> result = votingService.createVoting(dto);

        StepVerifier.create(result)
                .assertNext(resp -> {
                    assertEquals("id123", resp.votingId());
                    assertTrue(resp.voteUrl().contains("8080/vote/v1/id123"));
                    assertTrue(resp.resultUrl().contains("8080/result/v1/id123"));
                })
                .verifyComplete();

        verify(votingRepository).save(any(Voting.class));
    }

    @Test
    void testCreateVoting_Success() {
        VotingRequestDTO request = new VotingRequestDTO("Assunto Teste", "5", "v1");
        Voting voting = new Voting();
        voting.setVotingId("abc123");
        voting.setSubject("Assunto Teste");
        voting.setOpenVotingDate(Instant.now());
        voting.setCloseVotingDate(Instant.now().plusSeconds(300));

        when(votingRepository.save(any(Voting.class))).thenReturn(Mono.just(voting));

        Mono<VotingResponseDTO> responseMono = votingService.createVoting(request);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("abc123", response.votingId());
                    assertEquals("http://localhost:8080/vote/v1/abc123", response.voteUrl());
                    assertEquals(voting.getCloseVotingDate(), response.closeVotingDate());
                })
                .verifyComplete();

        verify(votingRepository).save(any(Voting.class));
    }

    @Test
    void testCreateVoting_InvalidExpirationDate_ThrowsException() {
        VotingRequestDTO request = new VotingRequestDTO("Assunto Teste", "invalid", "v1");

        InvalidFormatExpirationDate exception = assertThrows(InvalidFormatExpirationDate.class, () -> {
            votingService.createVoting(request);
        });

        assertEquals("Invalid time format, poll timeout set to 1 minute.", exception.getMessage());
    }

    @Test
    void testCreateVoting_NullExpirationDate_UsesDefault() {
        VotingRequestDTO request = new VotingRequestDTO("Assunto Teste", null, "v1");
        Voting voting = new Voting();
        voting.setVotingId("def456");
        voting.setSubject("Assunto Teste");
        voting.setOpenVotingDate(Instant.now());
        voting.setCloseVotingDate(voting.getOpenVotingDate().plusSeconds(60));

        when(votingRepository.save(any(Voting.class))).thenReturn(Mono.just(voting));

        Mono<VotingResponseDTO> responseMono = votingService.createVoting(request);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("def456", response.votingId());
                    assertEquals("http://localhost:8080/vote/v1/def456", response.voteUrl());
                    assertEquals(voting.getCloseVotingDate(), response.closeVotingDate());
                })
                .verifyComplete();

        verify(votingRepository).save(any(Voting.class));
    }

    @Test
    void testCreateVoting_ZeroExpirationDate_UsesDefault() {
        VotingRequestDTO request = new VotingRequestDTO("Assunto Teste", "0", "v1");
        Voting voting = new Voting();
        voting.setVotingId("zeroId");
        voting.setSubject("Assunto Teste");
        voting.setOpenVotingDate(Instant.now());
        voting.setCloseVotingDate(voting.getOpenVotingDate().plusSeconds(60));

        when(votingRepository.save(any(Voting.class))).thenReturn(Mono.just(voting));

        Mono<VotingResponseDTO> responseMono = votingService.createVoting(request);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("zeroId", response.votingId());
                    assertEquals("http://localhost:8080/vote/v1/zeroId", response.voteUrl());
                    assertEquals(voting.getCloseVotingDate(), response.closeVotingDate());
                })
                .verifyComplete();

        verify(votingRepository).save(any(Voting.class));
    }

    @Test  // Adicionada anotação @Test que estava faltando
    void testCreateVoting_NegativeExpirationDate_UsesDefault() {
        VotingRequestDTO request = new VotingRequestDTO("Assunto Teste", "-10", "v1");
        Voting voting = new Voting();
        voting.setVotingId("negId");
        voting.setSubject("Assunto Teste");
        voting.setOpenVotingDate(Instant.now());
        voting.setCloseVotingDate(voting.getOpenVotingDate().plusSeconds(60));

        when(votingRepository.save(any(Voting.class))).thenReturn(Mono.just(voting));

        Mono<VotingResponseDTO> responseMono = votingService.createVoting(request);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("negId", response.votingId());
                    assertEquals("http://localhost:8080/vote/v1/negId", response.voteUrl()); // Corrigida URL para incluir v1
                    assertEquals(voting.getCloseVotingDate(), response.closeVotingDate());
                })
                .verifyComplete();

        verify(votingRepository).save(any(Voting.class));
    }

    @Test
    void testValidateExpireVotingTime_NotExpired() {
        Voting voting = new Voting();
        voting.setVotingId("notExpiredId");
        voting.setCloseVotingDate(Instant.now().plusSeconds(120));
        voting.setVotingSatus(true);

        when(votingRepository.findById("notExpiredId")).thenReturn(Mono.just(voting));

        Mono<Void> result = votingService.validateExpireVotingTime("notExpiredId");

        StepVerifier.create(result)
                .verifyComplete();

        verify(votingRepository, times(1)).findById("notExpiredId");
    }

    @Test
    void testValidateExpireVotingTime_Expired_ThrowsException() {
        Voting voting = new Voting();
        voting.setVotingId("expiredId");
        voting.setCloseVotingDate(Instant.now().minusSeconds(10));
        voting.setVotingSatus(false);

        when(votingRepository.findById("expiredId")).thenReturn(Mono.just(voting));

        Mono<Void> result = votingService.validateExpireVotingTime("expiredId");

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof VotingExpiredException &&
                                throwable.getMessage().equals("This voting is inactive and no longer accepts votes.")
                )
                .verify();

        verify(votingRepository, times(1)).findById("expiredId");
    }

    @Test
    void testValidateExpireVotingTime_VotingNotFound_ThrowsException() {
        when(votingRepository.findById("notFoundId")).thenReturn(Mono.empty());

        Mono<Void> result = votingService.validateExpireVotingTime("notFoundId");

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof VotingNotFoundException &&
                                throwable.getMessage().equals("Voting not found for this notFoundId")
                )
                .verify();

        verify(votingRepository, times(1)).findById("notFoundId");
    }

    @Test
    void findAllVotings_shouldReturnAll() {
        Voting voting = new Voting();
        when(votingRepository.findAll()).thenReturn(Flux.just(voting));

        Flux<Voting> result = votingService.findAllVotings();

        StepVerifier.create(result)
                .expectNext(voting)
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnVoting() {
        Voting voting = new Voting();
        when(votingRepository.findById("id")).thenReturn(Mono.just(voting));

        Mono<Voting> result = votingService.findById("id");

        StepVerifier.create(result)
                .expectNext(voting)
                .verifyComplete();
    }

    @Test
    void saveVoting_shouldSaveVoting() {
        Voting voting = new Voting();
        when(votingRepository.save(voting)).thenReturn(Mono.just(voting));

        Mono<Voting> result = votingService.saveVoting(voting);

        StepVerifier.create(result)
                .expectNext(voting)
                .verifyComplete();
    }

    @Test
    void convertToCollection_shouldConvertDTOToVoting() throws Exception {
        VotingRequestDTO dto = new VotingRequestDTO("subject", "10", "v1");
        Method method = VotingService.class.getDeclaredMethod("convertToCollection", VotingRequestDTO.class);
        method.setAccessible(true);
        Voting voting = (Voting) method.invoke(votingService, dto);

        assertEquals("subject", voting.getSubject());
        assertNotNull(voting.getOpenVotingDate());
        assertNotNull(voting.getCloseVotingDate());
        assertTrue(voting.isVotingSatus());
    }

    @Test
    void createExpirationDate_shouldReturnDefault_whenInputIsNullOrBlank() throws Exception {
        Instant now = Instant.now();
        Method method = VotingService.class.getDeclaredMethod("createExpirationDate", Instant.class, String.class);
        method.setAccessible(true);

        Instant result1 = (Instant) method.invoke(votingService, now, null);
        Instant result2 = (Instant) method.invoke(votingService, now, "");

        assertEquals(now.plusSeconds(60), result1);
        assertEquals(now.plusSeconds(60), result2);
    }

    @Test
    void createExpirationDate_shouldReturnCustomMinutes_whenInputIsValid() throws Exception {
        Instant now = Instant.now();
        Method method = VotingService.class.getDeclaredMethod("createExpirationDate", Instant.class, String.class);
        method.setAccessible(true);

        Instant result = (Instant) method.invoke(votingService, now, "5");

        assertEquals(now.plusSeconds(300), result);
    }

    @Test
    void createExpirationDate_shouldReturnDefault_whenInputIsZeroOrNegative() throws Exception {
        Instant now = Instant.now();
        Method method = VotingService.class.getDeclaredMethod("createExpirationDate", Instant.class, String.class);
        method.setAccessible(true);

        Instant result1 = (Instant) method.invoke(votingService, now, "0");
        Instant result2 = (Instant) method.invoke(votingService, now, "-10");

        assertEquals(now.plusSeconds(60), result1);
        assertEquals(now.plusSeconds(60), result2);
    }

    @Test
    void createExpirationDate_shouldThrowException_whenInputIsInvalidFormat() throws Exception {
        Instant now = Instant.now();
        Method method = VotingService.class.getDeclaredMethod("createExpirationDate", Instant.class, String.class);
        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () ->
                method.invoke(votingService, now, "abc")
        );

        assertInstanceOf(InvalidFormatExpirationDate.class, exception.getCause());
        assertEquals("Invalid time format, poll timeout set to 1 minute.", exception.getCause().getMessage());
    }

    @Test
    void createVoting_shouldError_whenRepositoryFails() {
        VotingRequestDTO dto = new VotingRequestDTO("subject", "5", "v1");
        when(votingRepository.save(any(Voting.class)))
                .thenReturn(Mono.error(new RuntimeException("Database connection error")));

        Mono<VotingResponseDTO> result = votingService.createVoting(dto);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void findAllVotings_shouldError_whenRepositoryFails() {
        when(votingRepository.findAll())
                .thenReturn(Flux.error(new RuntimeException("Database error")));

        Flux<Voting> result = votingService.findAllVotings();

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void findById_shouldError_whenRepositoryFails() {
        when(votingRepository.findById("id"))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        Mono<Voting> result = votingService.findById("id");

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void saveVoting_shouldError_whenRepositoryFails() {
        Voting voting = new Voting();
        when(votingRepository.save(voting))
                .thenReturn(Mono.error(new RuntimeException("Save failed")));

        Mono<Voting> result = votingService.saveVoting(voting);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void validateExpireVotingTime_shouldError_whenRepositoryFails() {
        when(votingRepository.findById("votingId"))
                .thenReturn(Mono.error(new RuntimeException("Database connection error")));

        Mono<Void> result = votingService.validateExpireVotingTime("votingId");

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void validateExpireVotingTime_shouldError_whenVotingExpiredByTime() {
        Voting voting = new Voting();
        voting.setVotingId("expiredTimeId");
        voting.setVotingSatus(true);
        voting.setCloseVotingDate(Instant.now().minusSeconds(60));

        when(votingRepository.findById("expiredTimeId")).thenReturn(Mono.just(voting));

        Mono<Void> result = votingService.validateExpireVotingTime("expiredTimeId");

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof VotingExpiredException &&
                                throwable.getMessage().equals("This voting has expired, you can no longer vote.")
                )
                .verify();
    }

    @Test
    void parseExpirationMinutes_shouldReturnDefault_whenInputIsBlank() throws Exception {
        Method method = VotingService.class.getDeclaredMethod("parseExpirationMinutes", String.class);
        method.setAccessible(true);

        long result = (long) method.invoke(votingService, "   ");

        assertEquals(1L, result);
    }

    @Test
    void buildVoteUrl_shouldGenerateCorrectUrl() throws Exception {
        ReflectionTestUtils.setField(votingService, "apiContext", "/api");
        Method method = VotingService.class.getDeclaredMethod("buildVoteUrl", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(votingService, "v1", "voting123");

        assertEquals("http://localhost:8080/api/vote/v1/voting123", result);
    }

    @Test
    void buildResultUrl_shouldGenerateCorrectUrl() throws Exception {
        ReflectionTestUtils.setField(votingService, "apiContext", "/api");
        Method method = VotingService.class.getDeclaredMethod("buildResultUrl", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(votingService, "v1", "voting123");

        assertEquals("http://localhost:8080/api/result/v1/voting123", result);
    }
}