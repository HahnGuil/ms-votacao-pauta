package br.com.hahn.votacao.infrastructure.scheduling;

import br.com.hahn.votacao.infrastructure.service.VoteBatchConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class VoteBatchSchedulerTest {

    private VoteBatchConsumer voteBatchConsumer;
    private VoteBatchScheduler voteBatchScheduler;

    @BeforeEach
    void setUp() {
        voteBatchConsumer = mock(VoteBatchConsumer.class);
        voteBatchScheduler = new VoteBatchScheduler(voteBatchConsumer);
    }

    @Test
    void scheduledFlush_shouldCallConsumerScheduledFlush() {
        voteBatchScheduler.scheduledFlush();
        verify(voteBatchConsumer, times(1)).scheduledFlush();
    }
}

