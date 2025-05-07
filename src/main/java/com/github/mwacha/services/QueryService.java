package com.github.mwacha.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço para realizar consultas no ChromaDB com base em perguntas do usuário.
 */
@Service
@Slf4j
public class QueryService {

    private final EmbeddingService embeddingService;
    private final ChromaClient chromaClient;
    private final OllamaClient ollamaClient;

    public QueryService(EmbeddingService embeddingService, ChromaClient chromaClient, OllamaClient ollamaClient) {
        this.embeddingService = embeddingService;
        this.chromaClient = chromaClient;
        this.ollamaClient = ollamaClient;
    }

    /**
     * Realiza uma consulta com base na pergunta do usuário e retorna uma resposta.
     *
     * @param question A pergunta do usuário.
     * @return Resposta baseada nos documentos encontrados ou mensagem informativa se nenhum documento for encontrado.
     */
    public String ask(String question) {
        if (question == null || question.trim().isEmpty()) {
            log.warn("Pergunta vazia ou nula recebida.");
            return "Por favor, forneça uma pergunta válida.";
        }

        try {
            // Gerar embedding para a pergunta
            float[] questionEmbedding = embeddingService.embed(question);
            log.debug("Embedding gerado para a pergunta: tamanho {}", questionEmbedding.length);

            // Consultar documentos relevantes no ChromaDB
            List<String> contextChunks = chromaClient.queryRelevant(question, questionEmbedding);
            log.debug("Documentos relevantes encontrados (tamanho: {}): {}", contextChunks.size(), contextChunks);

            // Construir o prompt com instruções claras
            StringBuilder prompt = new StringBuilder();
            prompt.append("Você é um assistente especializado em responder perguntas com base em documentos técnicos. Sua tarefa é extrair a definição exata do termo perguntado a partir do contexto fornecido. Responda de forma concisa, usando a definição exata do contexto, sem adicionar informações externas. Se o termo não estiver no contexto, responda: 'O termo não foi encontrado no contexto fornecido.'\n\n");
            prompt.append("Contexto:\n");
            if (contextChunks.isEmpty()) {
                prompt.append("Nenhum contexto relevante encontrado.\n");
                log.warn("Nenhum documento relevante retornado para a pergunta: {}", question);
            } else {
                contextChunks.forEach(chunk -> prompt.append(chunk).append("\n\n"));
            }
            prompt.append("Pergunta: ").append(question).append("\n");
            prompt.append("Resposta: ");

            // Logar o prompt para depuração
            log.debug("Prompt enviado ao Ollama: {}", prompt.toString());

            // Enviar o prompt ao Ollama
            String response = ollamaClient.ask(prompt.toString());
            log.debug("Resposta bruta do Ollama: {}", response);

            // Verificar se a resposta é válida
            if (response == null || response.trim().isEmpty()) {
                log.info("Resposta inadequada do Ollama para a pergunta: {}", question);
                return "Nenhuma informação relevante foi encontrada para a pergunta: \"" + question + "\".";
            }

            return response;
        } catch (Exception e) {
            log.error("Erro ao processar a pergunta '{}': {}", question, e.getMessage());
            return "Ocorreu um erro ao processar a pergunta: " + e.getMessage();
        }
    }
}