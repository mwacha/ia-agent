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
class ChromaClientTest {

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    @InjectMocks
    private ChromaClient chromaClient;

    @BeforeEach
    void setUp() throws Exception {
        // Configurar mock para get() (verificar coleção)
        when(webClient.get().uri(anyString()).retrieve().bodyToMono(Map.class))
                .thenReturn(Mono.just(Map.of("id", "test-uuid", "name", "my_collection")));

        // Configurar mock para post() (criar coleção)
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class))
                .thenReturn(Mono.just(Map.of("id", "test-uuid", "name", "my_collection")));

        // Inicializar o ChromaClient
        chromaClient.init();
    }

    @Test
    void testAddDocument() {
        // Arrange
        String text = "Test document";
        float[] embedding = new float[1024];
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Void.class))
                .thenReturn(Mono.empty());

        // Act
        chromaClient.addDocument(text, embedding);

        // Assert
        verify(webClient.post().uri(anyString()), times(1)).bodyValue(any());
    }

    @Test
    void testAddDocumentInvalidEmbedding() {
        // Arrange
        String text = "Test document";
        float[] embedding = new float[512]; // Tamanho errado

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> chromaClient.addDocument(text, embedding));
        verifyNoInteractions(webClient);
    }

    @Test
    void testQueryRelevant() {
        // Arrange
        String query = "Test query";
        float[] embedding = new float[1024];
        Map<String, Object> response = Map.of("documents", List.of(List.of("Relevant document")));
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class))
                .thenReturn(Mono.just(response));

        // Act
        List<String> result = chromaClient.queryRelevant(query, embedding);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Relevant document", result.get(0));
        verify(webClient.post().uri(anyString()), times(1)).bodyValue(any());
    }
}