package com.github.aws.tutorials.rag.knowledge.embedding;

import com.github.aws.tutorials.rag.utility.EnvUtils;
import com.github.aws.tutorials.rag.utility.SecretManagerRetriever;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;

@RequiredArgsConstructor
@Slf4j
public class OpenAIKnowledgeBaseIngestor implements KnowledgeBaseIngestor {
    private static final String OPENAI_API_KEY = "openai-api-key";

    private final EmbeddingStoreIngestor ingestor;
    
    public OpenAIKnowledgeBaseIngestor() {
        SecretManagerRetriever secretManagerRetriever = new SecretManagerRetriever();
        EmbeddingModel openAiEmbeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(secretManagerRetriever.getSecret(OPENAI_API_KEY))
                .modelName(TEXT_EMBEDDING_3_SMALL)
                .build();

        this.ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(450, 0))
                .embeddingModel(openAiEmbeddingModel)
                .embeddingStore(EnvUtils.getEmbeddingStore())
                .build();
    }
    
    @Override
    public void ingest(Document document) {
        ingestor.ingest(document);
    }
}
