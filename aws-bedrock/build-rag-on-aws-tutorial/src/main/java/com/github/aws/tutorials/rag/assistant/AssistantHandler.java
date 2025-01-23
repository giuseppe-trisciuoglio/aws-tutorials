package com.github.aws.tutorials.rag.assistant;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.aws.tutorials.rag.assistant.dto.ApiGatewayEvent;
import com.github.aws.tutorials.rag.assistant.dto.ChatRequest;
import com.github.aws.tutorials.rag.assistant.dto.ChatResponse;
import com.github.aws.tutorials.rag.utility.EnvUtils;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.*;
import java.util.Map;

import static com.github.aws.tutorials.rag.utility.ResponseUtils.createResponse;

@Slf4j
@RequiredArgsConstructor
public class AssistantHandler implements RequestStreamHandler {

    private final Assistant assistant;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssistantHandler() {
        EmbeddingStore<TextSegment> embeddingStore = EnvUtils.getEmbeddingStore();
        DynamoDbClient dynamoDbClient = EnvUtils.getDynamoDbClient();
        String tableName = EnvUtils.getDynamoDbTableName();

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryStore(new DynamoDBChatMemoryStore(dynamoDbClient, tableName))
                .build();
        
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(EnvUtils.getEmbeddingModel())
                .maxResults(2) 
                .minScore(0.5)
                .build();

        assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(EnvUtils.getChatLanguageModel())
                .chatMemory(chatMemory)
                .contentRetriever(contentRetriever)
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
