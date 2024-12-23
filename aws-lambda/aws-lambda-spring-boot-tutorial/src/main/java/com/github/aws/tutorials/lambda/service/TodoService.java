package com.github.aws.tutorials.lambda.service;

import com.github.aws.tutorials.lambda.model.Todo;
import com.github.aws.tutorials.lambda.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TodoService {
    private final TodoRepository todoRepository;

    public Todo createTodo(Todo todo) {
        return todoRepository.createTodo(todo);
    }
    
    public Todo updateTodo(Todo todo) {
        return todoRepository.updateTodo(todo);
    }

    public Optional<Todo> getTodo(UUID id) {
        return todoRepository.getTodoById(id);
    }

    public List<Todo> getAllTodos() {
        return todoRepository.getAllTodos();
    }

    public void deleteTodo(UUID id) {
        todoRepository.deleteTodo(id);
    }
}
