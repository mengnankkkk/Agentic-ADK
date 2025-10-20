package com.alibaba.langengine.ngt.vectorstore;

import com.alibaba.fastjson.JSON;
import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.core.vectorstore.VectorStore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class Ngt extends VectorStore {

    private Embeddings embedding;
    private final String indexName;
    private final NgtVectorStoreParam param;
    private final NgtVectorStoreService service;

    public Ngt(String indexName) {
        this(indexName, null, null);
    }

    public Ngt(String indexName, NgtVectorStoreParam param) {
        this(indexName, param, null);
    }

    Ngt(String indexName, NgtVectorStoreParam param, NgtVectorStoreService service) {
        this.indexName = indexName;
        this.param = param != null ? param : NgtVectorStoreParam.builder().build();
        this.service = service != null ? service : new NgtVectorStoreService(indexName, this.param);
    }

    public void init() {
        try {
            service.init(embedding);
        } catch (Exception e) {
            log.error("Failed to initialize NGT index", e);
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INDEX_INITIALIZATION_FAILED,
                    "Failed to initialize NGT index", e);
        }
    }

    @Override
    public void addDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }

        try {
            List<Document> embeddedDocs = embedding.embedDocument(documents);
            service.addDocuments(embeddedDocs);
        } catch (Exception e) {
            log.error("Failed to add documents to NGT", e);
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INSERT_FAILED,
                    "Failed to add documents", e);
        }
    }

    @Override
    public List<Document> similaritySearch(String query, int k, Double maxDistanceValue, Integer type) {
        try {
            List<String> embeddingStrings = embedding.embedQuery(query, k);
            if (CollectionUtils.isEmpty(embeddingStrings)) {
                return List.of();
            }

            List<Double> queryEmbedding = parseEmbedding(embeddingStrings.get(0));
            return service.similaritySearch(queryEmbedding, k, maxDistanceValue);
        } catch (Exception e) {
            log.error("Failed to perform NGT similarity search", e);
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.SEARCH_FAILED,
                    "Failed to perform similarity search", e);
        }
    }

    public List<Document> similaritySearchByVector(List<Double> embeddingVector, int k, Double maxDistanceValue) {
        try {
            return service.similaritySearch(embeddingVector, k, maxDistanceValue);
        } catch (Exception e) {
            log.error("Failed to perform NGT vector search", e);
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.SEARCH_FAILED,
                    "Failed to perform vector search", e);
        }
    }

    public void delete(List<String> ids) {
        try {
            service.deleteDocuments(ids);
        } catch (Exception e) {
            log.error("Failed to delete NGT documents", e);
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.DELETE_FAILED,
                    "Failed to delete documents", e);
        }
    }

    public void close() {
        try {
            service.close();
        } catch (Exception e) {
            log.warn("Failed to close NGT resources", e);
        }
    }

    private List<Double> parseEmbedding(String embeddingValue) {
        if (embeddingValue == null) {
            return List.of();
        }
        try {
            return JSON.parseArray(embeddingValue, Double.class);
        } catch (Exception ex) {
            List<Double> values = new ArrayList<>();
            for (String part : embeddingValue.split(",")) {
                try {
                    values.add(Double.parseDouble(part.trim()));
                } catch (NumberFormatException ignore) {
                }
            }
            return values;
        }
    }
}
