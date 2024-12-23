package com.github.aws.tutorials.lambda;

import com.github.aws.tutorials.lambda.controller.TodoController;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.boot.SpringApplication;

@SpringBootApplication
//@Import({ TodoController.class })
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
