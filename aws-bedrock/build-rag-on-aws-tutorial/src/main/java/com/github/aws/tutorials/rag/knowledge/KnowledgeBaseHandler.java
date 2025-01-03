package com.github.aws.tutorials.rag.knowledge;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.aws.tutorials.rag.knowledge.dto.S3Detail;
import com.github.aws.tutorials.rag.knowledge.dto.S3Event;
import com.github.aws.tutorials.rag.knowledge.embedding.TitanV2KnowledgeBaseIngestor;
import com.github.aws.tutorials.rag.knowledge.repository.KnowledgeBaseS3RepositoryImpl;
import com.github.aws.tutorials.rag.utility.EnvUtils;
import dev.langchain4j.data.document.loader.amazon.s3.AmazonS3DocumentLoader;
import dev.langchain4j.data.document.loader.amazon.s3.AwsCredentials;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.bedrock.BedrockTitanEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Slf4j
public class KnowledgeBaseHandler implements RequestStreamHandler {
    private final KnowledgeDocumentProcessor knowledgeDocumentProcessor;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeBaseHandler() {
        Region region = Region.of(EnvUtils.getDefaultAwsRegion());
        BedrockTitanEmbeddingModel titanEmbeddingModel = BedrockTitanEmbeddingModel.builder()
                .region(region)
                .model("amazon.titan-embed-text-v2:0")
                .build();

        EmbeddingStore<TextSegment> embeddingStore = OpenSearchEmbeddingStore.builder()
                .serverUrl(EnvUtils.getOpenSearchUrl())
                .region(EnvUtils.getDefaultAwsRegion())
                .serviceName("aoss")
                .options(AwsSdk2TransportOptions.builder().build())
                .build();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(450, 0))
                .embeddingModel(titanEmbeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        knowledgeDocumentProcessor = new KnowledgeDocumentProcessor(
                new KnowledgeBaseS3RepositoryImpl(AmazonS3DocumentLoader.builder()
                        .region(region)
                        .build()),
                new TitanV2KnowledgeBaseIngestor(ingestor)
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
            if (null ==  detail.getBucket()) {
                log.warn("S3 event bucket is null");
                return;
            }
            String bucketName = detail.getBucket().getName();
            if (null ==  detail.getObject()) {
                log.warn("S3 event object is null");
                return;
            }
            String fileName = detail.getObject().getKey();
            log.info("New file uploaded: {} in bucket: {}", fileName, bucketName);
            knowledgeDocumentProcessor.processDocument(bucketName, fileName);
        } catch (Exception e) {
            log.error("Error processing S3 event", e);
            throw e;
        } finally {
            outputStream.close();
        }
    }

}
