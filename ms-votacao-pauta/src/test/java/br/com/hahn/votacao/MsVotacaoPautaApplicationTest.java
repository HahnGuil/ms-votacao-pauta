package br.com.hahn.votacao;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MsVotacaoPautaApplicationTest {

    @Test
    void applicationStartupListener_shouldLogStartupInfo() throws Exception {
        MsVotacaoPautaApplication.ApplicationStartupListener listener = new MsVotacaoPautaApplication.ApplicationStartupListener();
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        ConfigurableEnvironment env = mock(ConfigurableEnvironment.class);

        when(event.getApplicationContext()).thenReturn(context);
        when(context.getEnvironment()).thenReturn(env);
        when(env.getProperty("server.port", "8080")).thenReturn("8080");
        when(context.getBeansOfType(org.springframework.data.mongodb.core.ReactiveMongoTemplate.class)).thenReturn(java.util.Collections.emptyMap());

        assertDoesNotThrow(() -> listener.onApplicationEvent(event));

        verify(event, times(2)).getApplicationContext();
        verify(env, times(1)).getProperty("server.port", "8080");
    }

}

