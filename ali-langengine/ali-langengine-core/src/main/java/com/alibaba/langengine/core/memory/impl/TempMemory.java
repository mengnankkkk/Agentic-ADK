/**
 * Copyright (C) 2025 AIDC-AI
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
package com.alibaba.langengine.core.memory.impl;

import com.alibaba.langengine.core.memory.BaseMemory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 临时记忆清单系统（Temporary Memory）
 *
 * 功能特性：
 * - 支持临时信息的快速存储和检索
 * - 自动过期机制，支持基于时间和使用频率的清理
 * - 优先级队列管理，支持重要信息的优先保留
 * - 支持多种临时信息类型（文本、数字、列表等）
 * - 提供快速搜索和过滤功能
 * - 支持批量操作和事务性更新
 *
 * 清理策略：
 * - 时间过期：超过设定时间的临时信息自动清理
 * - 频率清理：长时间未使用的信息被清理
 * - 容量清理：超出容量限制时清理低优先级信息
 * - 手动清理：提供手动清理接口
 *
 * 使用示例：
 * <pre>
 * TempMemory memory = new TempMemory();
 * memory.setMaxEntries(100);
 * memory.setDefaultExpiryMinutes(30);
 * memory.setCleanupIntervalMinutes(5);
 *
 * // 存储临时信息
 * String tempId = memory.storeTempInfo("用户偏好", "喜欢科幻电影", 60); // 60分钟过期
 *
 * // 检索临时信息
 * Object info = memory.getTempInfo(tempId);
 *
 * // 搜索临时信息
 * List<String> results = memory.searchTempInfo("电影");
 *
 * // 自动清理过期信息
 * memory.cleanupExpiredEntries();
 *
 * // 获取临时信息统计
 * Map<String, Object> stats = memory.getTempStats();
 * </pre>
 *
 * @author xiaoxuan.lp
 */
@Data
@Slf4j
public class TempMemory extends BaseMemory {

    /**
     * 临时信息存储
     * key: 信息ID
     * value: 临时信息条目
     */
    private Map<String, TempEntry> tempEntries = new ConcurrentHashMap<>();

    /**
     * 类别索引
     * key: 类别
     * value: 该类别下的信息ID列表
     */
    private Map<String, Set<String>> categoryIndex = new ConcurrentHashMap<>();

    /**
     * 标签索引
     * key: 标签
     * value: 包含该标签的信息ID列表
     */
    private Map<String, Set<String>> tagIndex = new ConcurrentHashMap<>();

    /**
     * 优先级队列（按过期时间排序）
     */
    private PriorityQueue<TempEntry> expiryQueue = new PriorityQueue<>(
        Comparator.comparing(entry -> entry.getExpiryTime())
    );

    /**
     * 关键词识别模式
     */
    private List<String> tempKeywords = Arrays.asList(
        "临时", "暂存", "缓存", "记住", "记录", "存储", "保存",
        "temp", "temporary", "cache", "memo", "remember", "note"
    );

    /**
     * 最大条目数量
     */
    private int maxEntries = 200;

    /**
     * 默认过期时间（分钟）
     */
    private int defaultExpiryMinutes = 60;

    /**
     * 清理间隔（分钟）
     */
    private int cleanupIntervalMinutes = 10;

    /**
     * 最后清理时间
     */
    private LocalDateTime lastCleanupTime = LocalDateTime.now();

    /**
     * 临时条目类
     */
    @Data
    public static class TempEntry {
        private String id;
        private String category;           // 类别
        private String key;               // 键名
        private Object value;             // 值
        private Set<String> tags;         // 标签集合
        private int priority;             // 优先级（1-10，10最高）
        private LocalDateTime createdTime; // 创建时间
        private LocalDateTime expiryTime;  // 过期时间
        private LocalDateTime lastAccessTime; // 最后访问时间
        private int accessCount;          // 访问计数
        private Map<String, Object> metadata; // 元数据

        public TempEntry(String category, String key, Object value) {
            this.id = UUID.randomUUID().toString();
            this.category = category;
            this.key = key;
            this.value = value;
            this.priority = 5; // 默认中等优先级
            this.createdTime = LocalDateTime.now();
            this.lastAccessTime = LocalDateTime.now();
            this.accessCount = 0;
            this.tags = new HashSet<>();
            this.metadata = new HashMap<>();
        }

        /**
         * 检查是否已过期
         */
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }

        /**
         * 记录访问
         */
        public void recordAccess() {
            this.lastAccessTime = LocalDateTime.now();
            this.accessCount++;
        }

        /**
         * 更新过期时间
         */
        public void updateExpiry(int minutes) {
            this.expiryTime = LocalDateTime.now().plusMinutes(minutes);
        }

        /**
         * 计算活跃度得分
         */
        public double getActivityScore() {
            // 基于访问频率和最近访问时间计算活跃度
            long hoursSinceLastAccess = java.time.Duration.between(lastAccessTime, LocalDateTime.now()).toHours();
            double recencyScore = Math.max(0, 1.0 - (hoursSinceLastAccess / 24.0)); // 24小时内衰减
            double frequencyScore = Math.min(1.0, accessCount / 10.0); // 访问10次满分

            return (recencyScore * 0.6) + (frequencyScore * 0.4);
        }
    }

    @Override
    public List<String> memoryVariables() {
        return Arrays.asList("temp_info", "temp_summary");
    }

    @Override
    public Map<String, Object> loadMemoryVariables(String sessionId, Map<String, Object> inputs) {
        Map<String, Object> result = new HashMap<>();

        // 获取临时信息摘要
        String tempSummary = getTempSummary();
        result.put("temp_summary", tempSummary);

        // 检查是否需要提取临时信息
        String query = inputs.values().stream()
            .map(Object::toString)
            .collect(Collectors.joining(" "));

        if (containsTempKeywords(query)) {
            String tempInfo = extractAndStoreTempInfo(query);
            result.put("temp_info", tempInfo);
        } else {
            result.put("temp_info", "无临时信息提取需求");
        }

        return result;
    }

    @Override
    public void saveContext(String sessionId, Map<String, Object> inputs, Map<String, Object> outputs) {
        // 从对话中提取临时信息
        String inputText = inputs.values().stream()
            .map(Object::toString)
            .collect(Collectors.joining(" "));
        String outputText = outputs.values().stream()
            .map(Object::toString)
            .collect(Collectors.joining(" "));

        String combinedText = inputText + " " + outputText;

        if (containsTempKeywords(combinedText)) {
            extractAndStoreTempInfo(combinedText);
        }

        // 定期清理过期条目
        performPeriodicCleanup();
    }

    /**
     * 检查文本是否包含临时关键词
     */
    private boolean containsTempKeywords(String text) {
        String lowerText = text.toLowerCase();
        return tempKeywords.stream().anyMatch(lowerText::contains);
    }

    /**
     * 从文本中提取并存储临时信息
     */
    private String extractAndStoreTempInfo(String text) {
        StringBuilder result = new StringBuilder();

        // 提取模式：记住XX、记录YY、暂存ZZ等
        Pattern tempPattern = Pattern.compile("(?:记住|记录|暂存|存储|保存)\\s*[:：]?\\s*([^。！？\\n]{3,100})");
        Matcher matcher = tempPattern.matcher(text);

        while (matcher.find()) {
            String tempInfo = matcher.group(1).trim();
            if (tempInfo.length() > 2) {
                String tempId = storeTempInfo("对话提取", tempInfo, defaultExpiryMinutes);
                result.append("已存储临时信息：").append(tempInfo).append("\n");
            }
        }

        return result.length() > 0 ? result.toString() : "未提取到临时信息";
    }

    /**
     * 存储临时信息
     *
     * @param category 类别
     * @param key 键名
     * @param value 值
     * @param expiryMinutes 过期时间（分钟）
     * @return 临时信息ID
     */
    public String storeTempInfo(String category, String key, Object value, int expiryMinutes) {
        return storeTempInfo(category, key, value, expiryMinutes, 5); // 默认中等优先级
    }

    /**
     * 存储临时信息（完整参数）
     */
    public String storeTempInfo(String category, String key, Object value, int expiryMinutes, int priority) {
        TempEntry entry = new TempEntry(category, key, value);
        entry.setPriority(priority);
        entry.updateExpiry(expiryMinutes);

        // 添加标签（基于键名和值生成）
        Set<String> tags = generateTags(key, value);
        entry.getTags().addAll(tags);

        // 存储条目
        tempEntries.put(entry.getId(), entry);

        // 更新索引
        categoryIndex.computeIfAbsent(category, k -> ConcurrentHashMap.newKeySet()).add(entry.getId());
        for (String tag : tags) {
            tagIndex.computeIfAbsent(tag, k -> ConcurrentHashMap.newKeySet()).add(entry.getId());
        }

        // 更新过期队列
        expiryQueue.offer(entry);

        // 检查容量限制
        enforceCapacityLimit();

        log.debug("Stored temp entry: {} -> {}", key, value);
        return entry.getId();
    }

    /**
     * 简化版存储方法（使用默认过期时间）
     */
    public String storeTempInfo(String category, String key, Object value) {
        return storeTempInfo(category, key, value, defaultExpiryMinutes);
    }

    /**
     * 生成标签
     */
    private Set<String> generateTags(String key, Object value) {
        Set<String> tags = new HashSet<>();

        // 基于键名生成标签
        if (key != null) {
            String[] keyWords = key.toLowerCase().split("[\\s_,]");
            for (String word : keyWords) {
                if (word.length() > 2) {
                    tags.add(word);
                }
            }
        }

        // 基于值生成标签
        if (value != null) {
            String valueStr = value.toString().toLowerCase();
            String[] valueWords = valueStr.split("[\\s_,。！？]");
            for (String word : valueWords) {
                if (word.length() > 2 && word.length() < 20) {
                    tags.add(word);
                }
            }
        }

        return tags;
    }

    /**
     * 获取临时信息
     */
    public Object getTempInfo(String tempId) {
        TempEntry entry = tempEntries.get(tempId);
        if (entry == null) {
            return null;
        }

        // 检查是否过期
        if (entry.isExpired()) {
            removeTempInfo(tempId);
            return null;
        }

        // 记录访问
        entry.recordAccess();

        return entry.getValue();
    }

    /**
     * 删除临时信息
     */
    public boolean removeTempInfo(String tempId) {
        TempEntry entry = tempEntries.remove(tempId);
        if (entry == null) {
            return false;
        }

        // 从索引中移除
        Set<String> categoryEntries = categoryIndex.get(entry.getCategory());
        if (categoryEntries != null) {
            categoryEntries.remove(tempId);
            if (categoryEntries.isEmpty()) {
                categoryIndex.remove(entry.getCategory());
            }
        }

        for (String tag : entry.getTags()) {
            Set<String> tagEntries = tagIndex.get(tag);
            if (tagEntries != null) {
                tagEntries.remove(tempId);
                if (tagEntries.isEmpty()) {
                    tagIndex.remove(tag);
                }
            }
        }

        log.debug("Removed temp entry: {}", tempId);
        return true;
    }

    /**
     * 搜索临时信息
     */
    public List<String> searchTempInfo(String keyword) {
        return tempEntries.values().stream()
            .filter(entry -> !entry.isExpired())
            .filter(entry -> entry.getKey().toLowerCase().contains(keyword.toLowerCase()) ||
                           entry.getValue().toString().toLowerCase().contains(keyword.toLowerCase()) ||
                           entry.getTags().contains(keyword.toLowerCase()))
            .sorted((a, b) -> Integer.compare(b.getAccessCount(), a.getAccessCount())) // 按访问频率排序
            .map(entry -> String.format("%s: %s (类别: %s, 优先级: %d)",
                                      entry.getKey(), entry.getValue(),
                                      entry.getCategory(), entry.getPriority()))
            .collect(Collectors.toList());
    }

    /**
     * 根据类别获取临时信息
     */
    public List<TempEntry> getTempInfoByCategory(String category) {
        Set<String> entryIds = categoryIndex.get(category);
        if (entryIds == null) {
            return Collections.emptyList();
        }

        return entryIds.stream()
            .map(tempEntries::get)
            .filter(Objects::nonNull)
            .filter(entry -> !entry.isExpired())
            .sorted((a, b) -> b.getActivityScore() - a.getActivityScore() > 0 ? -1 : 1) // 按活跃度排序
            .collect(Collectors.toList());
    }

    /**
     * 执行定期清理
     */
    private void performPeriodicCleanup() {
        LocalDateTime now = LocalDateTime.now();

        // 检查是否到达清理间隔
        if (java.time.Duration.between(lastCleanupTime, now).toMinutes() < cleanupIntervalMinutes) {
            return;
        }

        cleanupExpiredEntries();
        lastCleanupTime = now;
    }

    /**
     * 清理过期条目
     */
    public void cleanupExpiredEntries() {
        List<String> expiredIds = tempEntries.values().stream()
            .filter(TempEntry::isExpired)
            .map(TempEntry::getId)
            .collect(Collectors.toList());

        for (String id : expiredIds) {
            removeTempInfo(id);
        }

        if (!expiredIds.isEmpty()) {
            log.debug("Cleaned up {} expired temp entries", expiredIds.size());
        }
    }

    /**
     * 强制容量限制
     */
    private void enforceCapacityLimit() {
        if (tempEntries.size() <= maxEntries) {
            return;
        }

        // 按活跃度排序，删除最低活跃度的条目
        List<TempEntry> sortedEntries = tempEntries.values().stream()
            .sorted(Comparator.comparing(TempEntry::getActivityScore))
            .collect(Collectors.toList());

        int entriesToRemove = tempEntries.size() - maxEntries + 10; // 多删除一些以留出缓冲

        for (int i = 0; i < entriesToRemove && i < sortedEntries.size(); i++) {
            removeTempInfo(sortedEntries.get(i).getId());
        }
    }

    /**
     * 获取临时信息摘要
     */
    private String getTempSummary() {
        cleanupExpiredEntries(); // 清理后再统计

        Map<String, Long> categoryCounts = categoryIndex.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .map(tempEntries::get)
                    .filter(Objects::nonNull)
                    .filter(e -> !e.isExpired())
                    .count()
            ));

        if (categoryCounts.isEmpty()) {
            return "暂无临时信息";
        }

        StringBuilder summary = new StringBuilder("临时信息概览：");
        for (Map.Entry<String, Long> entry : categoryCounts.entrySet()) {
            summary.append(String.format("\n- %s: %d 条", entry.getKey(), entry.getValue()));
        }

        return summary.toString();
    }

    /**
     * 获取临时信息统计
     */
    public Map<String, Object> getTempStats() {
        cleanupExpiredEntries();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_entries", tempEntries.size());
        stats.put("total_categories", categoryIndex.size());
        stats.put("total_tags", tagIndex.size());

        // 类别分布
        Map<String, Long> categoryDistribution = categoryIndex.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (long) entry.getValue().size()
            ));
        stats.put("category_distribution", categoryDistribution);

        // 优先级分布
        Map<Integer, Long> priorityDistribution = tempEntries.values().stream()
            .collect(Collectors.groupingBy(
                TempEntry::getPriority,
                Collectors.counting()
            ));
        stats.put("priority_distribution", priorityDistribution);

        // 平均活跃度
        double avgActivity = tempEntries.values().stream()
            .mapToDouble(TempEntry::getActivityScore)
            .average()
            .orElse(0.0);
        stats.put("average_activity_score", avgActivity);

        return stats;
    }

    /**
     * 批量更新过期时间
     */
    public void extendExpiryTime(String category, int additionalMinutes) {
        categoryIndex.getOrDefault(category, Collections.emptySet()).forEach(id -> {
            TempEntry entry = tempEntries.get(id);
            if (entry != null && !entry.isExpired()) {
                entry.updateExpiry(additionalMinutes);
            }
        });
    }

    /**
     * 清空所有临时信息
     */
    public void clearAllTempInfo() {
        tempEntries.clear();
        categoryIndex.clear();
        tagIndex.clear();
        expiryQueue.clear();
        log.info("Cleared all temporary entries");
    }

    @Override
    public void clear(String sessionId) {
        // 临时记忆通常不需要按会话清理，因为它是全局性的
        // 但可以提供按会话清理的功能
        if (sessionId != null) {
            // 这里可以实现按会话清理逻辑
            // 目前简化为清空所有
            clearAllTempInfo();
        } else {
            clearAllTempInfo();
        }
    }
}
