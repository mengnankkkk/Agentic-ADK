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
package com.alibaba.langengine.turbopuffer.vectorstore;

import com.alibaba.langengine.core.indexes.Document;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


@Slf4j
public class BatchProcessor {
    
    private final TurbopufferParam.BatchConfig config;
    private final Consumer<List<Document>> batchHandler;
    private final BlockingQueue<Document> queue;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    public BatchProcessor(TurbopufferParam.BatchConfig config, Consumer<List<Document>> batchHandler) {
        this.config = config;
        this.batchHandler = batchHandler;
        this.queue = new LinkedBlockingQueue<>(config.maxQueueSize);
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "TurbopufferBatchProcessor");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * 启动批量处理器
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            // 启动批量处理任务
            scheduler.scheduleWithFixedDelay(this::processBatch, 0, config.batchTimeoutMs, TimeUnit.MILLISECONDS);
            log.info("Batch processor started with batch size: {}, timeout: {}ms", 
                    config.batchSize, config.batchTimeoutMs);
        }
    }
    
    /**
     * 停止批量处理器
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            // 处理剩余的文档
            processBatch();
            
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("Batch processor stopped");
        }
    }
    
    /**
     * 添加文档到批量队列
     */
    public boolean add(Document document) {
        if (!config.enableBatch) {
            // 如果未启用批量处理，直接处理单个文档
            try {
                List<Document> singleDocList = new ArrayList<>();
                singleDocList.add(document);
                batchHandler.accept(singleDocList);
                return true;
            } catch (Exception e) {
                log.error("Failed to process single document", e);
                return false;
            }
        }
        
        if (!running.get()) {
            start();
        }
        
        try {
            boolean added = queue.offer(document, 1, TimeUnit.SECONDS);
            if (!added) {
                log.warn("Failed to add document to batch queue - queue is full");
            }
            return added;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while adding document to batch queue", e);
            return false;
        }
    }
    
    /**
     * 批量添加文档
     */
    public boolean addAll(List<Document> documents) {
        if (!config.enableBatch) {
            // 如果未启用批量处理，直接处理
            try {
                batchHandler.accept(documents);
                return true;
            } catch (Exception e) {
                log.error("Failed to process document batch", e);
                return false;
            }
        }
        
        if (!running.get()) {
            start();
        }
        
        int added = 0;
        for (Document doc : documents) {
            if (queue.offer(doc)) {
                added++;
            } else {
                break;
            }
        }
        
        if (added != documents.size()) {
            log.warn("Only added {} out of {} documents to batch queue", added, documents.size());
        }
        
        return added > 0;
    }
    
    /**
     * 强制处理当前批次
     */
    public void flush() {
        processBatch();
    }
    
    /**
     * 处理批次
     */
    private void processBatch() {
        if (queue.isEmpty()) {
            return;
        }
        
        List<Document> batch = new ArrayList<>();
        
        // 从队列中取出文档
        int count = 0;
        while (count < config.batchSize && !queue.isEmpty()) {
            Document doc = queue.poll();
            if (doc != null) {
                batch.add(doc);
                count++;
            } else {
                break;
            }
        }
        
        if (!batch.isEmpty()) {
            try {
                batchHandler.accept(batch);
                log.debug("Processed batch of {} documents", batch.size());
            } catch (Exception e) {
                log.error("Failed to process batch of {} documents", batch.size(), e);
                // 可以考虑将失败的文档重新放回队列或记录到死信队列
            }
        }
    }
    
    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        return queue.size();
    }
    
    /**
     * 检查是否正在运行
     */
    public boolean isRunning() {
        return running.get();
    }
}
