package com.github.aws.tutorials.lambda.controller;

import com.github.aws.tutorials.lambda.model.Todo;
import com.github.aws.tutorials.lambda.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TodoControllerTest {

    @Mock
    private TodoService todoService;

    @InjectMocks
    private TodoController todoController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createTodoSuccessfully() {
        Todo todo = new Todo();
        when(todoService.createTodo(todo)).thenReturn(todo);

        ResponseEntity<Todo> response = todoController.createTodo(todo);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(todo, response.getBody());
        verify(todoService, times(1)).createTodo(todo);
    }

    @Test
    void getTodoByIdSuccessfully() {
        UUID id = UUID.randomUUID();
        Todo todo = new Todo();
        when(todoService.getTodo(id)).thenReturn(Optional.of(todo));

        ResponseEntity<Todo> response = todoController.getTodo(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(todo, response.getBody());
        verify(todoService, times(1)).getTodo(id);
    }

    @Test
    void getTodoByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(todoService.getTodo(id)).thenReturn(Optional.empty());

        ResponseEntity<Todo> response = todoController.getTodo(id);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(todoService, times(1)).getTodo(id);
    }

    @Test
    void getAllTodosSuccessfully() {
        List<Todo> todos = List.of(new Todo(), new Todo());
        when(todoService.getAllTodos()).thenReturn(todos);

        ResponseEntity<List<Todo>> response = todoController.getAllTodos();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(todos.size(), response.getBody().size());
        verify(todoService, times(1)).getAllTodos();
    }

    @Test
    void deleteTodoSuccessfully() {
        UUID id = UUID.randomUUID();
        doNothing().when(todoService).deleteTodo(id);

        ResponseEntity<Void> response = todoController.deleteTodo(id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(todoService, times(1)).deleteTodo(id);
    }
}