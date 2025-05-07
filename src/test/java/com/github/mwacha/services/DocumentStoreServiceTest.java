package com.github.mwacha.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentStoreServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ChromaClient chromaClient;

    @InjectMocks
    private DocumentStoreService documentStoreService;

    @BeforeEach
    void setUp() {
        // Configuração inicial, se necessário
    }

    @Test
    void testStoreDocumentTxt() throws IOException {
        // Arrange
        String text = "Sample document content.";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", text.getBytes());
        float[] embedding = new float[1024];
        when(embeddingService.embed(anyString())).thenReturn(embedding);

        // Act
        documentStoreService.storeDocument(file);

        // Assert
        verify(embeddingService, times(1)).embed(text);
        verify(chromaClient, times(1)).addDocument(text, embedding);
    }

    @Test
    void testStoreDocumentUnsupportedFormat() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.xyz", "application/octet-stream", new byte[0]);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> documentStoreService.storeDocument(file));
        verifyNoInteractions(embeddingService, chromaClient);
    }

    @Test
    void testChunkText() {
        // Arrange
        String text = "Sentence one. Sentence two. Sentence three.";
        int maxTokens = 2;

        // Act
        List<String> chunks = documentStoreService.chunkText(text, maxTokens);

        // Assert
        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0).contains("Sentence one."));
        assertTrue(chunks.get(1).contains("Sentence two."));
        assertTrue(chunks.get(2).contains("Sentence three."));
    }
}