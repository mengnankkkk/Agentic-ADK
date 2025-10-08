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

import com.mongodb.client.*;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.search.FieldSearchPath;
import com.mongodb.client.model.search.VectorSearchOptions;
import lombok.extern.slf4j.Slf4j;
import org.bson.Bson;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.search.SearchPath.fieldPath;

@Slf4j
public class AtlasClient implements Closeable {

    private final MongoClient mongoClient;
    private final MongoCollection<Document> collection;
    private final String indexName;
    private final AtlasVectorStoreParam param;

    public AtlasClient(String connectionString, String databaseName, String collectionName, String indexName, AtlasVectorStoreParam param) {
        try {
            this.mongoClient = MongoClients.create(connectionString);
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            this.collection = database.getCollection(collectionName);
            this.indexName = indexName;
            this.param = param;
            log.info("Successfully connected to MongoDB Atlas.");
        } catch (Exception e) {
            throw new AtlasVectorStoreException("CONNECTION_FAILED", "Failed to connect to MongoDB Atlas.", e);
        }
    }

    public void insertDocuments(List<Document> documents) {
        try {
            collection.insertMany(documents);
        } catch (Exception e) {
            throw new AtlasVectorStoreException("INSERT_FAILED", "Failed to insert documents into Atlas.", e);
        }
    }

    public void deleteDocuments(List<String> ids) {
        try {
            List<ObjectId> objectIds = ids.stream().map(ObjectId::new).collect(Collectors.toList());
            collection.deleteMany(Filters.in("_id", objectIds));
        } catch (Exception e) {
            throw new AtlasVectorStoreException("DELETE_FAILED", "Failed to delete documents from Atlas.", e);
        }
    }

    public List<Document> similaritySearch(List<Float> queryVector, int limit) {
        try {
            FieldSearchPath fieldSearchPath = fieldPath(param.getVectorPath());
            VectorSearchOptions options = VectorSearchOptions.exactVectorSearchOptions();

            List<Bson> pipeline = Arrays.asList(
                    Aggregates.vectorSearch(
                            fieldSearchPath,
                            queryVector,
                            indexName,
                            param.getNumCandidates(),
                            limit,
                            options),
                    new Document("$addFields", new Document("score", new Document("$meta", "vectorSearchScore")))
            );

            List<Document> results = new ArrayList<>();
            collection.aggregate(pipeline).forEach(results::add);
            return results;

        } catch (Exception e) {
            throw new AtlasVectorStoreException("SEARCH_FAILED", "Failed to perform vector search in Atlas.", e);
        }
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                log.info("MongoDB Atlas client closed.");
            } catch (Exception e) {
                log.warn("Error closing MongoDB Atlas client.", e);
            }
        }
    }
}