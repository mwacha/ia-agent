package com.github.mwacha.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class OllamaClient {

    private final WebClient webClient;

    public OllamaClient() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:11434")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Envia um prompt ao Ollama e retorna a resposta gerada.
     *
     * @param prompt O prompt a ser enviado.
     * @return A resposta gerada pelo modelo.
     * @throws RuntimeException se ocorrer um erro na chamada ao Ollama.
     */
    public String ask(String prompt) {
        Map<String, Object> request = Map.of(
                "model", "gemma2",
                "prompt", prompt,
                "max_tokens", 1000,
                "temperature", 0.7,
                "stream", true // Explicitamente habilitar streaming
        );

        try {
            StringBuilder responseBuilder = new StringBuilder();
            AtomicBoolean isDone = new AtomicBoolean(false);

            Flux<Map> responseFlux = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(Map.class);

            responseFlux.subscribe(
                    response -> {
                        String fragment = (String) response.get("response");
                        if (fragment != null) {
                            responseBuilder.append(fragment);
                        }
                        if (Boolean.TRUE.equals(response.get("done"))) {
                            isDone.set(true);
                        }
                    },
                    error -> {
                        log.error("Erro ao processar stream do Ollama: {}", error.getMessage());
                        throw new RuntimeException("Erro ao processar stream do Ollama", error);
                    }
            );

            // Aguardar até que a resposta esteja completa
            while (!isDone.get()) {
                Thread.sleep(10); // Pequena espera para evitar consumo excessivo de CPU
            }

            String result = responseBuilder.toString();
            log.debug("Resposta completa do Ollama: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Erro ao chamar Ollama: {}", e.getMessage());
            throw new RuntimeException("Erro ao chamar Ollama", e);
        }
    }
}