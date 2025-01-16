package com.github.aws.tutorials.rag.assistant;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.aws.tutorials.rag.assistant.dto.ApiGatewayEvent;
import com.github.aws.tutorials.rag.assistant.dto.ChatRequest;
import com.github.aws.tutorials.rag.assistant.dto.ChatResponse;
import com.github.aws.tutorials.rag.utility.EnvUtils;
import com.github.aws.tutorials.rag.utility.SecretManagerRetriever;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.*;
import java.util.Map;

import static com.github.aws.tutorials.rag.utility.ResponseUtils.createResponse;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

@Slf4j
@RequiredArgsConstructor
public class AssistantHandler implements RequestStreamHandler {
    private static final String OPENAI_API_KEY = "openai-api-key";

    private final Assistant assistant;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssistantHandler() {
        SecretManagerRetriever secretManagerRetriever = new SecretManagerRetriever();
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(secretManagerRetriever.getSecret(OPENAI_API_KEY))
                .modelName(GPT_4_O_MINI)
                .build();

        EmbeddingStore<TextSegment> embeddingStore = EnvUtils.getEmbeddingStore();
        DynamoDbClient dynamoDbClient = EnvUtils.getDynamoDbClient();
        String tableName = EnvUtils.getDynamoDbTableName();

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryStore(new DynamoDBChatMemoryStore(dynamoDbClient, tableName))
                .build();

        assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(chatModel)
                .chatMemory(chatMemory)
                .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
                .build();
    }

    @Override
    public void handleRequest(InputStream inputStream,
                              OutputStream outputStream,
                              Context context) throws IOException {

        Map<String, Object> response;
        Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            ApiGatewayEvent event = objectMapper.readValue(reader, ApiGatewayEvent.class);
            String body = event.getBody();
            if (body == null) {
                response = createResponse(400, "Missing request body", null);
                objectMapper.writeValue(writer, response);
                return;
            }
            log.info("Received API Gateway event: {}", body);
            ChatRequest chatRequest = objectMapper.readValue(body, ChatRequest.class);
            String answer = assistant.chat(chatRequest.getMessage());
            response = createResponse(200, "Message processed successfully", new ChatResponse(answer));

        } catch (Exception e) {
            log.error("Error processing API Gateway event", e);
            response = createResponse(500, e.getMessage(), null);
        }
        objectMapper.writeValue(writer, response);
    }
}
