package com.github.aws.tutorials.rag.utility;

public final class EnvUtils {
    private EnvUtils() {
        throw new IllegalStateException("Utility class");
    }
    public static String getDefaultAwsRegion() {
        String awsRegion = System.getenv("AWS_REGION");
        return awsRegion != null ? awsRegion : "eu-west-1";
    }

    public static String getOpenSearchUrl() {
        String openSearchUrl = System.getenv("OPENSEARCH_URL");
        if (openSearchUrl == null || openSearchUrl.isEmpty()) {
            throw new IllegalArgumentException("OPENSEARCH_URL environment variable is not set");
        }
        return openSearchUrl;
    }
}
