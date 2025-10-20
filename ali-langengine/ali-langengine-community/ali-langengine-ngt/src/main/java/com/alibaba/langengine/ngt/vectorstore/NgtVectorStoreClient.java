package com.alibaba.langengine.ngt.vectorstore;

import com.alibaba.langengine.ngt.vectorstore.model.NgtSearchResult;

import java.util.List;

public interface NgtVectorStoreClient {

    void initialize(String indexName, NgtVectorStoreParam param, int dimension);

    int insert(float[] vector);

    List<NgtSearchResult> search(float[] vector, int topK, float epsilon, float radius);

    void remove(int objectId);

    void close();
}
