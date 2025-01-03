package com.github.aws.tutorials.rag.knowledge;

import com.github.aws.tutorials.rag.knowledge.embedding.KnowledgeBaseIngestor;
import com.github.aws.tutorials.rag.knowledge.repository.KnowledgeBaseRepository;
import dev.langchain4j.data.document.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class KnowledgeDocumentProcessor {
    
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseIngestor knowledgeBaseIngestor;

    public void processDocument(String bucketName, String objectKey) {
        try {
            Document document = knowledgeBaseRepository.getDocument(bucketName, objectKey)
                    .orElseThrow(() -> new IOException("Document not found"));
            knowledgeBaseIngestor.ingest(document);
        } catch (Exception e) {
            log.error("Error processing document", e);
        }
    }
}
