package com.github.aws.tutorials.rag.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class S3Detail {
        private String version;
        private Map<String, String> userIdentity;
        private Bucket bucket;
        private S3Object object;
}
