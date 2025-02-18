package com.github.aws.tutorials.rag.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class S3Object {
    private String key;
    private Long size;
    private String etag;
}
