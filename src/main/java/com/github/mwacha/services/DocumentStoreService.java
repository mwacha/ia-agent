package com.github.mwacha.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentStoreService {

    private final EmbeddingService embeddingService;
    private final ChromaClient chromaClient;

    public DocumentStoreService(EmbeddingService embeddingService, ChromaClient chromaClient) {
        this.embeddingService = embeddingService;
        this.chromaClient = chromaClient;
    }

    public void storeDocument(MultipartFile file) throws IOException {
        String text = extractText(file);
        List<String> chunks = chunkText(text, 1000);

        for (String chunk : chunks) {
            float[] embedding = embeddingService.embed(chunk);
            chromaClient.addDocument(chunk, embedding);
        }
    }

    private String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename().toLowerCase();

        if (filename.endsWith(".pdf")) {
            try (PDDocument pdf = PDDocument.load(file.getInputStream())) {
                return new PDFTextStripper().getText(pdf);
            }
        } else if (filename.endsWith(".txt")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } else if (filename.endsWith(".doc")) {
            try (HWPFDocument doc = new HWPFDocument(file.getInputStream())) {
                return new WordExtractor(doc).getText();
            }
        } else if (filename.endsWith(".docx")) {
            try (XWPFDocument docx = new XWPFDocument(file.getInputStream())) {
                return new XWPFWordExtractor(docx).getText();
            }
        }
        throw new IllegalArgumentException("Tipo de arquivo não suportado: " + filename);
    }

    List<String> chunkText(String text, int maxTokens) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?])\s*");

        StringBuilder chunk = new StringBuilder();
        int tokenCount = 0;
        for (String sentence : sentences) {
            tokenCount += sentence.split("\\s+").length;
            if (tokenCount > maxTokens) {
                chunks.add(chunk.toString());
                chunk.setLength(0);
                tokenCount = sentence.split("\\s+").length;
            }
            chunk.append(sentence).append(" ");
        }
        if (!chunk.isEmpty()) chunks.add(chunk.toString());

        return chunks;
    }
}