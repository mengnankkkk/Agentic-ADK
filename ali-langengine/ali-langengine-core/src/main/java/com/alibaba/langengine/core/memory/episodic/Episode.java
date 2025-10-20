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
package com.alibaba.langengine.core.memory.episodic;

import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 情节（Episode）
 *
 * 情节是一系列相关事件的集合，代表一段连贯的经历或体验
 *
 * 情节特征：
 * - 时间连续性：事件按时间顺序组织
 * - 空间一致性：通常发生在同一场景或相关场景
 * - 语义相关性：事件之间具有因果或主题关联
 * - 完整性：有明确的开始和结束
 *
 * 情节示例：
 * - 一次完整的客服对话
 * - 一个任务的执行过程
 * - 一次会议的全过程
 * - 一段学习经历
 *
 * @author xiaoxuan.lp
 */
@Data
public class Episode {

    /**
     * 情节唯一标识符
     */
    private String id;

    /**
     * 情节标题/主题
     */
    private String title;

    /**
     * 情节描述
     */
    private String description;

    /**
     * 情节类型
     */
    private EpisodeType episodeType;

    /**
     * 情节中的事件列表（按时间排序）
     */
    private List<Event> events;

    /**
     * 情节开始时间
     */
    private Long startTime;

    /**
     * 情节结束时间
     */
    private Long endTime;

    /**
     * 情节发生的主要地点
     */
    private String location;

    /**
     * 情节的主要参与者
     */
    private Set<String> participants;

    /**
     * 情节的整体情感
     */
    private String overallEmotion;

    /**
     * 情节重要性评分 (0-1)
     */
    private Double importance;

    /**
     * 情节标签
     */
    private Set<String> tags;

    /**
     * 情节摘要（自动生成或手动设置）
     */
    private String summary;

    /**
     * 情节元数据
     */
    private Map<String, Object> metadata;

    /**
     * 情节状态
     */
    private EpisodeStatus status;

    /**
     * 创建时间
     */
    private Long createdAt;

    /**
     * 更新时间
     */
    private Long updatedAt;

    /**
     * 情节类型枚举
     */
    public enum EpisodeType {
        /**
         * 对话会话
         */
        CONVERSATION,

        /**
         * 任务执行
         */
        TASK,

        /**
         * 学习过程
         */
        LEARNING,

        /**
         * 问题解决
         */
        PROBLEM_SOLVING,

        /**
         * 决策过程
         */
        DECISION_MAKING,

        /**
         * 探索发现
         */
        EXPLORATION,

        /**
         * 社交互动
         */
        SOCIAL_INTERACTION,

        /**
         * 其他
         */
        OTHER
    }

    /**
     * 情节状态枚举
     */
    public enum EpisodeStatus {
        /**
         * 进行中
         */
        ONGOING,

        /**
         * 已完成
         */
        COMPLETED,

        /**
         * 已暂停
         */
        PAUSED,

        /**
         * 已取消
         */
        CANCELLED
    }

    public Episode() {
        this.id = UUID.randomUUID().toString();
        this.events = new ArrayList<>();
        this.participants = new HashSet<>();
        this.tags = new HashSet<>();
        this.metadata = new HashMap<>();
        this.status = EpisodeStatus.ONGOING;
        this.importance = 0.5;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Episode(String title, EpisodeType episodeType) {
        this();
        this.title = title;
        this.episodeType = episodeType;
    }

    /**
     * Builder模式
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Episode episode;

        public Builder() {
            this.episode = new Episode();
        }

        public Builder id(String id) {
            episode.setId(id);
            return this;
        }

        public Builder title(String title) {
            episode.setTitle(title);
            return this;
        }

        public Builder description(String description) {
            episode.setDescription(description);
            return this;
        }

        public Builder episodeType(EpisodeType episodeType) {
            episode.setEpisodeType(episodeType);
            return this;
        }

        public Builder location(String location) {
            episode.setLocation(location);
            return this;
        }

        public Builder participants(Set<String> participants) {
            episode.setParticipants(new HashSet<>(participants));
            return this;
        }

        public Builder addParticipant(String participant) {
            episode.getParticipants().add(participant);
            return this;
        }

        public Builder overallEmotion(String emotion) {
            episode.setOverallEmotion(emotion);
            return this;
        }

        public Builder importance(Double importance) {
            episode.setImportance(importance);
            return this;
        }

        public Builder tags(Set<String> tags) {
            episode.setTags(new HashSet<>(tags));
            return this;
        }

        public Builder addTag(String tag) {
            episode.getTags().add(tag);
            return this;
        }

        public Builder summary(String summary) {
            episode.setSummary(summary);
            return this;
        }

        public Builder status(EpisodeStatus status) {
            episode.setStatus(status);
            return this;
        }

        public Episode build() {
            return episode;
        }
    }

    /**
     * 添加事件到情节
     */
    public void addEvent(Event event) {
        if (event == null) {
            return;
        }

        events.add(event);
        updateTimeBounds(event);
        updateParticipants(event);
        updateImportance(event);
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 批量添加事件
     */
    public void addEvents(List<Event> newEvents) {
        if (newEvents != null) {
            newEvents.forEach(this::addEvent);
        }
    }

    /**
     * 更新时间边界
     */
    private void updateTimeBounds(Event event) {
        if (event.getTimestamp() == null) {
            return;
        }

        if (startTime == null || event.getTimestamp() < startTime) {
            startTime = event.getTimestamp();
        }

        if (endTime == null || event.getTimestamp() > endTime) {
            endTime = event.getTimestamp();
        }
    }

    /**
     * 更新参与者列表
     */
    private void updateParticipants(Event event) {
        if (event.getParticipants() != null) {
            participants.addAll(event.getParticipants());
        }
    }

    /**
     * 更新重要性评分
     */
    private void updateImportance(Event event) {
        if (event.getImportance() != null && events.size() > 0) {
            // 使用移动平均计算整体重要性
            double currentImportance = this.importance != null ? this.importance : 0.5;
            this.importance = (currentImportance * (events.size() - 1) + event.getImportance()) / events.size();
        }
    }

    /**
     * 完成情节
     */
    public void complete() {
        this.status = EpisodeStatus.COMPLETED;
        this.endTime = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 暂停情节
     */
    public void pause() {
        this.status = EpisodeStatus.PAUSED;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 恢复情节
     */
    public void resume() {
        this.status = EpisodeStatus.ONGOING;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 取消情节
     */
    public void cancel() {
        this.status = EpisodeStatus.CANCELLED;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 获取情节持续时间（毫秒）
     */
    public Long getDuration() {
        if (startTime == null) {
            return 0L;
        }

        Long effectiveEndTime = endTime != null ? endTime : System.currentTimeMillis();
        return effectiveEndTime - startTime;
    }

    /**
     * 获取情节中的事件数量
     */
    public int getEventCount() {
        return events.size();
    }

    /**
     * 按时间范围获取事件
     */
    public List<Event> getEventsBetween(Long start, Long end) {
        return events.stream()
            .filter(event -> event.occurredBetween(start, end))
            .collect(Collectors.toList());
    }

    /**
     * 按类型获取事件
     */
    public List<Event> getEventsByType(Event.EventType eventType) {
        return events.stream()
            .filter(event -> event.getEventType() == eventType)
            .collect(Collectors.toList());
    }

    /**
     * 按参与者获取事件
     */
    public List<Event> getEventsByParticipant(String participant) {
        return events.stream()
            .filter(event -> event.hasParticipant(participant))
            .collect(Collectors.toList());
    }

    /**
     * 按标签获取事件
     */
    public List<Event> getEventsByTag(String tag) {
        return events.stream()
            .filter(event -> event.hasTag(tag))
            .collect(Collectors.toList());
    }

    /**
     * 获取最重要的N个事件
     */
    public List<Event> getTopEvents(int topN) {
        return events.stream()
            .sorted((e1, e2) -> {
                double imp1 = e1.getImportance() != null ? e1.getImportance() : 0.0;
                double imp2 = e2.getImportance() != null ? e2.getImportance() : 0.0;
                return Double.compare(imp2, imp1);
            })
            .limit(topN)
            .collect(Collectors.toList());
    }

    /**
     * 生成情节摘要
     */
    public String generateSummary() {
        if (events.isEmpty()) {
            return "空情节";
        }

        StringBuilder summary = new StringBuilder();

        // 标题和类型
        if (title != null) {
            summary.append(title);
        } else if (episodeType != null) {
            summary.append(episodeType.name());
        }
        summary.append("\n");

        // 时间信息
        if (startTime != null) {
            summary.append("开始: ").append(new Date(startTime)).append("\n");
        }
        if (endTime != null) {
            summary.append("结束: ").append(new Date(endTime)).append("\n");
        }

        // 参与者
        if (!participants.isEmpty()) {
            summary.append("参与者: ").append(String.join(", ", participants)).append("\n");
        }

        // 事件概览
        summary.append("事件数量: ").append(events.size()).append("\n");

        // 关键事件
        List<Event> topEvents = getTopEvents(3);
        if (!topEvents.isEmpty()) {
            summary.append("关键事件:\n");
            for (Event event : topEvents) {
                summary.append("  - ").append(event.getShortDescription()).append("\n");
            }
        }

        this.summary = summary.toString();
        return this.summary;
    }

    /**
     * 转换为自然语言描述
     */
    public String toNaturalLanguage() {
        StringBuilder narrative = new StringBuilder();

        if (title != null) {
            narrative.append(title).append("\n\n");
        }

        if (description != null) {
            narrative.append(description).append("\n\n");
        }

        // 按时间顺序叙述事件
        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            narrative.append(i + 1).append(". ");
            narrative.append(event.getContent());

            if (event.getParticipants() != null && !event.getParticipants().isEmpty()) {
                narrative.append(" (").append(String.join(", ", event.getParticipants())).append(")");
            }

            narrative.append("\n");
        }

        return narrative.toString();
    }

    /**
     * 计算与另一个情节的相似度
     */
    public double similarityTo(Episode other) {
        if (other == null) {
            return 0.0;
        }

        double score = 0.0;
        int factors = 0;

        // 类型相似度
        if (this.episodeType != null && other.episodeType != null) {
            score += (this.episodeType == other.episodeType) ? 1.0 : 0.0;
            factors++;
        }

        // 地点相似度
        if (this.location != null && other.location != null) {
            score += this.location.equals(other.location) ? 1.0 : 0.0;
            factors++;
        }

        // 参与者相似度
        if (!this.participants.isEmpty() && !other.participants.isEmpty()) {
            Set<String> intersection = new HashSet<>(this.participants);
            intersection.retainAll(other.participants);
            Set<String> union = new HashSet<>(this.participants);
            union.addAll(other.participants);
            score += (double) intersection.size() / union.size();
            factors++;
        }

        // 标签相似度
        if (!this.tags.isEmpty() && !other.tags.isEmpty()) {
            Set<String> intersection = new HashSet<>(this.tags);
            intersection.retainAll(other.tags);
            Set<String> union = new HashSet<>(this.tags);
            union.addAll(other.tags);
            score += (double) intersection.size() / union.size();
            factors++;
        }

        // 事件相似度（简化版，比较平均事件相似度）
        if (!this.events.isEmpty() && !other.events.isEmpty()) {
            double eventSimilarity = 0.0;
            int comparisons = 0;

            for (Event e1 : this.events) {
                for (Event e2 : other.events) {
                    eventSimilarity += e1.similarityTo(e2);
                    comparisons++;
                }
            }

            if (comparisons > 0) {
                score += eventSimilarity / comparisons;
                factors++;
            }
        }

        return factors > 0 ? score / factors : 0.0;
    }

    @Override
    public String toString() {
        return String.format("Episode[%s, events=%d, status=%s]",
            title != null ? title : id,
            events.size(),
            status);
    }
}
