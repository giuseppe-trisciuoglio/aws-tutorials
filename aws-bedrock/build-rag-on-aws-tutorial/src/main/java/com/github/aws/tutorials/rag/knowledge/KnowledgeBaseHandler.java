package com.github.aws.tutorials.rag.knowledge;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.aws.tutorials.rag.knowledge.dto.S3Detail;
import com.github.aws.tutorials.rag.knowledge.dto.S3Event;
import com.github.aws.tutorials.rag.knowledge.embedding.OpenAIKnowledgeBaseIngestor;
import com.github.aws.tutorials.rag.knowledge.embedding.TitanV2KnowledgeBaseIngestor;
import com.github.aws.tutorials.rag.knowledge.repository.KnowledgeBaseS3RepositoryImpl;
import com.github.aws.tutorials.rag.utility.EnvUtils;
import dev.langchain4j.data.document.loader.amazon.s3.AmazonS3DocumentLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static com.github.aws.tutorials.rag.utility.ResponseUtils.createResponse;

@Slf4j
public class KnowledgeBaseHandler implements RequestStreamHandler {
    private final KnowledgeDocumentProcessor knowledgeDocumentProcessor;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeBaseHandler() {
        knowledgeDocumentProcessor = new KnowledgeDocumentProcessor(
                new KnowledgeBaseS3RepositoryImpl(AmazonS3DocumentLoader.builder()
                        .region(EnvUtils.getDefaultAwsRegionToString())
                        .build()),
                EnvUtils.isOpenAIEnabled() 
                        ? new OpenAIKnowledgeBaseIngestor() 
                        : new TitanV2KnowledgeBaseIngestor()
        );
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        try {
            S3Event event = objectMapper.readValue(inputStream, S3Event.class);
            log.info("Received S3 event: {}", event);
            S3Detail detail = event.getDetail();
            if (detail == null) {
                log.warn("S3 event detail is null");
                return;
            }
            log.info("Received S3 detail: {}", detail);
            if (null == detail.getBucket()) {
                log.warn("S3 event bucket is null");
                return;
            }
            String bucketName = detail.getBucket().getName();
            if (null == detail.getObject()) {
                log.warn("S3 event object is null");
                return;
            }
            String fileName = detail.getObject().getKey();
            log.info("New file uploaded: {} in bucket: {}", fileName, bucketName);
            knowledgeDocumentProcessor.processDocument(bucketName, fileName);

            Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            objectMapper.writeValue(writer, createResponse(200, "Document processed successfully", null));
        } catch (Exception e) {
            log.error("Error processing S3 event", e);
            throw e;
        } finally {
            outputStream.close();
        }
    }

}
