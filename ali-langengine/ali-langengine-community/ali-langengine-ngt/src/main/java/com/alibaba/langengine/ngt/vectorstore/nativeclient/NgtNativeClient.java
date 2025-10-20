package com.alibaba.langengine.ngt.vectorstore.nativeclient;

import com.alibaba.langengine.ngt.vectorstore.NgtVectorStoreClient;
import com.alibaba.langengine.ngt.vectorstore.NgtVectorStoreException;
import com.alibaba.langengine.ngt.vectorstore.NgtVectorStoreParam;
import com.alibaba.langengine.ngt.vectorstore.model.NgtSearchResult;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class NgtNativeClient implements NgtVectorStoreClient {

    private final NgtNativeLibrary library;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private Pointer indexPointer;
    private Pointer errorPointer;
    private NgtVectorStoreParam param;
    private int dimension;
    private String indexPath;

    public NgtNativeClient(NgtNativeLibrary library) {
        this.library = library;
    }

    @Override
    public void initialize(String indexName, NgtVectorStoreParam param, int dimension) {
        lock.writeLock().lock();
        try {
            if (indexPointer != null) {
                return;
            }

            param.validate();
            this.param = param;
            this.dimension = dimension;

            errorPointer = library.ngt_create_error_object();
            if (errorPointer == null) {
                throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.NATIVE_LIBRARY_LOAD_FAILED,
                        "Failed to create NGT error object");
            }

            Path basePath = Paths.get(param.getIndexPath(), indexName);
            Path parent = basePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            this.indexPath = basePath.toAbsolutePath().toString();

            if (Files.exists(basePath)) {
                indexPointer = library.ngt_open_index(indexPath, errorPointer);
                handleError("Failed to open NGT index at " + indexPath);
                if (log.isInfoEnabled()) {
                    log.info("Opened existing NGT index: {}", indexPath);
                }
                return;
            }

            Pointer property = library.ngt_create_property(errorPointer);
            handleError("Failed to create NGT property");

            try {
                boolean dimensionSet = library.ngt_set_property_dimension(property, dimension, errorPointer);
                handleError("Failed to set NGT property dimension");
                if (!dimensionSet) {
                    throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INVALID_CONFIGURATION,
                            "NGT failed to set property dimension");
                }

                boolean distanceTypeSet = library.ngt_set_property_distance_type(property,
                        param.distanceTypeCode(), errorPointer);
                handleError("Failed to set NGT distance type");
                if (!distanceTypeSet) {
                    throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INVALID_CONFIGURATION,
                            "NGT failed to set distance type");
                }

                indexPointer = library.ngt_create_graph_and_tree(indexPath, property, errorPointer);
                handleError("Failed to create NGT index at " + indexPath);
                if (indexPointer == null) {
                    throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INDEX_INITIALIZATION_FAILED,
                            "NGT returned null index pointer");
                }

                boolean created = library.ngt_create_index(indexPointer, 0, errorPointer);
                handleError("Failed to build NGT index structure");
                if (!created) {
                    throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INDEX_INITIALIZATION_FAILED,
                            "NGT index creation returned false");
                }

                boolean saved = library.ngt_save_index(indexPointer, indexPath, true, errorPointer);
                handleError("Failed to save NGT index");
                if (!saved) {
                    throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INDEX_INITIALIZATION_FAILED,
                            "NGT save index returned false");
                }

                if (log.isInfoEnabled()) {
                    log.info("Created new NGT index: {}", indexPath);
                }
            } finally {
                if (property != null) {
                    library.ngt_destroy_property(property);
                }
            }
        } catch (Exception ex) {
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INDEX_INITIALIZATION_FAILED,
                    "Failed to initialize NGT index", ex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int insert(float[] vector) {
        lock.writeLock().lock();
        try {
            ensureInitialized();
            if (vector == null || vector.length != dimension) {
                throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.DIMENSION_MISMATCH,
                        "Vector dimension mismatch. Expected " + dimension + " got " + (vector == null ? 0 : vector.length));
            }

            int objectId = library.ngt_insert_index_as_float(indexPointer, vector, vector.length, errorPointer);
            handleError("Failed to insert vector into NGT index");

            if (objectId <= 0) {
                throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INSERT_FAILED,
                        "NGT insert returned invalid object id: " + objectId);
            }

            if (param.isAutoBuildIndex()) {
                boolean created = library.ngt_create_index(indexPointer, 0, errorPointer);
                handleError("Failed to rebuild NGT index");
                if (!created) {
                    throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INSERT_FAILED,
                            "NGT failed to rebuild index after insert");
                }

                library.ngt_save_index(indexPointer, indexPath, true, errorPointer);
                handleError("Failed to save NGT index after insert");
            }

            return objectId;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<NgtSearchResult> search(float[] vector, int topK, float epsilon, float radius) {
        lock.readLock().lock();
        Pointer results = null;
        try {
            ensureInitialized();
            if (vector == null || vector.length != dimension) {
                throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.DIMENSION_MISMATCH,
                        "Query vector dimension mismatch. Expected " + dimension + " got " + (vector == null ? 0 : vector.length));
            }

            results = library.ngt_create_empty_results(errorPointer);
            handleError("Failed to create NGT results container");
            if (results == null) {
                throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.SEARCH_FAILED,
                        "NGT returned null results pointer");
            }

            boolean success = library.ngt_search_index_as_float(indexPointer, vector, vector.length,
                    topK, epsilon, radius, results, errorPointer);
            handleError("Failed to perform NGT search");
            if (!success) {
                throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.SEARCH_FAILED,
                        "NGT search returned false");
            }

            int size = library.ngt_get_result_size(results, errorPointer);
            handleError("Failed to get NGT result size");
            if (size < 0) {
                return List.of();
            }

            List<NgtSearchResult> searchResults = new ArrayList<>(Math.min(size, topK));
            for (int i = 0; i < size; i++) {
                NgtNativeLibrary.NgtObjectDistance.ByValue distance = library.ngt_get_result(results, i, errorPointer);
                handleError("Failed to read NGT search result");
                searchResults.add(new NgtSearchResult(distance.id, distance.distance));
            }
            return searchResults;
        } finally {
            if (results != null) {
                library.ngt_destroy_results(results);
            }
            lock.readLock().unlock();
        }
    }

    @Override
    public void remove(int objectId) {
        lock.writeLock().lock();
        try {
            ensureInitialized();
            boolean removed = library.ngt_remove_index(indexPointer, new NativeLong(objectId), errorPointer);
            handleError("Failed to remove object from NGT index");
            if (!removed) {
                throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.DELETE_FAILED,
                        "NGT remove returned false for object " + objectId);
            }

            if (param.isAutoBuildIndex()) {
                library.ngt_create_index(indexPointer, 0, errorPointer);
                handleError("Failed to rebuild NGT index after removal");
                library.ngt_save_index(indexPointer, indexPath, true, errorPointer);
                handleError("Failed to save NGT index after removal");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            if (indexPointer != null) {
                library.ngt_close_index(indexPointer);
                indexPointer = null;
            }
        } finally {
            if (errorPointer != null) {
                library.ngt_destroy_error_object(errorPointer);
                errorPointer = null;
            }
            lock.writeLock().unlock();
        }
    }

    private void ensureInitialized() {
        if (indexPointer == null) {
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.INDEX_NOT_INITIALIZED,
                    "NGT client has not been initialized");
        }
    }

    private void handleError(String message) {
        if (errorPointer == null) {
            return;
        }
        String error = library.ngt_get_error_string(errorPointer);
        if (error != null && !error.isBlank()) {
            library.ngt_clear_error_string(errorPointer);
            throw new NgtVectorStoreException(NgtVectorStoreException.ErrorCodes.NATIVE_LIBRARY_LOAD_FAILED,
                    message + ": " + error);
        }
    }
}
