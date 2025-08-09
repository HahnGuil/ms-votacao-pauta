package br.com.hahn.votacao.domain.service;

import br.com.hahn.votacao.domain.dto.request.VoteRequestDTO;
import br.com.hahn.votacao.domain.enums.VoteOption;
import br.com.hahn.votacao.domain.model.Vote;
import br.com.hahn.votacao.domain.respository.VoteRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoteService {

    private final KafkaTemplate<String, VoteRequestDTO> kafkaTemplate;
    private final VoteRepository voteRepository;

    public VoteService(KafkaTemplate<String, VoteRequestDTO> kafkaTemplate, VoteRepository voteRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.voteRepository = voteRepository;
    }

    public void sendVoteToQueue(VoteRequestDTO voteRequestDTO) {
        kafkaTemplate.send("vote-topic", voteRequestDTO);
    }

    public void saveAllFromDTO(List<VoteRequestDTO> voteRequestDTOs) {
        List<Vote> votes = voteRequestDTOs.stream()
                .map(this::convertToCollection)
                .toList();
        voteRepository.saveAll(votes);
    }

    private Vote convertToCollection(VoteRequestDTO voteRequestDTO) {
        Vote vote = new Vote();

        vote.setVotingId(voteRequestDTO.votingId());
        vote.setUserId(voteRequestDTO.userId());
        vote.setVoteOption(VoteOption.fromString(voteRequestDTO.voteOption()));

        return vote;
    }
}
