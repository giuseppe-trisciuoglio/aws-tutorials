package com.github.aws.tutorials.rag.knowledge.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
public class S3Detail {
        private String version;
        private Map<String, String> userIdentity;
        private Bucket bucket;
        private S3Object object;
}
