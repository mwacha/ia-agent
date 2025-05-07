package com.github.mwacha.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    @InjectMocks
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        // Configuração inicial não necessária com RETURNS_DEEP_STUBS
    }

    @Test
    void testEmbedSuccess() {
        // Arrange
        String text = "Test text";
        List<Double> embedding = List.of(0.1, 0.2, 0.3); // Exemplo simplificado
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class))
                .thenReturn(Mono.just(Map.of("embedding", embedding)));

        // Act
        float[] result = embeddingService.embed(text);

        // Assert
        assertEquals(3, result.length);
        assertEquals(0.1f, result[0]);
        verify(webClient.post().uri(anyString()), times(1)).bodyValue(any());
    }

    @Test
    void testEmbedEmptyText() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> embeddingService.embed(""));
        verifyNoInteractions(webClient);
    }
}