package com.github.aws.tutorials.lambda.controller;

import com.github.aws.tutorials.lambda.model.Todo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled
class TodoControllerHttpTest {

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    private RestTemplate restTemplate;
    private final static String BASE_URL = "https://xxxxxxxxx.execute-api.eu-west-1.amazonaws.com/dev";

    @BeforeEach
    void setUp() {
        restTemplate = restTemplateBuilder.build();
    }

    @Test
    void createTodoSuccessfully() {
        String url = BASE_URL  + "/api/todos";
        Todo todo = new Todo();
        todo.setTitle("Test Todo");

        ResponseEntity<Todo> response = restTemplate.postForEntity(url, todo, Todo.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(todo.getTitle(), response.getBody().getTitle());
    }

    @Test
    void getTodoByIdSuccessfully() {
        String url = BASE_URL + "/api/todos/{id}";
        UUID id = UUID.randomUUID();
        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle("Test Todo");

        restTemplate.postForEntity(BASE_URL + "/api/todos", todo, Todo.class);

        ResponseEntity<Todo> response = restTemplate.getForEntity(url, Todo.class, id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(todo.getTitle(), response.getBody().getTitle());
    }

    @Test
    void getTodoByIdNotFound() {
        String url = BASE_URL + "/api/todos/{id}";
        UUID id = UUID.randomUUID();
        
        assertThrows(HttpClientErrorException.NotFound.class, () -> restTemplate.getForEntity(url, Todo.class, id));
    }

    @Test
    void getAllTodosSuccessfully() {
        String url = BASE_URL + "/api/todos";

        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void deleteTodoSuccessfully() {
        String url = BASE_URL + "/api/todos/{id}";
        UUID id = UUID.randomUUID();
        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle("Test Todo");

        restTemplate.postForEntity(BASE_URL + "/api/todos", todo, Todo.class);

        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class, id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}