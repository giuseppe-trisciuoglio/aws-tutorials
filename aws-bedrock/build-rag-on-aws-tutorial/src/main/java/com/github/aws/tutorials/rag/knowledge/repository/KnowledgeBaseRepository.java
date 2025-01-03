package com.github.aws.tutorials.rag.knowledge.repository;

import dev.langchain4j.data.document.Document;

import java.util.Optional;

public interface KnowledgeBaseRepository {
    Optional<Document> getDocument(String bucketName, String objectKey);
}
