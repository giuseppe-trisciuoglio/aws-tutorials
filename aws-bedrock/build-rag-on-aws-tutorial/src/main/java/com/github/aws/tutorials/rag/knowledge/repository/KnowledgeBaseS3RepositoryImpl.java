package com.github.aws.tutorials.rag.knowledge.repository;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.amazon.s3.AmazonS3DocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class KnowledgeBaseS3RepositoryImpl implements KnowledgeBaseRepository {
    
    private final AmazonS3DocumentLoader loader;

    /*public KnowledgeBaseS3RepositoryImpl() {
        this.loader = AmazonS3DocumentLoader.builder()
                .region(EnvUtils.getDefaultAwsRegion())
                .build();
    }*/

    public Optional<Document> getDocument(String bucketName, String objectKey) {
        Objects.requireNonNull(bucketName, "Bucket name cannot be null");
        Objects.requireNonNull(objectKey, "Object key cannot be null");
        try {
            Document document = loader.loadDocument(bucketName, objectKey, new TextDocumentParser());
            return Optional.of(document);
        } catch (Exception e) {
            log.error("Error reading document from S3", e);
        }
        return Optional.empty();
    }
}
