package com.github.aws.tutorials.rag.utility;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class ResponseUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static Map<String, Object> createResponse(int statusCode, 
                                                     String message, 
                                                     Object data) {
        final Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);

        final Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Access-Control-Allow-Origin", "*");
        responseHeaders.put("Access-Control-Allow-Headers", "Content-Type");
        responseHeaders.put("Access-Control-Allow-Methods", "OPTIONS,POST,GET,PUT,DELETE");
        response.put("headers", responseHeaders);

        final Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("message", message);
        if (data != null) {
            bodyMap.put("data", data);
        }

        try {
            response.put("body", objectMapper.writeValueAsString(bodyMap));
        } catch (Exception e) {
            response.put("body", "{\"message\": \"Error creating response\"}");
        }

        return response;
    }
}
