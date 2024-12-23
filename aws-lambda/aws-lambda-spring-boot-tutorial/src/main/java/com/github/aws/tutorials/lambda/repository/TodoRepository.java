package com.github.aws.tutorials.lambda.repository;

import com.github.aws.tutorials.lambda.model.Todo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class TodoRepository {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public TodoRepository(DynamoDbClient dynamoDbClient, @Value("${dynamodb.table}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public Todo createTodo(Todo todo) {
        if (todo.getId() == null) {
            todo.setId(UUID.randomUUID());
        }
        if (todo.getCreatedAt() == null) {
            todo.setCreatedAt(LocalDateTime.now());
        }

        Map<String, AttributeValue> item = todoToAttributeMap(todo);

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .conditionExpression("attribute_not_exists(id)")
                .build();

        try {
            dynamoDbClient.putItem(request);
            return todo;
        } catch (DynamoDbException e) {
            log.error("Error creating todo: " + e.getMessage());
            throw e;
        }
    }

    public Optional<Todo> getTodoById(UUID id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id.toString()).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        try {
            GetItemResponse response = dynamoDbClient.getItem(request);
            if (response.hasItem()) {
                return Optional.of(mapToTodo(response.item()));
            }
            return Optional.empty();
        } catch (DynamoDbException e) {
            log.error("Error getting todo by id: " + e.getMessage());
            throw e;
        }
    }

    public List<Todo> getAllTodos() {
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .build();

        try {
            ScanResponse response = dynamoDbClient.scan(scanRequest);
            return response.items().stream()
                    .map(this::mapToTodo)
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            log.error("Error scanning todos: " + e.getMessage());
            throw e;
        }
    }

    public Todo updateTodo(Todo todo) {
        if (todo.getId() == null) {
            throw new IllegalArgumentException("Todo ID cannot be null for update operation");
        }

        Map<String, String> attrNames = new HashMap<>();
        Map<String, AttributeValue> attrValues = new HashMap<>();
        StringBuilder updateExpression = new StringBuilder("SET");

        if (todo.getTitle() != null) {
            updateExpression.append(" #title = :title,");
            attrNames.put("#title", "title");
            attrValues.put(":title", AttributeValue.builder().s(todo.getTitle()).build());
        }

        if (todo.getDescription() != null) {
            updateExpression.append(" #description = :description,");
            attrNames.put("#description", "description");
            attrValues.put(":description", AttributeValue.builder().s(todo.getDescription()).build());
        }

        updateExpression.append(" #completed = :completed,");
        attrNames.put("#completed", "completed");
        attrValues.put(":completed", AttributeValue.builder().bool(todo.isCompleted()).build());

        // Rimuove l'ultima virgola
        updateExpression.setLength(updateExpression.length() - 1);

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(todo.getId().toString()).build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .updateExpression(updateExpression.toString())
                .expressionAttributeNames(attrNames)
                .expressionAttributeValues(attrValues)
                .returnValues(ReturnValue.ALL_NEW)
                .build();

        try {
            UpdateItemResponse response = dynamoDbClient.updateItem(request);
            return mapToTodo(response.attributes());
        } catch (DynamoDbException e) {
            log.error("Error updating todo: " + e.getMessage());
            throw e;
        }
    }

    public void deleteTodo(UUID id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id.toString()).build());

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        try {
            dynamoDbClient.deleteItem(request);
        } catch (DynamoDbException e) {
            log.error("Error deleting todo: " + e.getMessage());
            throw e;
        }
    }

    private Map<String, AttributeValue> todoToAttributeMap(Todo todo) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(todo.getId().toString()).build());
        item.put("title", AttributeValue.builder().s(todo.getTitle()).build());

        if (todo.getDescription() != null) {
            item.put("description", AttributeValue.builder().s(todo.getDescription()).build());
        }

        item.put("completed", AttributeValue.builder().bool(todo.isCompleted()).build());
        item.put("created_at", AttributeValue.builder()
                .s(todo.getCreatedAt().format(DATE_FORMATTER))
                .build());

        return item;
    }

    private Todo mapToTodo(Map<String, AttributeValue> item) {
        Todo todo = new Todo();
        todo.setId(UUID.fromString(item.get("id").s()));
        todo.setTitle(item.get("title").s());

        if (item.containsKey("description")) {
            todo.setDescription(item.get("description").s());
        }

        todo.setCompleted(item.get("completed").bool());
        todo.setCreatedAt(LocalDateTime.parse(item.get("created_at").s(), DATE_FORMATTER));

        return todo;
    }
}