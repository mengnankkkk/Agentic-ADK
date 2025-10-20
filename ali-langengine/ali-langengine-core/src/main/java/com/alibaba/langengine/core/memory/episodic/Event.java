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

/**
 * 事件（Event）
 *
 * 表示情节记忆中的单个事件，包含"何时、何地、发生了什么"的信息
 *
 * 事件是构成情节的基本单位，包含：
 * - 时间信息（何时）
 * - 空间信息（何地）
 * - 内容信息（什么事）
 * - 参与者信息（谁）
 * - 情感信息（感觉如何）
 *
 * 示例：
 * <pre>
 * Event event = Event.builder()
 *     .content("用户询问了产品价格")
 *     .eventType(EventType.QUESTION)
 *     .location("客服对话")
 *     .participants(Arrays.asList("用户", "客服AI"))
 *     .emotion("好奇")
 *     .importance(0.7)
 *     .build();
 * </pre>
 *
 * @author xiaoxuan.lp
 */
@Data
public class Event {

    /**
     * 事件唯一标识符
     */
    private String id;

    /**
     * 事件内容（发生了什么）
     */
    private String content;

    /**
     * 事件类型
     */
    private EventType eventType;

    /**
     * 事件发生时间
     */
    private Long timestamp;

    /**
     * 事件发生地点/场景
     */
    private String location;

    /**
     * 参与者列表
     */
    private List<String> participants;

    /**
     * 事件相关的情感/情绪
     */
    private String emotion;

    /**
     * 事件重要性评分 (0-1)
     */
    private Double importance;

    /**
     * 事件标签
     */
    private Set<String> tags;

    /**
     * 事件元数据
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    private Long createdAt;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /**
         * 对话事件
         */
        CONVERSATION,

        /**
         * 问题事件
         */
        QUESTION,

        /**
         * 回答事件
         */
        ANSWER,

        /**
         * 行动事件
         */
        ACTION,

        /**
         * 决策事件
         */
        DECISION,

        /**
         * 观察事件
         */
        OBSERVATION,

        /**
         * 反思事件
         */
        REFLECTION,

        /**
         * 错误事件
         */
        ERROR,

        /**
         * 成功事件
         */
        SUCCESS,

        /**
         * 其他事件
         */
        OTHER
    }

    public Event() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
        this.participants = new ArrayList<>();
        this.tags = new HashSet<>();
        this.metadata = new HashMap<>();
        this.importance = 0.5;
    }

    public Event(String content, EventType eventType) {
        this();
        this.content = content;
        this.eventType = eventType;
    }

    public Event(String content, EventType eventType, String location) {
        this(content, eventType);
        this.location = location;
    }

    /**
     * Builder模式
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Event event;

        public Builder() {
            this.event = new Event();
        }

        public Builder id(String id) {
            event.setId(id);
            return this;
        }

        public Builder content(String content) {
            event.setContent(content);
            return this;
        }

        public Builder eventType(EventType eventType) {
            event.setEventType(eventType);
            return this;
        }

        public Builder timestamp(Long timestamp) {
            event.setTimestamp(timestamp);
            return this;
        }

        public Builder location(String location) {
            event.setLocation(location);
            return this;
        }

        public Builder participants(List<String> participants) {
            event.setParticipants(new ArrayList<>(participants));
            return this;
        }

        public Builder addParticipant(String participant) {
            event.getParticipants().add(participant);
            return this;
        }

        public Builder emotion(String emotion) {
            event.setEmotion(emotion);
            return this;
        }

        public Builder importance(Double importance) {
            event.setImportance(importance);
            return this;
        }

        public Builder tags(Set<String> tags) {
            event.setTags(new HashSet<>(tags));
            return this;
        }

        public Builder addTag(String tag) {
            event.getTags().add(tag);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            event.setMetadata(new HashMap<>(metadata));
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            event.getMetadata().put(key, value);
            return this;
        }

        public Event build() {
            return event;
        }
    }

    /**
     * 获取事件的简短描述
     */
    public String getShortDescription() {
        StringBuilder desc = new StringBuilder();

        if (eventType != null) {
            desc.append("[").append(eventType.name()).append("] ");
        }

        if (content != null) {
            String shortContent = content.length() > 50 ?
                content.substring(0, 47) + "..." : content;
            desc.append(shortContent);
        }

        return desc.toString();
    }

    /**
     * 获取事件的详细描述
     */
    public String getDetailedDescription() {
        StringBuilder desc = new StringBuilder();

        desc.append("事件: ").append(content != null ? content : "无描述").append("\n");
        desc.append("类型: ").append(eventType != null ? eventType.name() : "未知").append("\n");

        if (timestamp != null) {
            desc.append("时间: ").append(new Date(timestamp)).append("\n");
        }

        if (location != null && !location.isEmpty()) {
            desc.append("地点: ").append(location).append("\n");
        }

        if (participants != null && !participants.isEmpty()) {
            desc.append("参与者: ").append(String.join(", ", participants)).append("\n");
        }

        if (emotion != null && !emotion.isEmpty()) {
            desc.append("情感: ").append(emotion).append("\n");
        }

        if (importance != null) {
            desc.append("重要性: ").append(String.format("%.2f", importance)).append("\n");
        }

        if (tags != null && !tags.isEmpty()) {
            desc.append("标签: ").append(String.join(", ", tags)).append("\n");
        }

        return desc.toString();
    }

    /**
     * 计算与另一个事件的相似度
     */
    public double similarityTo(Event other) {
        if (other == null) {
            return 0.0;
        }

        double score = 0.0;
        int factors = 0;

        // 类型相似度
        if (this.eventType != null && other.eventType != null) {
            score += (this.eventType == other.eventType) ? 1.0 : 0.0;
            factors++;
        }

        // 地点相似度
        if (this.location != null && other.location != null) {
            score += this.location.equals(other.location) ? 1.0 : 0.0;
            factors++;
        }

        // 参与者相似度
        if (this.participants != null && other.participants != null &&
            !this.participants.isEmpty() && !other.participants.isEmpty()) {
            Set<String> intersection = new HashSet<>(this.participants);
            intersection.retainAll(other.participants);
            Set<String> union = new HashSet<>(this.participants);
            union.addAll(other.participants);
            score += (double) intersection.size() / union.size();
            factors++;
        }

        // 标签相似度
        if (this.tags != null && other.tags != null &&
            !this.tags.isEmpty() && !other.tags.isEmpty()) {
            Set<String> intersection = new HashSet<>(this.tags);
            intersection.retainAll(other.tags);
            Set<String> union = new HashSet<>(this.tags);
            union.addAll(other.tags);
            score += (double) intersection.size() / union.size();
            factors++;
        }

        return factors > 0 ? score / factors : 0.0;
    }

    /**
     * 判断事件是否发生在指定时间范围内
     */
    public boolean occurredBetween(Long startTime, Long endTime) {
        if (timestamp == null) {
            return false;
        }

        if (startTime != null && timestamp < startTime) {
            return false;
        }

        if (endTime != null && timestamp > endTime) {
            return false;
        }

        return true;
    }

    /**
     * 判断事件是否包含指定参与者
     */
    public boolean hasParticipant(String participant) {
        return participants != null && participants.contains(participant);
    }

    /**
     * 判断事件是否包含指定标签
     */
    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }

    @Override
    public String toString() {
        return getShortDescription();
    }
}
