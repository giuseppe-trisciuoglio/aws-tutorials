package com.github.aws.tutorials.rag.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class S3Event {
    @JsonProperty("detail")
    private S3Detail detail;
}
