package com.github.aws.tutorials.rag.knowledge.embedding;

import dev.langchain4j.data.document.Document;

public interface KnowledgeBaseIngestor {
    void ingest(Document document);
}
