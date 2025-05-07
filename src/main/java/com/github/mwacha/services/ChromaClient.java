package com.github.mwacha.services;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Serviço para interação com a API REST do ChromaDB, permitindo criar coleções,
 * adicionar documentos com embeddings e consultar documentos relevantes.
 */
@Service
@Slf4j
public class ChromaClient {

    private static final String BASE_URL = "http://localhost:8001";
    private static final String COLLECTION_NAME = "my_collection";
    private static final String API_PATH = "/api/v1";
    private static final int EXPECTED_EMBEDDING_SIZE = 1024;

    private final WebClient webClient;
    private String collectionUuid; // Armazena o UUID da coleção

    public ChromaClient() {
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    @PostConstruct
    public void init() {
        try {
            criarOuObterColecao();
        } catch (Exception e) {
            log.error("Falha ao inicializar a coleção {}: {}", COLLECTION_NAME, e.getMessage());
            throw new IllegalStateException("Não foi possível inicializar o ChromaClient", e);
        }
    }

    /**
     * Adiciona um documento com seu embedding à coleção no ChromaDB.
     *
     * @param text     O texto do documento.
     * @param embedding O vetor de embedding do documento.
     * @throws IllegalArgumentException se o texto ou embedding for inválido.
     */
    public void addDocument(String text, float[] embedding) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("O texto do documento não pode ser nulo ou vazio.");
        }
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("O embedding não pode ser nulo ou vazio.");
        }
        if (embedding.length != EXPECTED_EMBEDDING_SIZE) {
            throw new IllegalArgumentException(
                    "O embedding deve ter " + EXPECTED_EMBEDDING_SIZE + " dimensões, mas tem " + embedding.length);
        }
        if (collectionUuid == null) {
            throw new IllegalStateException("UUID da coleção não foi inicializado.");
        }

        List<Float> embeddingList = new ArrayList<>();
        for (float value : embedding) {
            embeddingList.add(value);
        }

        Map<String, Object> doc = Map.of(
                "documents", List.of(text),
                "embeddings", List.of(embeddingList),
                "metadatas", List.of(Map.of("source", "upload")),
                "ids", List.of("doc_" + System.currentTimeMillis())
        );

        // Logar o corpo da requisição para depuração
        log.debug("Enviando requisição para upsert: {}", doc);
        log.debug("Tamanho do embedding enviado: {}", embeddingList.size());

        try {
            webClient.post()
                    .uri(API_PATH + "/collections/" + collectionUuid + "/upsert")
                    .bodyValue(doc)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Documento adicionado com sucesso à coleção {}", COLLECTION_NAME);
        } catch (WebClientResponseException e) {
            log.error("Erro ao adicionar documento: Status {}, Resposta: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * Consulta documentos relevantes com base em um embedding de consulta.
     *
     * @param query     O texto da consulta (para logging, não usado diretamente).
     * @param embedding O vetor de embedding da consulta.
     * @return Lista de textos dos documentos relevantes ou lista vazia se nenhum for encontrado.
     * @throws IllegalArgumentException se o embedding for inválido.
     */
    public List<String> queryRelevant(String query, float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("O embedding da consulta não pode ser nulo ou vazio.");
        }
        if (embedding.length != EXPECTED_EMBEDDING_SIZE) {
            throw new IllegalArgumentException(
                    "O embedding deve ter " + EXPECTED_EMBEDDING_SIZE + " dimensões, mas tem " + embedding.length);
        }
        if (collectionUuid == null) {
            throw new IllegalStateException("UUID da coleção não foi inicializado.");
        }

        List<Float> embeddingList = new ArrayList<>();
        for (float value : embedding) {
            embeddingList.add(value);
        }

        Map<String, Object> body = Map.of(
                "query_embeddings", List.of(embeddingList),
                "n_results", 20, // Aumentado para 20
                "include", List.of("documents", "metadatas", "distances")
        );

        log.debug("Enviando requisição para query: {}", body);

        try {
            Map<String, Object> response = webClient.post()
                    .uri(API_PATH + "/collections/" + collectionUuid + "/query")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("documents")) {
                log.warn("Nenhum documento encontrado para a consulta: {}", query);
                return Collections.emptyList();
            }

            List<List<String>> documents = (List<List<String>>) response.get("documents");
            log.debug("Documentos retornados: {}", documents);
            return documents.isEmpty() ? Collections.emptyList() : documents.get(0);
        } catch (WebClientResponseException e) {
            log.error("Erro ao consultar documentos: Status {}, Resposta: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * Cria a coleção se ela não existir, ou verifica sua existência.
     * Armazena o UUID da coleção para uso nos endpoints.
     * Trata erro 500 com mensagem de coleção inexistente como 404.
     */
    private void criarOuObterColecao() {
        try {
            Map<String, Object> existingCollection = webClient.get()
                    .uri(API_PATH + "/collections/" + COLLECTION_NAME)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (existingCollection != null && existingCollection.containsKey("name")) {
                collectionUuid = (String) existingCollection.get("id");
                log.info("Coleção {} já existe com UUID {}.", COLLECTION_NAME, collectionUuid);
                return;
            }
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND ||
                    (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR &&
                            e.getResponseBodyAsString().contains("Collection " + COLLECTION_NAME + " does not exist"))) {
                log.info("Coleção {} não existe, será criada.", COLLECTION_NAME);
            } else {
                log.error("Erro ao verificar coleção: Status {}, Resposta: {}",
                        e.getStatusCode(), e.getResponseBodyAsString());
                throw e;
            }
        }

        Map<String, Object> request = Map.of("name", COLLECTION_NAME);

        try {
            Map<String, Object> response = webClient.post()
                    .uri(API_PATH + "/collections")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response != null && response.containsKey("id")) {
                collectionUuid = (String) response.get("id");
                log.info("Coleção {} criada com sucesso com UUID {}.", COLLECTION_NAME, collectionUuid);
            } else {
                throw new IllegalStateException("Falha ao obter UUID da coleção criada.");
            }
        } catch (WebClientResponseException e) {
            log.error("Erro ao criar coleção: Status {}, Resposta: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
}