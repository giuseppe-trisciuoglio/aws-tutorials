package com.github.aws.tutorials.rag.knowledge.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class S3Object {
    private String key;
    private Long size;
    private String etag;
}
