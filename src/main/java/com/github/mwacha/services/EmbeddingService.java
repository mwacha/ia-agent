package com.github.mwacha.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Serviço para gerar embeddings usando o Ollama.
 */
@Service
@Slf4j
public class EmbeddingService {

    private static final int EXPECTED_EMBEDDING_SIZE = 1024;

    private final WebClient webClient;

    public EmbeddingService() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:11434")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Gera um embedding para o texto fornecido.
     *
     * @param text Texto para gerar o embedding.
     * @return Vetor de embedding como float[].
     * @throws IllegalStateException se o embedding gerado for inválido.
     */
    public float[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("O texto para embedding não pode ser nulo ou vazio.");
        }

        Map<String, Object> body = Map.of(
                "model", "snowflake-arctic-embed2",
                "prompt", text
        );
        try {
            Map response = webClient.post()
                    .uri("/api/embeddings")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Double> embedding = (List<Double>) response.get("embedding");
            if (embedding == null || embedding.size() != EXPECTED_EMBEDDING_SIZE) {
                throw new IllegalStateException(
                        "Embedding gerado tem " + (embedding == null ? 0 : embedding.size()) +
                                " dimensões, esperado " + EXPECTED_EMBEDDING_SIZE);
            }

            float[] floats = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                floats[i] = embedding.get(i).floatValue();
            }
            log.debug("Embedding gerado para texto '{}': tamanho {}", text.substring(0, Math.min(text.length(), 50)), floats.length);
            return floats;
        } catch (Exception e) {
            log.error("Erro ao gerar embedding para texto '{}': {}", text, e.getMessage());
            throw new RuntimeException("Erro ao gerar embedding", e);
        }
    }
}