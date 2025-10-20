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
package com.alibaba.langengine.sqlitevss;

import com.alibaba.langengine.core.util.WorkPropertiesUtils;
import com.alibaba.langengine.sqlitevss.vectorstore.SqliteVssException;
import lombok.extern.slf4j.Slf4j;


/**
 * 配置管理类 - 提供类型安全的配置加载
 * 支持从环境变量和配置文件加载参数
 * 
 * @author xiaoxuan.lp
 */
@Slf4j
public class SqliteVssConfiguration {

    private static final String DEFAULT_DB_PATH = "vectorstore.db";
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 30;
    private static final int DEFAULT_MAX_IDLE_TIME = 600;

    /**
     * 获取数据库文件路径
     * 
     * @return 数据库文件路径
     */
    public static String getDbPath() {
        String path = WorkPropertiesUtils.get("sqlite_vss_db_path");
        return path != null ? path : DEFAULT_DB_PATH;
    }

    /**
     * 获取连接池最大大小
     * 
     * @return 连接池最大大小
     */
    public static int getMaxPoolSize() {
        String value = WorkPropertiesUtils.get("sqlite_vss_max_pool_size");
        if (value == null) {
            return DEFAULT_MAX_POOL_SIZE;
        }
        
        try {
            int poolSize = Integer.parseInt(value);
            if (poolSize <= 0) {
                log.warn("Invalid max pool size: {}, using default: {}", value, DEFAULT_MAX_POOL_SIZE);
                return DEFAULT_MAX_POOL_SIZE;
            }
            return poolSize;
        } catch (NumberFormatException e) {
            log.warn("Invalid max pool size format: {}, using default: {}", value, DEFAULT_MAX_POOL_SIZE);
            return DEFAULT_MAX_POOL_SIZE;
        }
    }

    /**
     * 获取连接超时时间（秒）
     * 
     * @return 连接超时时间
     */
    public static int getConnectionTimeout() {
        String value = WorkPropertiesUtils.get("sqlite_vss_connection_timeout");
        if (value == null) {
            return DEFAULT_CONNECTION_TIMEOUT;
        }
        
        try {
            int timeout = Integer.parseInt(value);
            if (timeout <= 0) {
                log.warn("Invalid connection timeout: {}, using default: {}", value, DEFAULT_CONNECTION_TIMEOUT);
                return DEFAULT_CONNECTION_TIMEOUT;
            }
            return timeout;
        } catch (NumberFormatException e) {
            log.warn("Invalid connection timeout format: {}, using default: {}", value, DEFAULT_CONNECTION_TIMEOUT);
            return DEFAULT_CONNECTION_TIMEOUT;
        }
    }

    /**
     * 获取最大空闲时间（秒）
     * 
     * @return 最大空闲时间
     */
    public static int getMaxIdleTime() {
        String value = WorkPropertiesUtils.get("sqlite_vss_max_idle_time");
        if (value == null) {
            return DEFAULT_MAX_IDLE_TIME;
        }
        
        try {
            int idleTime = Integer.parseInt(value);
            if (idleTime <= 0) {
                log.warn("Invalid max idle time: {}, using default: {}", value, DEFAULT_MAX_IDLE_TIME);
                return DEFAULT_MAX_IDLE_TIME;
            }
            return idleTime;
        } catch (NumberFormatException e) {
            log.warn("Invalid max idle time format: {}, using default: {}", value, DEFAULT_MAX_IDLE_TIME);
            return DEFAULT_MAX_IDLE_TIME;
        }
    }

    /**
     * 验证配置的有效性
     * 
     * @throws SqliteVssException 如果配置无效
     */
    public static void validateConfiguration() {
        // 验证基本配置参数
        if (getMaxPoolSize() <= 0) {
            throw SqliteVssException.configError("Max pool size must be greater than 0");
        }
        
        if (getConnectionTimeout() <= 0) {
            throw SqliteVssException.configError("Connection timeout must be greater than 0");
        }
        
        if (getMaxIdleTime() <= 0) {
            throw SqliteVssException.configError("Max idle time must be greater than 0");
        }
        
        log.debug("Configuration validated successfully: dbPath={}, maxPoolSize={}, connectionTimeout={}, maxIdleTime={}", 
                getDbPath(), getMaxPoolSize(), getConnectionTimeout(), getMaxIdleTime());
    }
}
