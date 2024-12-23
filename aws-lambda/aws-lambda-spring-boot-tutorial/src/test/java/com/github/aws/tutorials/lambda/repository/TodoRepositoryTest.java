package com.github.aws.tutorials.lambda.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.aws.tutorials.lambda.model.Todo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.BeanUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TodoRepositoryTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    @InjectMocks
    private TodoRepository todoRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createTodoSuccessfully() {
        Todo todo = new Todo();
        todo.setId(UUID.randomUUID());
        todo.setCreatedAt(LocalDateTime.now());
        PutItemRequest request = PutItemRequest.builder().tableName("todos").item(new HashMap<>()).build();
        when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());

        Todo result = todoRepository.createTodo(todo);

        assertNotNull(result);
        assertEquals(todo.getId(), result.getId());
        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    @Test
    void createTodoWithExistingId() {
        Todo todo = new Todo();
        todo.setId(UUID.randomUUID());
        todo.setCreatedAt(LocalDateTime.now());
        PutItemRequest request = PutItemRequest.builder().tableName("todos").item(new HashMap<>()).build();
        when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenThrow(ConditionalCheckFailedException.builder().build());

        assertThrows(ConditionalCheckFailedException.class, () -> todoRepository.createTodo(todo));
        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    @Test
    void getTodoByIdSuccessfully() {
        UUID id = UUID.randomUUID();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(id.toString()).build());
        item.put("title", AttributeValue.builder().s("title").build());
        item.put("completed", AttributeValue.builder().bool(true).build());
        item.put("created_at", AttributeValue.builder().s(LocalDateTime.now().toString()).build());
        GetItemRequest request = GetItemRequest.builder().tableName("todos").key(item).build();
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().item(item).build());

        Optional<Todo> result = todoRepository.getTodoById(id);

        assertTrue(result.isPresent());
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
    }

    @Test
    void getTodoByIdNotFound() {
        UUID id = UUID.randomUUID();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(id.toString()).build());
        GetItemRequest request = GetItemRequest.builder().tableName("todos").key(item).build();
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());

        Optional<Todo> result = todoRepository.getTodoById(id);

        assertFalse(result.isPresent());
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
    }

    @Test
    void getAllTodosSuccessfully() {
        ScanRequest request = ScanRequest.builder().tableName("todos").build();
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(ScanResponse.builder().items(new ArrayList<>()).build());

        List<Todo> result = todoRepository.getAllTodos();

        assertNotNull(result);
        verify(dynamoDbClient, times(1)).scan(any(ScanRequest.class));
    }

    @Test
    void updateTodoSuccessfully() {
        Todo todo = new Todo();
        todo.setId(UUID.randomUUID());
        todo.setTitle("title");
        todo.setCompleted(true);
        todo.setCreatedAt(LocalDateTime.now());
        
        HashMap<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(todo.getId().toString()).build());
        item.put("title", AttributeValue.builder().s(todo.getTitle()).build());
        item.put("completed", AttributeValue.builder().bool(todo.isCompleted()).build());
        item.put("created_at", AttributeValue.builder().s(todo.getCreatedAt().toString()).build());

        UpdateItemRequest request = UpdateItemRequest.builder().tableName("todos").key(new HashMap<>()).build();
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(UpdateItemResponse.builder().attributes(item)
                        .build());

        Todo result = todoRepository.updateTodo(todo);

        assertNotNull(result);
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void updateTodoNotFound() {
        Todo todo = new Todo();
        todo.setId(UUID.randomUUID());
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(ResourceNotFoundException.class, () -> todoRepository.updateTodo(todo));
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void deleteTodoSuccessfully() {
        UUID id = UUID.randomUUID();
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id.toString()).build());
        
        when(dynamoDbClient.deleteItem(any(DeleteItemRequest.class))).thenReturn(DeleteItemResponse.builder().build());
        
        todoRepository.deleteTodo(id);

        verify(dynamoDbClient, times(1)).deleteItem(any(DeleteItemRequest.class));
    }
}