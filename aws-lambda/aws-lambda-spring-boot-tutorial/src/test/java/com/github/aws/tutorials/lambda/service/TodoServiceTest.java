package com.github.aws.tutorials.lambda.service;

import com.github.aws.tutorials.lambda.model.Todo;
import com.github.aws.tutorials.lambda.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @InjectMocks
    private TodoService todoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createTodoSuccessfully() {
        Todo todo = new Todo();
        when(todoRepository.createTodo(todo)).thenReturn(todo);

        Todo result = todoService.createTodo(todo);

        assertNotNull(result);
        assertEquals(todo, result);
        verify(todoRepository, times(1)).createTodo(todo);
    }

    @Test
    void getTodoByIdSuccessfully() {
        UUID id = UUID.randomUUID();
        Todo todo = new Todo();
        when(todoRepository.getTodoById(id)).thenReturn(Optional.of(todo));

        Optional<Todo> result = todoService.getTodo(id);

        assertTrue(result.isPresent());
        assertEquals(todo, result.get());
        verify(todoRepository, times(1)).getTodoById(id);
    }

    @Test
    void getTodoByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(todoRepository.getTodoById(id)).thenReturn(Optional.empty());

        Optional<Todo> result = todoService.getTodo(id);

        assertFalse(result.isPresent());
        verify(todoRepository, times(1)).getTodoById(id);
    }

    @Test
    void getAllTodosSuccessfully() {
        List<Todo> todos = List.of(new Todo(), new Todo());
        when(todoRepository.getAllTodos()).thenReturn(todos);

        List<Todo> result = todoService.getAllTodos();

        assertNotNull(result);
        assertEquals(todos.size(), result.size());
        verify(todoRepository, times(1)).getAllTodos();
    }

    @Test
    void deleteTodoSuccessfully() {
        UUID id = UUID.randomUUID();
        doNothing().when(todoRepository).deleteTodo(id);

        todoService.deleteTodo(id);

        verify(todoRepository, times(1)).deleteTodo(id);
    }


    @Test
    void updateTodoSuccessfully() {
        Todo todo = new Todo();
        when(todoRepository.updateTodo(todo)).thenReturn(todo);

        Todo result = todoService.updateTodo(todo);

        assertNotNull(result);
        assertEquals(todo, result);
        verify(todoRepository, times(1)).updateTodo(todo);
    }

    @Test
    void updateTodoNotFound() {
        Todo todo = new Todo();
        when(todoRepository.updateTodo(todo)).thenReturn(null);

        Todo result = todoService.updateTodo(todo);

        assertNull(result);
        verify(todoRepository, times(1)).updateTodo(todo);
    }
}