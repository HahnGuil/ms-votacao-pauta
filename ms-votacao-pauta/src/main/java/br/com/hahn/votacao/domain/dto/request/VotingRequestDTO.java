package br.com.hahn.votacao.domain.dto.request;

public record VotingRequestDTO(String subject, String userDefinedExpirationDate, String apiVersion) {

    public VotingRequestDTO withApiVersion(String apiVersion) {
        return new VotingRequestDTO(this.subject, this.userDefinedExpirationDate, apiVersion);
    }
}
