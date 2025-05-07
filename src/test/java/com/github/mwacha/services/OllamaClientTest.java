package com.github.mwacha.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OllamaClientTest {

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    @InjectMocks
    private OllamaClient ollamaClient;

    @BeforeEach
    void setUp() {
        // Configuração inicial não necessária com RETURNS_DEEP_STUBS
    }

    @Test
    void testAskSuccess() {
        // Arrange
        String prompt = "Test prompt";
        Map<String, Object> response1 = Map.of("response", "Part1", "done", false);
        Map<String, Object> response2 = Map.of("response", "Part2", "done", true);
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToFlux(Map.class))
                .thenReturn(Flux.just(response1, response2));

        // Act
        String result = ollamaClient.ask(prompt);

        // Assert
        assertEquals("Part1Part2", result);
        verify(webClient.post().uri(anyString()), times(1)).bodyValue(any());
    }

    @Test
    void testAskError() {
        // Arrange
        String prompt = "Test prompt";
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToFlux(Map.class))
                .thenThrow(new RuntimeException("Ollama error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> ollamaClient.ask(prompt));
    }
}