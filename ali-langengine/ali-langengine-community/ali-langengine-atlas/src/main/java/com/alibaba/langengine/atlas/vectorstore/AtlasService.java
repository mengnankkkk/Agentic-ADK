/**
 * Copyright (C) 2024 AIDC-AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.langengine.atlas.vectorstore;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.Closeable;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class AtlasService implements Closeable {

    private final AtlasClient client;
    private final AtlasVectorStoreParam param;

    public AtlasService(String connectionString, String databaseName, String collectionName, String indexName, AtlasVectorStoreParam param) {
        this(new AtlasClient(connectionString, databaseName, collectionName, indexName, param), param);
    }

    /**
     * Constructor for testing purposes.
     */
    AtlasService(AtlasClient client, AtlasVectorStoreParam param) {
        this.client = client;
        this.param = param;
    }

    public void addDocuments(List<com.alibaba.langengine.core.indexes.Document> langEngineDocuments) {
        if (CollectionUtils.isEmpty(langEngineDocuments)) {
            return;
        }
        List<Document> mongoDocuments = langEngineDocuments.stream()
                .map(this::toMongoDocument)
                .collect(Collectors.toList());
        client.insertDocuments(mongoDocuments);
    }

    public void deleteDocuments(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        client.deleteDocuments(ids);
    }

    public List<com.alibaba.langengine.core.indexes.Document> similaritySearch(List<Float> queryVector, int k) {
        List<Document> results = client.similaritySearch(queryVector, k);
        return results.stream()
                .map(this::toLangEngineDocument)
                .collect(Collectors.toList());
    }

    private Document toMongoDocument(com.alibaba.langengine.core.indexes.Document langEngineDoc) {
        Document mongoDoc = new Document();
        if (langEngineDoc.getUniqueId() != null && ObjectId.isValid(langEngineDoc.getUniqueId())) {
            mongoDoc.put(param.getUniqueIdPath(), new ObjectId(langEngineDoc.getUniqueId()));
        }
        mongoDoc.put(param.getTextPath(), langEngineDoc.getPageContent());
        mongoDoc.put(param.getVectorPath(), langEngineDoc.getEmbedding());
        mongoDoc.put(param.getMetadataPath(), langEngineDoc.getMetadata());
        return mongoDoc;
    }

    private com.alibaba.langengine.core.indexes.Document toLangEngineDocument(Document mongoDoc) {
        com.alibaba.langengine.core.indexes.Document langEngineDoc = new com.alibaba.langengine.core.indexes.Document();
        langEngineDoc.setUniqueId(mongoDoc.getObjectId(param.getUniqueIdPath()).toHexString());
        langEngineDoc.setPageContent(mongoDoc.getString(param.getTextPath()));

        // embeddings are not typically returned in search results to save bandwidth
        // langEngineDoc.setEmbedding(mongoDoc.getList(param.getVectorPath(), Double.class));

        Document metadataDoc = mongoDoc.get(param.getMetadataPath(), Document.class);
        if (metadataDoc != null) {
            langEngineDoc.setMetadata(metadataDoc);
        }
        if (mongoDoc.containsKey("score")) {
            langEngineDoc.setScore(mongoDoc.getDouble("score"));
        }
        return langEngineDoc;
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}