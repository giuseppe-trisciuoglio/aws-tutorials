package com.github.aws.tutorials.rag.knowledge.repository;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class KnowledgeBaseFileSystemRepositoryImpl implements KnowledgeBaseRepository {

    @Override
    public Optional<Document> getDocument(String directoryPath, String fileName) {
        Objects.requireNonNull(directoryPath, "directoryPath cannot be null");
        Objects.requireNonNull(directoryPath, "fileName cannot be null");
        try {
            DocumentParser parser = fileName.endsWith(".pdf")
                    ? new ApachePdfBoxDocumentParser()
                    : new TextDocumentParser();
            String filePath = directoryPath + "/" + fileName;
            Document document = FileSystemDocumentLoader.loadDocument(filePath, parser);
            return Optional.of(document);
        } catch (Exception e) {
            log.error("Error reading document from file system", e);
        }
        return Optional.empty();
    }
}
