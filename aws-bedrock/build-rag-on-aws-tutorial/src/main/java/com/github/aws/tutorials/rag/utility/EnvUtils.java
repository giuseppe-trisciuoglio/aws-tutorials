package com.github.aws.tutorials.rag.utility;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class EnvUtils {
    private static final String AOSS = "aoss";
    private EnvUtils() {
        throw new IllegalStateException("Utility class");
    }
    
    public static String getDefaultAwsRegionToString() {
        String awsRegion = System.getenv("AWS_REGION");
        return awsRegion != null ? awsRegion : "eu-west-1";
    }

    public static Region getDefaultAwsRegion() {
        String awsRegion = System.getenv("AWS_REGION");
        return Region.of(awsRegion != null ? awsRegion : "eu-west-1");
    }

    public static String getOpenSearchUrl() {
        String openSearchUrl = System.getenv("OPENSEARCH_URL");
        if (openSearchUrl == null || openSearchUrl.isEmpty()) {
            throw new IllegalArgumentException("OPENSEARCH_URL environment variable is not set");
        }
        return openSearchUrl;
    }
    
    public static boolean isOpenAIEnabled() {
        String openaiEnabled = System.getenv("OPENAI_ENABLED");
        if (openaiEnabled == null || openaiEnabled.isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(openaiEnabled);
    }

    public static DynamoDbClient getDynamoDbClient() {
        return DynamoDbClient.builder()
                .region(getDefaultAwsRegion())
                .build();
    }
    
    public static String getDynamoDbTableName() {
        String tableName = System.getenv("DYNAMODB_TABLE");
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("DYNAMODB_TABLE environment variable is not set");
        }
        return tableName;
    }
    
    public static EmbeddingStore<TextSegment> getEmbeddingStore() {
        return OpenSearchEmbeddingStore.builder()
                .serverUrl(EnvUtils.getOpenSearchUrl())
                .region(EnvUtils.getDefaultAwsRegionToString())
                .serviceName(AOSS)
                .options(AwsSdk2TransportOptions.builder().build())
                .build();
    }
}
