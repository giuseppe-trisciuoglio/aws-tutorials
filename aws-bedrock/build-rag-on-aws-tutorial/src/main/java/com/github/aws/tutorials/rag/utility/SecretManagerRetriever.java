package com.github.aws.tutorials.rag.utility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

@RequiredArgsConstructor
@Slf4j
public class SecretManagerRetriever {
    private final SecretsManagerClient client;
    
    public SecretManagerRetriever() {
        this.client = SecretsManagerClient.builder()
                .region(EnvUtils.getDefaultAwsRegion())
                .build();
    }
    
    public String getSecret(String secretName) {
        try {
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            GetSecretValueResponse valueResponse = client.getSecretValue(valueRequest);
            return valueResponse.secretString();

        } catch (SecretsManagerException e) {
            log.error("Error retrieving secret", e);
            throw new RuntimeException("Error retrieving secret", e);
        }
    }
}
