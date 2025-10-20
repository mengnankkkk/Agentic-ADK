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
package com.alibaba.langengine.sqlitevss.vectorstore.service;

import com.alibaba.langengine.sqlitevss.vectorstore.SqliteVssException;
import com.alibaba.langengine.sqlitevss.vectorstore.SqliteVssParam;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
public class SqliteVssClient implements AutoCloseable {

    private final SqliteVssParam param;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public SqliteVssClient(SqliteVssParam param) {
        this.param = param;
        this.objectMapper = new ObjectMapper();
        this.dataSource = createDataSource();
        initializeDatabase();
    }

    private DataSource createDataSource() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + param.getDbPath());
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(param.getMaxPoolSize());
            config.setConnectionTimeout(param.getConnectionTimeoutSeconds() * 1000);
            config.setIdleTimeout(param.getMaxIdleTimeSeconds() * 1000);
            config.setConnectionTestQuery("SELECT 1");

            // SQLite specific settings
            Properties props = new Properties();
            if (param.isEnableWalMode()) {
                props.setProperty("journal_mode", "WAL");
            }
            if (param.isEnableForeignKeys()) {
                props.setProperty("foreign_keys", "true");
            }
            props.setProperty("page_size", String.valueOf(param.getPageSize()));
            props.setProperty("cache_size", String.valueOf(param.getCacheSize()));
            config.setDataSourceProperties(props);

            return new HikariDataSource(config);
        } catch (Exception e) {
            throw SqliteVssException.connectionError("Failed to create data source", e);
        }
    }

    private void initializeDatabase() {
        if (param.isAutoCreateTable()) {
            createCollectionIfNotExists(param.getCollectionName());
        }
    }

    /**
     * 验证集合名称，防止SQL注入攻击
     * 集合名称只能包含字母、数字、下划线，不能以数字开头，长度不超过64个字符
     * 
     * @param collectionName 集合名称
     * @throws SqliteVssException 如果集合名称无效
     */
    private void validateCollectionName(String collectionName) {
        if (StringUtils.isBlank(collectionName)) {
            throw SqliteVssException.configError("Collection name cannot be null or empty");
        }
        
        if (collectionName.length() > 64) {
            throw SqliteVssException.configError("Collection name cannot exceed 64 characters");
        }
        
        // 只允许字母、数字、下划线，且不能以数字开头
        if (!collectionName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw SqliteVssException.configError(
                "Collection name can only contain letters, numbers, and underscores, and cannot start with a number. Invalid name: " + collectionName);
        }
        
        // 检查SQL关键字黑名单
        String upperCaseName = collectionName.toUpperCase();
        String[] sqlKeywords = {
            "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "INDEX", 
            "TABLE", "DATABASE", "SCHEMA", "VIEW", "TRIGGER", "FUNCTION", "PROCEDURE",
            "UNION", "WHERE", "ORDER", "GROUP", "HAVING", "JOIN", "ON", "AS"
        };
        
        for (String keyword : sqlKeywords) {
            if (upperCaseName.equals(keyword)) {
                throw SqliteVssException.configError("Collection name cannot be a SQL keyword: " + collectionName);
            }
        }
    }

    public void createCollectionIfNotExists(String collectionName) {
        String createTableSql = String.format(
            "CREATE TABLE IF NOT EXISTS %s (" +
            "id TEXT PRIMARY KEY," +
            "content TEXT NOT NULL," +
            "vector BLOB," +
            "metadata TEXT," +
            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")", collectionName);

        String createIndexSql = String.format(
            "CREATE INDEX IF NOT EXISTS idx_%s_created_at ON %s(created_at)",
            collectionName, collectionName);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSql);
            stmt.execute(createIndexSql);

            if (param.getIndexParam().isCreateIndexOnStartup()) {
                createVectorIndex(collectionName);
            }

            log.info("Collection '{}' created successfully", collectionName);
        } catch (SQLException e) {
            throw SqliteVssException.sqlError("Failed to create collection: " + collectionName, e);
        }
    }

    private void createVectorIndex(String collectionName) {
        validateCollectionName(collectionName);
        // SQLite-VSS extension would be loaded here in a real implementation
        // For now, we'll just log that we would create a vector index
        log.info("Vector index would be created for collection '{}' with extension", collectionName);
    }

    public void insertDocument(SqliteVssInsertRequest request) {
        validateCollectionName(request.getCollectionName());
        
        String sql = request.isUpsert() 
            ? String.format("INSERT OR REPLACE INTO %s (id, content, vector, metadata, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)", request.getCollectionName())
            : String.format("INSERT INTO %s (id, content, vector, metadata) VALUES (?, ?, ?, ?)", request.getCollectionName());

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, request.getId());
            stmt.setString(2, request.getContent());
            
            // Serialize vector as JSON for storage
            if (request.getVector() != null) {
                String vectorJson = objectMapper.writeValueAsString(request.getVector());
                stmt.setString(3, vectorJson);
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }
            
            // Serialize metadata as JSON
            if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
                String metadataJson = objectMapper.writeValueAsString(request.getMetadata());
                stmt.setString(4, metadataJson);
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw SqliteVssException.documentError("Failed to insert document with id: " + request.getId(), null);
            }

            log.debug("Document inserted successfully: {}", request.getId());
        } catch (Exception e) {
            throw SqliteVssException.documentError("Failed to insert document", e);
        }
    }

    public void insertDocumentsBatch(List<SqliteVssInsertRequest> requests) {
        if (requests.isEmpty()) {
            return;
        }

        String collectionName = requests.get(0).getCollectionName();
        validateCollectionName(collectionName);
        
        boolean isUpsert = requests.get(0).isUpsert();
        
        String sql = isUpsert 
            ? String.format("INSERT OR REPLACE INTO %s (id, content, vector, metadata, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)", collectionName)
            : String.format("INSERT INTO %s (id, content, vector, metadata) VALUES (?, ?, ?, ?)", collectionName);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (SqliteVssInsertRequest request : requests) {
                stmt.setString(1, request.getId());
                stmt.setString(2, request.getContent());
                
                if (request.getVector() != null) {
                    String vectorJson = objectMapper.writeValueAsString(request.getVector());
                    stmt.setString(3, vectorJson);
                } else {
                    stmt.setNull(3, Types.VARCHAR);
                }
                
                if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
                    String metadataJson = objectMapper.writeValueAsString(request.getMetadata());
                    stmt.setString(4, metadataJson);
                } else {
                    stmt.setNull(4, Types.VARCHAR);
                }

                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            conn.commit();

            log.info("Batch inserted {} documents into collection '{}'", results.length, collectionName);
        } catch (Exception e) {
            throw SqliteVssException.documentError("Failed to insert documents batch", e);
        }
    }

    public SqliteVssSearchResponse searchSimilarDocuments(SqliteVssSearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        // For similarity search, we need to calculate distances in SQL
        // This is a simplified implementation - in practice, you'd use SQLite-VSS extension
        String sql = buildSearchSql(request);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setSearchParameters(stmt, request);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<SqliteVssSearchResponse.SearchResult> results = new ArrayList<>();
                
                while (rs.next()) {
                    SqliteVssSearchResponse.SearchResult result = SqliteVssSearchResponse.SearchResult.builder()
                        .id(rs.getString("id"))
                        .content(rs.getString("content"))
                        .metadata(parseMetadata(rs.getString("metadata")))
                        .score(request.isIncludeDistances() ? rs.getDouble("distance") : null)
                        .vector(request.isIncludeVectors() ? parseVector(rs.getString("vector")) : null)
                        .build();
                    
                    results.add(result);
                }
                
                long executionTime = System.currentTimeMillis() - startTime;
                
                return SqliteVssSearchResponse.builder()
                    .results(results)
                    .totalResults(results.size())
                    .executionTimeMs(executionTime)
                    .build();
            }
        } catch (Exception e) {
            throw SqliteVssException.searchError("Failed to search documents", e);
        }
    }

    private String buildSearchSql(SqliteVssSearchRequest request) {
        validateCollectionName(request.getCollectionName());
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, content, metadata, vector");
        
        if (request.isIncludeDistances()) {
            // Simplified distance calculation - in practice, use SQLite-VSS functions
            sql.append(", 0.0 as distance");
        }
        
        sql.append(" FROM ").append(request.getCollectionName());
        sql.append(" WHERE 1=1");
        
        // Add metadata filters
        if (request.getMetadataFilter() != null && !request.getMetadataFilter().isEmpty()) {
            for (String key : request.getMetadataFilter().keySet()) {
                sql.append(" AND JSON_EXTRACT(metadata, '$.").append(key).append("') = ?");
            }
        }
        
        // For exact search without vector similarity (simplified)
        if (StringUtils.isNotBlank(request.getQueryText())) {
            sql.append(" AND content LIKE ?");
        }
        
        sql.append(" ORDER BY created_at DESC");
        sql.append(" LIMIT ?");
        
        return sql.toString();
    }

    private void setSearchParameters(PreparedStatement stmt, SqliteVssSearchRequest request) throws SQLException {
        int paramIndex = 1;
        
        // Set metadata filter parameters
        if (request.getMetadataFilter() != null && !request.getMetadataFilter().isEmpty()) {
            for (Object value : request.getMetadataFilter().values()) {
                stmt.setObject(paramIndex++, value);
            }
        }
        
        // Set text search parameter
        if (StringUtils.isNotBlank(request.getQueryText())) {
            stmt.setString(paramIndex++, "%" + request.getQueryText() + "%");
        }
        
        // Set limit
        stmt.setInt(paramIndex, request.getTopK());
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (StringUtils.isBlank(metadataJson)) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse metadata JSON: {}", metadataJson);
            return new HashMap<>();
        }
    }

    private List<Double> parseVector(String vectorJson) {
        if (StringUtils.isBlank(vectorJson)) {
            return null;
        }
        
        try {
            return objectMapper.readValue(vectorJson, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse vector JSON: {}", vectorJson);
            return null;
        }
    }

    public boolean deleteDocument(String collectionName, String id) {
        validateCollectionName(collectionName);
        
        String sql = String.format("DELETE FROM %s WHERE id = ?", collectionName);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            int rows = stmt.executeUpdate();
            
            log.debug("Deleted {} document(s) with id: {}", rows, id);
            return rows > 0;
        } catch (SQLException e) {
            throw SqliteVssException.sqlError("Failed to delete document with id: " + id, e);
        }
    }

    public long getDocumentCount(String collectionName) {
        String sql = String.format("SELECT COUNT(*) FROM %s", collectionName);
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw SqliteVssException.sqlError("Failed to get document count", e);
        }
    }

    @Override
    public void close() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            log.info("SQLite-VSS client closed successfully");
        }
    }
}
