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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 话题跟踪记忆系统（Topic Tracking Memory）
 *
 * 功能特性：
 * - 自动提取和跟踪用户感兴趣的话题和关键词
 * - 记录话题出现频率和时间戳
 * - 支持话题热度计算和衰减机制
 * - 提供话题相关性推荐
 * - 支持话题聚类和分类
 *
 * 使用示例：
 * <pre>
 * TopicTrackingMemory memory = new TopicTrackingMemory();
 * memory.setTopicKeywords(Arrays.asList("AI", "机器学习", "编程", "旅行"));
 * memory.setInterestThreshold(0.3);
 *
 * // 保存对话上下文，自动提取话题
 * memory.saveContext(inputs, outputs);
 *
 * // 获取用户兴趣话题
 * List<String> interests = memory.getTopInterests(5);
 *
 * // 搜索相关话题
 * List<String> relatedTopics = memory.getRelatedTopics("深度学习");
 * </pre>
 *
 * @author xiaoxuan.lp
 */
@Data
@Slf4j
public class TopicTrackingMemory extends BaseMemory {

    /**
     * 用户兴趣话题集合
     * key: 话题关键词
     * value: 话题信息（频率、最近出现时间、热度等）
     */
    private Map<String, TopicInfo> userInterests = new HashMap<>();

    /**
     * 会话话题历史
     * key: sessionId
     * value: 该会话中出现的话题列表
     */
    private Map<String, Set<String>> sessionTopics = new HashMap<>();

    /**
     * 预定义的话题关键词库
     * 用于辅助话题识别
     */
    private List<String> topicKeywords = Arrays.asList(
        "人工智能", "机器学习", "深度学习", "编程", "Java", "Python", "旅行", "美食",
        "电影", "音乐", "读书", "健身", "科技", "新闻", "游戏", "摄影", "设计"
    );

    /**
     * 兴趣度阈值
     * 高于此值的兴趣会被认为是主要兴趣
     */
    private double interestThreshold = 0.3;

    /**
     * 话题热度衰减因子（每天）
     * 每过一天，热度衰减的比例
     */
    private static final double DECAY_FACTOR = 0.95;

    /**
     * 最大跟踪话题数量
     */
    private int maxTrackedTopics = 50;

    /**
     * 话题信息类
     */
    @Data
    public static class TopicInfo {
        private String topic;
        private int frequency = 0;        // 出现频率
        private long firstSeen;           // 首次出现时间
        private long lastSeen;            // 最后出现时间
        private double score = 0.0;       // 综合得分
        private String category;          // 话题分类
        private Set<String> relatedTerms = new HashSet<>(); // 相关术语

        public TopicInfo(String topic) {
            this.topic = topic;
            this.firstSeen = System.currentTimeMillis();
            this.lastSeen = System.currentTimeMillis();
        }

        /**
         * 更新话题信息
         */
        public void update(String category) {
            this.frequency++;
            this.lastSeen = System.currentTimeMillis();
            this.category = category;
            calculateScore();
        }

        /**
         * 计算综合得分
         * 基于频率、时间衰减等因素
         */
        private void calculateScore() {
            long daysSinceFirstSeen = (System.currentTimeMillis() - firstSeen) / (24 * 60 * 60 * 1000L);
            long daysSinceLastSeen = (System.currentTimeMillis() - lastSeen) / (24 * 60 * 60 * 1000L);

            // 基础得分：频率贡献
            double baseScore = Math.log(frequency + 1) / Math.log(10);

            // 时间衰减：最近出现的话题得分更高
            double timeDecay = Math.pow(DECAY_FACTOR, daysSinceLastSeen);

            // 持续性奖励：长期反复出现的话题得分更高
            double persistenceBonus = Math.pow(DECAY_FACTOR, daysSinceFirstSeen) * 0.5;

            this.score = baseScore * timeDecay + persistenceBonus;
        }
    }

    @Override
    public List<String> memoryVariables() {
        return Arrays.asList("topics", "interests");
    }

    @Override
    public Map<String, Object> loadMemoryVariables(String sessionId, Map<String, Object> inputs) {
        Map<String, Object> result = new HashMap<>();

        // 获取用户兴趣话题摘要
        String interestsSummary = getInterestsSummary(sessionId);
        result.put("interests", interestsSummary);

        // 获取话题追踪信息
        String topicsSummary = getTopicsSummary(sessionId);
        result.put("topics", topicsSummary);

        return result;
    }

    @Override
    public void saveContext(String sessionId, Map<String, Object> inputs, Map<String, Object> outputs) {
        // 从输入和输出中提取话题
        List<String> topics = extractTopics(inputs, outputs);

        if (topics.isEmpty()) {
            return;
        }

        // 初始化会话话题集合
        sessionTopics.computeIfAbsent(sessionId, k -> new HashSet<>());

        // 更新话题信息
        for (String topic : topics) {
            updateTopicInfo(topic, sessionId);
        }

        // 清理过时的话题
        cleanupOldTopics();

        log.debug("Updated topics for session {}: {}", sessionId, topics);
    }

    /**
     * 提取话题关键词
     */
    private List<String> extractTopics(Map<String, Object> inputs, Map<String, Object> outputs) {
        List<String> topics = new ArrayList<>();

        // 从输入和输出文本中提取关键词
        String inputText = inputs.values().stream()
            .map(Object::toString)
            .collect(Collectors.joining(" "));
        String outputText = outputs.values().stream()
            .map(Object::toString)
            .collect(Collectors.joining(" "));

        String combinedText = (inputText + " " + outputText).toLowerCase();

        // 提取预定义关键词
        for (String keyword : topicKeywords) {
            if (combinedText.contains(keyword.toLowerCase())) {
                topics.add(keyword);
            }
        }

        // 使用正则表达式提取可能的关键词（名词短语）
        Pattern pattern = Pattern.compile("\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*\\b");
        Matcher matcher = pattern.matcher(combinedText);

        while (matcher.find()) {
            String potentialTopic = matcher.group().trim();
            if (potentialTopic.length() > 2 && potentialTopic.length() < 30) {
                topics.add(potentialTopic);
            }
        }

        return topics.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 更新话题信息
     */
    private void updateTopicInfo(String topic, String sessionId) {
        TopicInfo info = userInterests.computeIfAbsent(topic, TopicInfo::new);
        String category = categorizeTopic(topic);
        info.update(category);

        // 添加到会话话题记录
        sessionTopics.get(sessionId).add(topic);

        // 更新相关术语
        updateRelatedTerms(topic, info);
    }

    /**
     * 话题分类
     */
    private String categorizeTopic(String topic) {
        String lowerTopic = topic.toLowerCase();

        if (lowerTopic.contains("学习") || lowerTopic.contains("编程") || lowerTopic.contains("技术")) {
            return "技术";
        } else if (lowerTopic.contains("旅行") || lowerTopic.contains("美食") || lowerTopic.contains("娱乐")) {
            return "生活";
        } else if (lowerTopic.contains("新闻") || lowerTopic.contains("政治") || lowerTopic.contains("经济")) {
            return "时事";
        } else if (lowerTopic.contains("电影") || lowerTopic.contains("音乐") || lowerTopic.contains("读书")) {
            return "文化";
        } else {
            return "其他";
        }
    }

    /**
     * 更新相关术语
     */
    private void updateRelatedTerms(String topic, TopicInfo info) {
        // 这里可以实现简单的相关性算法
        // 例如：如果话题是"机器学习"，相关术语可以包括"AI"、"神经网络"等
        String lowerTopic = topic.toLowerCase();

        if (lowerTopic.contains("机器学习")) {
            info.getRelatedTerms().addAll(Arrays.asList("人工智能", "深度学习", "神经网络", "算法"));
        } else if (lowerTopic.contains("编程")) {
            info.getRelatedTerms().addAll(Arrays.asList("代码", "开发", "软件", "调试"));
        } else if (lowerTopic.contains("旅行")) {
            info.getRelatedTerms().addAll(Arrays.asList("旅游", "景点", "酒店", "机票"));
        }
    }

    /**
     * 清理过时的话题
     */
    private void cleanupOldTopics() {
        if (userInterests.size() <= maxTrackedTopics) {
            return;
        }

        // 按得分排序，保留得分最高的话题
        List<TopicInfo> sortedTopics = userInterests.values().stream()
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .collect(Collectors.toList());

        // 清理低分话题
        userInterests.clear();
        for (int i = 0; i < Math.min(maxTrackedTopics, sortedTopics.size()); i++) {
            TopicInfo topic = sortedTopics.get(i);
            userInterests.put(topic.getTopic(), topic);
        }
    }

    /**
     * 获取兴趣话题摘要
     */
    private String getInterestsSummary(String sessionId) {
        List<String> topInterests = getTopInterests(5);
        return topInterests.isEmpty() ? "暂无明显兴趣话题" :
               "主要兴趣：" + String.join(", ", topInterests);
    }

    /**
     * 获取话题摘要
     */
    private String getTopicsSummary(String sessionId) {
        Set<String> topics = sessionTopics.get(sessionId);
        if (topics == null || topics.isEmpty()) {
            return "暂无话题记录";
        }

        return "最近话题：" + String.join(", ", topics);
    }

    /**
     * 获取热门兴趣话题
     */
    public List<String> getTopInterests(int limit) {
        return userInterests.values().stream()
            .filter(info -> info.getScore() >= interestThreshold)
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .limit(limit)
            .map(TopicInfo::getTopic)
            .collect(Collectors.toList());
    }

    /**
     * 获取相关话题推荐
     */
    public List<String> getRelatedTopics(String topic) {
        TopicInfo info = userInterests.get(topic);
        if (info == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(info.getRelatedTerms());
    }

    /**
     * 获取话题统计信息
     */
    public Map<String, Object> getTopicStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_topics", userInterests.size());
        stats.put("total_sessions", sessionTopics.size());

        long activeTopics = userInterests.values().stream()
            .mapToLong(info -> info.getFrequency())
            .sum();
        stats.put("total_interactions", activeTopics);

        return stats;
    }

    /**
     * 搜索包含特定关键词的话题
     */
    public List<String> searchTopics(String keyword) {
        return userInterests.keySet().stream()
            .filter(topic -> topic.toLowerCase().contains(keyword.toLowerCase()))
            .collect(Collectors.toList());
    }

    @Override
    public void clear(String sessionId) {
        if (sessionId != null) {
            sessionTopics.remove(sessionId);
        } else {
            userInterests.clear();
            sessionTopics.clear();
        }
    }
}
