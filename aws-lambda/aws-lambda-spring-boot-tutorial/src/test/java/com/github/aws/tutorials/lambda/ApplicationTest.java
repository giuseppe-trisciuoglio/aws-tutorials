package com.github.aws.tutorials.lambda;

import com.github.aws.tutorials.lambda.controller.TodoController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ApplicationTest {
    @MockBean
    private TodoController todoController;

    @Test
    void todoControllerIsLoaded() {
        assertThat(todoController).isNotNull();
    }
}