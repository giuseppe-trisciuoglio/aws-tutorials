package com.github.aws.tutorials.rag.assistant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamoDBChatMemoryStore implements ChatMemoryStore {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<List<ChatMessage>> MESSAGE_LIST_TYPE = new TypeReference<List<ChatMessage>>() {};
    
    public DynamoDBChatMemoryStore(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public List<ChatMessage> getMessages(Object sessionId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(sessionId.toString()).build());
        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();
        GetItemResponse getItemResponse = dynamoDbClient.getItem(getItemRequest);
        if (getItemResponse.hasItem()) {
            return fromItem(getItemResponse.item());
        }
        return Collections.emptyList();
    }

    private List<ChatMessage> fromItem(Map<String, AttributeValue> item) {
        try {
            if (item == null || !item.containsKey("messages")) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(item.get("messages").s(), MESSAGE_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateMessages(Object sessionId, List<ChatMessage> messages) {
        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(tableName)
                .item(toItem(sessionId, messagesToJson(messages)))
                .build();
        dynamoDbClient.putItem(putItemRequest);
    }

    @Override
    public void deleteMessages(Object sessionId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(sessionId.toString()).build());
        DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();
        dynamoDbClient.deleteItem(deleteItemRequest);
    }
    

    private Map<String, AttributeValue> toItem(Object sessionId, String jsonMessages) {
        return Map.of(
                "id", AttributeValue.builder().s(sessionId.toString()).build(),
                "messages", AttributeValue.builder().s(jsonMessages).build(),
                "timestamp", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build()
        );
    }

    public String messagesToJson(List<ChatMessage> messages) {
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
