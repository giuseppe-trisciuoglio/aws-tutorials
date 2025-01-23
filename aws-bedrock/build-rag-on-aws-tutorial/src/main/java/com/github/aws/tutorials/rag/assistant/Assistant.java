package com.github.aws.tutorials.rag.assistant;

import dev.langchain4j.service.SystemMessage;

public interface Assistant {
    @SystemMessage("Your name is Andrea. \n" +
            "Answer the questions as best you can. \n" +
            "If the information is not within your context window respond with a default message.\n" +
            "\n" +
            "Default message: 'Sorry this question can't be answered'")
    String chat(String userMessage);
}
