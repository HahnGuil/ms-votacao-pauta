package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.enums.VoteOption;
import br.com.hahn.votacao.domain.exception.UserAlreadyVoteException;
import br.com.hahn.votacao.domain.model.Vote;
import br.com.hahn.votacao.domain.repository.VoteRepository;
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

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        voteRepository = mock(VoteRepository.class);
        votingService = mock(VotingService.class);
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        valueOps = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        voteService = new VoteService(kafkaTemplate, voteRepository, votingService, redisTemplate);
    }

    @Test
    void sendVoteToQueue_shouldSendVote_whenUserHasNotVotedAndRedisIsNew() {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM");
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("votingId", "userId");
        when(valueOps.setIfAbsent(anyString(), eq("pending"), any(Duration.class))).thenReturn(Mono.just(true));
        when(votingService.validateExpireVotingTime("votingId")).thenReturn(Mono.empty());

        Mono<Void> result = voteService.sendVoteToQueue(dto);

        StepVerifier.create(result)
                .verifyComplete();

        verify(kafkaTemplate).send("vote-topic", dto);
    }

    @Test
    void sendVoteToQueue_shouldError_whenUserAlreadyVotedInDb() {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM");
        doReturn(Mono.just(new Vote())).when(voteRepository).findByVotingIdAndUserId("votingId", "userId");

        Mono<Void> result = voteService.sendVoteToQueue(dto);

        StepVerifier.create(result)
                .expectError(UserAlreadyVoteException.class)
                .verify();
    }

    @Test
    void sendVoteToQueue_shouldError_whenUserAlreadyVotedInRedis() {
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM");
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
        VoteRequestDTO dto1 = new VoteRequestDTO("votingId1", "userId1", "SIM");
        VoteRequestDTO dto2 = new VoteRequestDTO("votingId2", "userId2", "NAO");
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
        VoteRequestDTO dto = new VoteRequestDTO("votingId", "userId", "SIM");
        Method method = VoteService.class.getDeclaredMethod("convertToCollection", VoteRequestDTO.class);
        method.setAccessible(true);
        Vote vote = (Vote) method.invoke(voteService, dto);

        assertEquals("votingId", vote.getVotingId());
        assertEquals("userId", vote.getUserId());
        assertEquals(VoteOption.SIM, vote.getVoteOption());
    }
}
