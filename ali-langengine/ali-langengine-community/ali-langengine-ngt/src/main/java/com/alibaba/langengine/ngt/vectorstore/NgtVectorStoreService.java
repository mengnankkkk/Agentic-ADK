package com.alibaba.langengine.ngt.vectorstore;

import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.ngt.NgtConfiguration;
import com.alibaba.langengine.ngt.vectorstore.model.NgtSearchResult;
import com.alibaba.langengine.ngt.vectorstore.nativeclient.NgtNativeClient;
import com.alibaba.langengine.ngt.vectorstore.nativeclient.NgtNativeLibrary;
import com.alibaba.langengine.ngt.vectorstore.nativeclient.NgtNativeLibraryLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class NgtVectorStoreService {

    private final String indexName;
    private final NgtVectorStoreParam param;
    private final NgtVectorStoreClient client;
    private final Map<Integer, Document> documentStore = new ConcurrentHashMap<>();

    private volatile boolean initialized = false;

    public NgtVectorStoreService(String indexName, NgtVectorStoreParam param) {
        this(indexName, param, null);
    }

    public NgtVectorStoreService(String indexName, NgtVectorStoreParam param, NgtVectorStoreClient client) {
        this.indexName = indexName;
        this.param = param != null ? param : NgtVectorStoreParam.builder().build();
        if (client != null) {
            this.client = client;
        } else {
            this.client = createNativeClient(this.param);
        }
    }

    private NgtVectorStoreClient createNativeClient(NgtVectorStoreParam param) {
        if (StringUtils.isNotBlank(NgtConfiguration.NGT_LIBRARY_PATH)) {
            System.setProperty("jna.library.path", NgtConfiguration.NGT_LIBRARY_PATH);
        }
        try {
            NgtNativeLibrary library = NgtNativeLibraryLoader.load(param.getNativeLibraryName());
            return new NgtNativeClient(library);
        } catch (UnsatisfiedLinkError error) {
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.NATIVE_LIBRARY_LOAD_FAILED,
                    "Failed to load NGT native library: " + error.getMessage(), error);
        }
    }

    public void init(Embeddings embeddings) {
        if (initialized) {
            return;
        }

        client.initialize(indexName, param, param.getDimension());
        initialized = true;
    }

    public void addDocuments(List<Document> documents) {
        ensureInitialized();
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }

        for (Document document : documents) {
            if (CollectionUtils.isEmpty(document.getEmbedding())) {
                continue;
            }

            float[] vector = toFloatArray(document.getEmbedding());
            int objectId = client.insert(vector);

            document.setUniqueId(String.valueOf(objectId));
            documentStore.put(objectId, document);
        }
    }

    public List<Document> similaritySearch(List<Double> queryEmbedding, int k, Double maxDistanceValue) {
        ensureInitialized();
        if (CollectionUtils.isEmpty(queryEmbedding)) {
            return List.of();
        }

        float[] queryVector = toFloatArray(queryEmbedding);
        int topK = Math.max(k, param.getSearchK());
        List<NgtSearchResult> results = client.search(queryVector, topK, param.getSearchEpsilon(), param.getSearchRadius());
        List<Document> documents = new ArrayList<>();
        for (NgtSearchResult result : results) {
            Document stored = documentStore.get(result.getObjectId());
            if (stored == null) {
                continue;
            }
            if (maxDistanceValue != null && result.getDistance() > maxDistanceValue) {
                continue;
            }

            Document copy = copyDocument(stored);
            copy.setScore((double) result.getDistance());
            documents.add(copy);
        }
        return documents;
    }

    public void deleteDocuments(List<String> ids) {
        ensureInitialized();
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        for (String id : ids) {
            if (StringUtils.isBlank(id)) {
                continue;
            }
            try {
                int objectId = Integer.parseInt(id);
                client.remove(objectId);
                documentStore.remove(objectId);
            } catch (NumberFormatException ex) {
                log.warn("Invalid NGT document id: {}", id);
            }
        }
    }

    public void close() {
        client.close();
        documentStore.clear();
        initialized = false;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INDEX_NOT_INITIALIZED,
                    "NGT vector store is not initialized");
        }
    }

    private float[] toFloatArray(List<Double> values) {
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i).floatValue();
        }
        return vector;
    }

    private Document copyDocument(Document source) {
        Document copy = new Document();
        copy.setUniqueId(source.getUniqueId());
        copy.setPageContent(source.getPageContent());
        copy.setSummary(source.getSummary());
        copy.setWholeContent(source.getWholeContent());
        copy.setMetadata(source.getMetadata() != null ? new ConcurrentHashMap<>(source.getMetadata()) : null);
        copy.setEmbedding(source.getEmbedding());
        copy.setIndex(source.getIndex());
        copy.setCategory(source.getCategory());
        copy.setScore(source.getScore());
        return copy;
    }

    NgtVectorStoreClient getClient() {
        return client;
    }

    Map<Integer, Document> getDocumentStore() {
        return documentStore;
    }
}
