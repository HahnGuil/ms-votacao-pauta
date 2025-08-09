package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.enums.VoteOption;
import br.com.hahn.votacao.domain.exception.UserAlreadyVoteException;
import br.com.hahn.votacao.domain.model.Vote;
import br.com.hahn.votacao.domain.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VoteServiceTest {

    @Mock
    private KafkaTemplate<String, VoteRequestDTO> kafkaTemplate;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private VotingService votingService;
    @Mock
    private ReactiveStringRedisTemplate redisTemplate;
    @Mock
    private ReactiveValueOperations<String, String> valueOps;

    @InjectMocks
    private VoteService voteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void sendVoteToQueue_shouldSendVote_whenUserHasNotVotedAndRedisIsNew() {
        VoteRequestDTO dto = new VoteRequestDTO("voting1", "user1", "SIM");
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("voting1", "user1");
        when(valueOps.setIfAbsent(anyString(), eq("pending"), any(Duration.class))).thenReturn(Mono.just(true));
        when(votingService.validateExpireVotingTime("voting1")).thenReturn(Mono.empty());
        when(kafkaTemplate.send(anyString(), any(VoteRequestDTO.class))).thenReturn(null);

        StepVerifier.create(voteService.sendVoteToQueue(dto))
                .verifyComplete();

        verify(kafkaTemplate, times(1)).send("vote-topic", dto);
    }

    @Test
    void sendVoteToQueue_shouldThrow_whenVotingTimeValidationFails() {
        VoteRequestDTO dto = new VoteRequestDTO("voting1", "user1", "SIM");
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("voting1", "user1");
        when(valueOps.setIfAbsent(anyString(), eq("pending"), any(Duration.class))).thenReturn(Mono.just(true));
        when(votingService.validateExpireVotingTime("voting1")).thenReturn(Mono.error(new RuntimeException("Voting time expired")));

        StepVerifier.create(voteService.sendVoteToQueue(dto))
                .expectError(RuntimeException.class)
                .verify();

        verify(kafkaTemplate, never()).send(anyString(), any(VoteRequestDTO.class));
    }

    @Test
    void sendVoteToQueue_shouldThrow_whenUserAlreadyVotedInDb() {
        VoteRequestDTO dto = new VoteRequestDTO("voting1", "user1", "SIM");
        doReturn(Mono.just(new Vote())).when(voteRepository).findByVotingIdAndUserId("voting1", "user1");

        StepVerifier.create(voteService.sendVoteToQueue(dto))
                .expectError(UserAlreadyVoteException.class)
                .verify();
    }

    @Test
    void sendVoteToQueue_shouldThrow_whenUserAlreadyVotedInRedis() {
        VoteRequestDTO dto = new VoteRequestDTO("voting1", "user1", "SIM");
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("voting1", "user1");
        when(valueOps.setIfAbsent(anyString(), eq("pending"), any(Duration.class))).thenReturn(Mono.just(false));

        StepVerifier.create(voteService.sendVoteToQueue(dto))
                .expectError(UserAlreadyVoteException.class)
                .verify();
    }

    @Test
    void hasUserAlreadyVoted_shouldReturnTrue_whenVoteExists() {
        doReturn(Mono.just(new Vote())).when(voteRepository).findByVotingIdAndUserId("voting1", "user1");

        StepVerifier.create(voteService.hasUserAlreadyVoted("voting1", "user1"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasUserAlreadyVoted_shouldReturnFalse_whenVoteNotExists() {
        doReturn(Mono.empty()).when(voteRepository).findByVotingIdAndUserId("voting1", "user1");

        StepVerifier.create(voteService.hasUserAlreadyVoted("voting1", "user1"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void saveAllFromDTO_shouldSaveAllVotes() {
        VoteRequestDTO dto1 = new VoteRequestDTO("voting1", "user1", "SIM");
        VoteRequestDTO dto2 = new VoteRequestDTO("voting1", "user2", "NAO");
        Vote vote1 = new Vote();
        vote1.setVotingId("voting1");
        vote1.setUserId("user1");
        vote1.setVoteOption(VoteOption.SIM);
        Vote vote2 = new Vote();
        vote2.setVotingId("voting1");
        vote2.setUserId("user2");
        vote2.setVoteOption(VoteOption.NAO);

        doReturn(Flux.just(vote1, vote2)).when(voteRepository).saveAll(any(List.class));

        StepVerifier.create(voteService.saveAllFromDTO(Flux.just(dto1, dto2)))
                .expectNext(vote1)
                .expectNext(vote2)
                .verifyComplete();
    }

    @Test
    void convertToCollection_shouldConvertDTOToVote() {
        VoteRequestDTO dto = new VoteRequestDTO("voting1", "user1", "SIM");
        Vote expectedVote = new Vote();
        expectedVote.setVotingId("voting1");
        expectedVote.setUserId("user1");
        expectedVote.setVoteOption(VoteOption.SIM);

        when(voteRepository.saveAll(any(List.class))).thenReturn(Flux.just(expectedVote));

        Vote vote = voteService.saveAllFromDTO(Flux.just(dto)).blockFirst();
        assertNotNull(vote);
        assertEquals("voting1", vote.getVotingId());
        assertEquals("user1", vote.getUserId());
        assertEquals(VoteOption.SIM, vote.getVoteOption());
    }
}

