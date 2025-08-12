package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.dto.response.CpfValidationResponseDTO;
import br.com.hahn.votacao.domain.enums.CpfStatus;
import br.com.hahn.votacao.domain.enums.VoteOption;
import br.com.hahn.votacao.domain.exception.InvalidCpfException;
import br.com.hahn.votacao.domain.exception.UserAlreadyVoteException;
import br.com.hahn.votacao.domain.exception.UserNotFoundException;
import br.com.hahn.votacao.domain.model.User;
import br.com.hahn.votacao.domain.model.Vote;
import br.com.hahn.votacao.domain.repository.VoteRepository;
import br.com.hahn.votacao.infrastructure.client.CpfValidationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VoteServiceTest {

    private KafkaTemplate<String, VoteRequestDTO> kafkaTemplate;
    private VoteRepository voteRepository;
    private VotingService votingService;
    private ReactiveStringRedisTemplate redisTemplate;
    private ReactiveValueOperations<String, String> valueOps;
    private VoteService voteService;
    private UserService userService;
    private CpfValidationClient cpfValidationClient;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        voteRepository = mock(VoteRepository.class);
        votingService = mock(VotingService.class);
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        valueOps = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        userService = mock(UserService.class);
        cpfValidationClient = mock(CpfValidationClient.class);
        voteService = new VoteService(kafkaTemplate, voteRepository, votingService, redisTemplate, userService, cpfValidationClient);

        // Corrige o mock para sempre retornar Mono.empty()
        when(votingService.validateExpireVotingTime(anyString())).thenReturn(Mono.empty());
    }

    @Test
    void sendVoteToQueue_shouldSendVote_whenUserHasNotVotedAndRedisIsNew() {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM", "v1");
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("votingId", "userId");
        when(valueOps.setIfAbsent(anyString(), eq("pending"), any(Duration.class))).thenReturn(Mono.just(true));

        User user = new User();
        user.setUserCPF("12345678900");
        when(userService.findById("userId")).thenReturn(Mono.just(user));
        CpfValidationResponseDTO response = new CpfValidationResponseDTO(CpfStatus.ABLE_TO_VOTE);
        when(cpfValidationClient.validateCpf("12345678900")).thenReturn(Mono.just(response));

        Mono<Void> result = voteService.sendVoteToQueue(dto);

        StepVerifier.create(result)
                .verifyComplete();

        verify(kafkaTemplate).send("vote-topic", dto);
        verify(valueOps).setIfAbsent("votingId:userId", "pending", Duration.ofMinutes(5));
    }

    @Test
    void sendVoteToQueue_shouldError_whenUserAlreadyVotedInRedis() {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM", "v1");
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("votingId", "userId");
        when(valueOps.setIfAbsent(anyString(), eq("pending"), any(Duration.class))).thenReturn(Mono.just(false));

        Mono<Void> result = voteService.sendVoteToQueue(dto);

        StepVerifier.create(result)
                .expectError(UserAlreadyVoteException.class)
                .verify();
    }

    @Test
    void hasUserAlreadyVoted_shouldReturnTrue_whenVoteExists() {
        doReturn(Mono.just(new Vote())).when(voteRepository).findByVotingIdAndUserId("votingId", "userId");

        Mono<Boolean> result = voteService.hasUserAlreadyVoted("votingId", "userId");

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasUserAlreadyVoted_shouldReturnFalse_whenVoteDoesNotExist() {
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("votingId", "userId");

        Mono<Boolean> result = voteService.hasUserAlreadyVoted("votingId", "userId");

        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void saveAllFromDTO_shouldConvertAndSaveAllVotes() {
        VoteRequestDTO dto1 = new VoteRequestDTO("votingId1", "userId1", "SIM", "v1");
        VoteRequestDTO dto2 = new VoteRequestDTO("votingId2", "userId2", "NAO", "v1");
        Vote vote1 = new Vote();
        vote1.setVotingId("votingId1");
        vote1.setUserId("userId1");
        vote1.setVoteOption(VoteOption.SIM);
        Vote vote2 = new Vote();
        vote2.setVotingId("votingId2");
        vote2.setUserId("userId2");
        vote2.setVoteOption(VoteOption.NAO);

        doReturn(Flux.fromIterable(List.of(vote1, vote2))).when(voteRepository).saveAll(anyList());

        Flux<Vote> result = voteService.saveAllFromDTO(Flux.just(dto1, dto2));

        StepVerifier.create(result)
                .expectNext(vote1)
                .expectNext(vote2)
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Vote>> captor = ArgumentCaptor.forClass(List.class);
        verify(voteRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals("votingId1", captor.getValue().get(0).getVotingId());
        assertEquals("votingId2", captor.getValue().get(1).getVotingId());
    }

    @Test
    void findByVotingId_shouldReturnVotes() {
        Vote vote = new Vote();
        vote.setVotingId("votingId");
        doReturn(Flux.just(vote)).when(voteRepository).findByVotingId("votingId");

        Flux<Vote> result = voteService.findByVotingId("votingId");

        StepVerifier.create(result)
                .expectNext(vote)
                .verifyComplete();
    }

    @Test
    void convertToCollection_shouldConvertDTOToVote() throws Exception {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM", "v1");
        Method method = VoteService.class.getDeclaredMethod("convertToCollection", VoteRequestDTO.class);
        method.setAccessible(true);
        Vote vote = (Vote) method.invoke(voteService, dto);

        assertEquals("votingId", vote.getVotingId());
        assertEquals("userId", vote.getUserId());
        assertEquals(VoteOption.SIM, vote.getVoteOption());
    }

    @Test
    void sendVoteToQueue_shouldError_whenUserNotFound() {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM", "v1");
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("votingId", "userId");
        when(valueOps.setIfAbsent(anyString(), eq("pending"), any(Duration.class))).thenReturn(Mono.just(true));
        when(userService.findById("userId")).thenReturn(Mono.empty());

        Mono<Void> result = voteService.sendVoteToQueue(dto);

        StepVerifier.create(result)
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    void sendVoteToQueue_shouldError_whenUserCpfIsEmpty() {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM", "v1");
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("votingId", "userId");
        when(valueOps.setIfAbsent(anyString(), eq("pending"), any(Duration.class))).thenReturn(Mono.just(true));
        User user = new User();
        user.setUserCPF("");
        when(userService.findById("userId")).thenReturn(Mono.just(user));

        Mono<Void> result = voteService.sendVoteToQueue(dto);

        StepVerifier.create(result)
                .expectError(InvalidCpfException.class)
                .verify();
    }

    @Test
    void sendVoteToQueue_shouldError_whenUserCpfIsNull() {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM", "v1");
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("votingId", "userId");
        when(valueOps.setIfAbsent(anyString(), eq("pending"), any(Duration.class))).thenReturn(Mono.just(true));
        User user = new User();
        user.setUserCPF(null);
        when(userService.findById("userId")).thenReturn(Mono.just(user));

        Mono<Void> result = voteService.sendVoteToQueue(dto);

        StepVerifier.create(result)
                .expectError(InvalidCpfException.class)
                .verify();
    }

    @Test
    void sendVoteToQueue_shouldError_whenCpfUnableToVote() {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM", "v1");
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("votingId", "userId");
        when(valueOps.setIfAbsent(anyString(), eq("pending"), any(Duration.class))).thenReturn(Mono.just(true));
        User user = new User();
        user.setUserCPF("12345678900");
        when(userService.findById("userId")).thenReturn(Mono.just(user));
        CpfValidationResponseDTO response = new CpfValidationResponseDTO(CpfStatus.UNABLE_TO_VOTE);
        when(cpfValidationClient.validateCpf("12345678900")).thenReturn(Mono.just(response));

        Mono<Void> result = voteService.sendVoteToQueue(dto);

        StepVerifier.create(result)
                .expectError(InvalidCpfException.class)
                .verify();
    }

    @Test
    void sendVoteToQueue_shouldComplete_whenCpfAbleToVote() {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM", "v1");
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("votingId", "userId");
        when(valueOps.setIfAbsent(anyString(), eq("pending"), any(Duration.class))).thenReturn(Mono.just(true));

        User user = new User();
        user.setUserCPF("98765432100");
        when(userService.findById("userId")).thenReturn(Mono.just(user));
        CpfValidationResponseDTO response = new CpfValidationResponseDTO(CpfStatus.ABLE_TO_VOTE);
        when(cpfValidationClient.validateCpf("98765432100")).thenReturn(Mono.just(response));

        Mono<Void> result = voteService.sendVoteToQueue(dto);

        StepVerifier.create(result)
                .verifyComplete();

        verify(kafkaTemplate).send("vote-topic", dto);
        verify(cpfValidationClient).validateCpf("98765432100");
    }

    @Test
    void getUserCpf_shouldError_whenUserNotFound() throws Exception {
        Method method = VoteService.class.getDeclaredMethod("getUserCpf", String.class);
        method.setAccessible(true);
        when(userService.findById("userId")).thenReturn(Mono.empty());

        Mono<String> mono = (Mono<String>) method.invoke(voteService, "userId");

        StepVerifier.create(mono)
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    void getUserCpf_shouldError_whenCpfIsEmpty() throws Exception {
        Method method = VoteService.class.getDeclaredMethod("getUserCpf", String.class);
        method.setAccessible(true);
        User user = new User();
        user.setUserCPF("");
        when(userService.findById("userId")).thenReturn(Mono.just(user));

        Mono<String> mono = (Mono<String>) method.invoke(voteService, "userId");

        StepVerifier.create(mono)
                .expectError(InvalidCpfException.class)
                .verify();
    }

    @Test
    void getUserCpf_shouldReturnCpf_whenCpfIsValid() throws Exception {
        Method method = VoteService.class.getDeclaredMethod("getUserCpf", String.class);
        method.setAccessible(true);
        User user = new User();
        user.setUserCPF("12345678900");
        when(userService.findById("userId")).thenReturn(Mono.just(user));

        Mono<String> mono = (Mono<String>) method.invoke(voteService, "userId");

        StepVerifier.create(mono)
                .expectNext("12345678900")
                .verifyComplete();
    }

    @Test
    void sendVoteToQueue_shouldError_whenCpfValidationClientFails() {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM", "v1");
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("votingId", "userId");
        when(valueOps.setIfAbsent(anyString(), eq("pending"), any(Duration.class))).thenReturn(Mono.just(true));

        User user = new User();
        user.setUserCPF("12345678900");
        when(userService.findById("userId")).thenReturn(Mono.just(user));
        when(cpfValidationClient.validateCpf("12345678900"))
                .thenReturn(Mono.error(new RuntimeException("External service unavailable")));

        Mono<Void> result = voteService.sendVoteToQueue(dto);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void sendVoteToQueue_shouldError_whenRedisOperationFails() {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM", "v1");
        when(votingService.validateExpireVotingTime("votingId")).thenReturn(Mono.empty());
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("votingId", "userId");
        when(valueOps.setIfAbsent(anyString(), eq("pending"), any(Duration.class)))
                .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

        Mono<Void> result = voteService.sendVoteToQueue(dto);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void hasUserAlreadyVoted_shouldError_whenRepositoryFails() {
        when(voteRepository.findByVotingIdAndUserId("votingId", "userId"))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        Mono<Boolean> result = voteService.hasUserAlreadyVoted("votingId", "userId");

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void saveAllFromDTO_shouldError_whenRepositoryFails() {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM", "v1");
        when(voteRepository.saveAll(anyList()))
                .thenReturn(Flux.error(new RuntimeException("Save operation failed")));

        Flux<Vote> result = voteService.saveAllFromDTO(Flux.just(dto));

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void findByVotingId_shouldError_whenRepositoryFails() {
        when(voteRepository.findByVotingId("votingId"))
                .thenReturn(Flux.error(new RuntimeException("Database error")));

        Flux<Vote> result = voteService.findByVotingId("votingId");

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void convertToCollection_shouldHandleNaoVoteOption() throws Exception {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "NAO", "v1");
        Method method = VoteService.class.getDeclaredMethod("convertToCollection", VoteRequestDTO.class);
        method.setAccessible(true);

        Vote vote = (Vote) method.invoke(voteService, dto);

        assertEquals("votingId", vote.getVotingId());
        assertEquals("userId", vote.getUserId());
        assertEquals(VoteOption.NAO, vote.getVoteOption());
    }
}
