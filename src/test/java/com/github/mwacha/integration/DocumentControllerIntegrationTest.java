package com.github.mwacha.integration;

import com.github.mwacha.services.DocumentStoreService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureWebTestClient
@Testcontainers
class DocumentControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DocumentStoreService documentStoreService;

    @Container
    private static final GenericContainer<?> chromaContainer = new GenericContainer<>("chromadb/chroma:latest")
            .withExposedPorts(8000);

    @BeforeAll
    static void beforeAll() {
        // Configurar ChromaDB
        System.setProperty("chroma.base-url", "http://" + chromaContainer.getHost() + ":" + chromaContainer.getMappedPort(8000));
    }

    @BeforeEach
    void setUp() {
        // Limpar coleção no ChromaDB antes de cada teste
        webTestClient.delete().uri("/api/reset-collection").exchange().expectStatus().isOk();
    }

    @Test
    void testUploadAndAsk() throws Exception {
        // Arrange
        String documentContent = "Domain\nModels: Estruturas de dados que representam objetos de negócio. São utilizados para transferência de dados dentro do sistema.";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", documentContent.getBytes());
        String question = "No padrão arquitetural da conta azul, o que significa models?";

        // Act: Upload
        webTestClient.post()
                .uri("/api/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(file)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Documento armazenado com sucesso.");

        // Act: Ask
        webTestClient.get()
                .uri("/api/ask?q=" + question)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Models: Estruturas de dados que representam objetos de negócio. São utilizados para transferência de dados dentro do sistema.");
    }

    @Test
    void testAskWithoutDocuments() {
        // Arrange
        String question = "What is models?";

        // Act
        webTestClient.get()
                .uri("/api/ask?q=" + question)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Nenhuma informação relevante foi encontrada para a pergunta: \"" + question + "\".");
    }
}