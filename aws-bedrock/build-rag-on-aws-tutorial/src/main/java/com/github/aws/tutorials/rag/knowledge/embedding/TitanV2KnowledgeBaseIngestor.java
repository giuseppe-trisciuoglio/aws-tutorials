package com.github.aws.tutorials.rag.knowledge.embedding;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TitanV2KnowledgeBaseIngestor implements KnowledgeBaseIngestor {
    private final EmbeddingStoreIngestor ingestor;

    /*public TitanEmbeddingStoreIngestorProcessor() {
        this.titanEmbeddingModel = BedrockTitanEmbeddingModel.builder()
                .region(Region.of(EnvUtils.getDefaultAwsRegion()))
                .model("amazon.titan-embed-text-v2:0")
                .build();

        this.embeddingStore = OpenSearchEmbeddingStore.builder()
                .serverUrl(EnvUtils.getOpenSearchUrl())
                .region(EnvUtils.getDefaultAwsRegion())
                .serviceName("aoss")
                .options(AwsSdk2TransportOptions.builder().build())
                .build();
        
        this.ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(450, 0))
                .embeddingModel(this.titanEmbeddingModel)
                .embeddingStore(this.embeddingStore)
                .build();
    }*/

    @Override
    public void ingest(Document document) {
        ingestor.ingest(document);
    }
}
