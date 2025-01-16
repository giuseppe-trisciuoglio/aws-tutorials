package com.github.aws.tutorials.rag.knowledge.embedding;

import com.github.aws.tutorials.rag.utility.EnvUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.bedrock.BedrockTitanEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TitanV2KnowledgeBaseIngestor implements KnowledgeBaseIngestor {
    private final EmbeddingStoreIngestor ingestor;

    public TitanV2KnowledgeBaseIngestor() {
        BedrockTitanEmbeddingModel titanEmbeddingModel = BedrockTitanEmbeddingModel.builder()
                .region(EnvUtils.getDefaultAwsRegion())
                .model("amazon.titan-embed-text-v2:0")
                .build();

        this.ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(450, 0))
                .embeddingModel(titanEmbeddingModel)
                .embeddingStore(EnvUtils.getEmbeddingStore())
                .build();
    }

    @Override
    public void ingest(Document document) {
        ingestor.ingest(document);
    }
}
