package com.github.aws.tutorials.rag.utility;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.bedrock.BedrockAnthropicMessageChatModel;
import dev.langchain4j.model.bedrock.BedrockTitanEmbeddingModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;

public final class EnvUtils {
    private static final String AOSS = "aoss";
    private static final String GPT_4_O_MINI = "gpt-4-o-mini";
    private static final String AWS_REGION = "AWS_REGION";
    private static final String OPENSEARCH_URL = "OPENSEARCH_URL";
    private static final String OPENAI_ENABLED = "OPENAI_ENABLED";
    private static final String AWS_REGION_DEFAULT = "eu-west-1";
    private static final String ANTHROPIC_CLAUDE_3_5_SONNET = "anthropic.claude-3-sonnet-20240229-v1:0";
    private static final String DYNAMODB_TABLE = "DYNAMODB_TABLE";
    private static final String OPENAI_API_KEY = "openai-api-key";
    public static final String AMAZON_TITAN_EMBED_TEXT_V_2_0 = "amazon.titan-embed-text-v2:0";

    private EnvUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String getDefaultAwsRegionToString() {
        String awsRegion = System.getenv(AWS_REGION);
        return awsRegion != null ? awsRegion : "eu-west-1";
    }

    public static Region getDefaultAwsRegion() {
        String awsRegion = System.getenv(AWS_REGION);
        return Region.of(awsRegion != null ? awsRegion : AWS_REGION_DEFAULT);
    }

    public static String getOpenSearchUrl() {
        String openSearchUrl = System.getenv(OPENSEARCH_URL);
        if (openSearchUrl == null || openSearchUrl.isEmpty()) {
            throw new IllegalArgumentException("OPENSEARCH_URL environment variable is not set");
        }
        return openSearchUrl;
    }

    public static ChatLanguageModel getChatLanguageModel() {
        ChatLanguageModel chatModel;
        if (isOpenAIEnabled()) {
            chatModel = OpenAiChatModel.builder()
                    .apiKey(getOpenAiApiKey())
                    .modelName(GPT_4_O_MINI)
                    .build();
        } else {
            chatModel = BedrockAnthropicMessageChatModel.builder()
                    .model(ANTHROPIC_CLAUDE_3_5_SONNET)
                    .build();
        }
        return chatModel;
    }

    public static String getOpenAiApiKey() {
        SecretManagerRetriever secretManagerRetriever = new SecretManagerRetriever();
        return secretManagerRetriever.getSecret(OPENAI_API_KEY);
    }

    public static boolean isOpenAIEnabled() {
        String openaiEnabled = System.getenv(OPENAI_ENABLED);
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
        String tableName = System.getenv(DYNAMODB_TABLE);
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

    public static EmbeddingModel getEmbeddingModel() {
        EmbeddingModel embeddingModel;
        if (isOpenAIEnabled()){
            embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(EnvUtils.getOpenAiApiKey())
                    .modelName(TEXT_EMBEDDING_3_SMALL)
                    .build();
        }else{
            embeddingModel = BedrockTitanEmbeddingModel.builder()
                    .region(EnvUtils.getDefaultAwsRegion())
                    .model(AMAZON_TITAN_EMBED_TEXT_V_2_0)
                    .build();

        }
        return embeddingModel;
    }
}
