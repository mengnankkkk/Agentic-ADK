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
package com.alibaba.langengine.core.memory.impl;

import com.alibaba.langengine.core.chatmodel.BaseChatModel;
import com.alibaba.langengine.core.memory.BaseMemory;
import com.alibaba.langengine.core.memory.episodic.*;
import com.alibaba.langengine.core.messages.BaseMessage;
import com.alibaba.langengine.core.messages.HumanMessage;
import com.alibaba.langengine.core.messages.AIMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 情节记忆（Episodic Memory）
 *
 * 按时间和事件组织的记忆系统，模拟人类的情节记忆
 *
 * 核心特性：
 * - 时间组织：按时间序列组织事件和情节
 * - 事件聚合：自动将相关事件聚合为情节
 * - 情景回忆：基于时间、地点、参与者等维度检索
 * - 重要性过滤：优先保留和检索重要情节
 *
 * 使用场景：
 * - 长期对话追踪：记录完整的对话历史和上下文
 * - 任务执行记录：跟踪任务的完整执行过程
 * - 用户行为分析：分析用户的行为模式和偏好
 * - 经验学习：从历史经历中学习和改进
 *
 * 示例：
 * <pre>
 * EpisodicMemory memory = EpisodicMemory.builder()
 *     .llm(chatModel)
 *     .autoCreateEpisodes(true)
 *     .episodeTimeout(30 * 60 * 1000L)  // 30分钟
 *     .build();
 *
 * // 保存对话
 * memory.saveContext(sessionId, inputs, outputs);
 *
 * // 回忆今天的情节
 * List<Episode> todayEpisodes = memory.getTodayEpisodes(sessionId);
 * </pre>
 *
 * @author xiaoxuan.lp
 */
@Slf4j
@Data
public class EpisodicMemory extends BaseMemory {

    /**
     * 情节存储
     */
    private EpisodeStore episodeStore;

    /**
     * 语言模型（用于生成摘要等）
     */
    private BaseChatModel llm;

    /**
     * 是否自动创建情节
     */
    private boolean autoCreateEpisodes = true;

    /**
     * 情节超时时间（毫秒），超过此时间没有新事件则自动完成情节
     */
    private long episodeTimeout = 30 * 60 * 1000L;  // 默认30分钟

    /**
     * 检索时返回的最大情节数
     */
    private int maxEpisodesInContext = 5;

    /**
     * 检索时返回的最大事件数
     */
    private int maxEventsInContext = 20;

    /**
     * 重要性阈值，只有重要性高于此值的情节才会被包含在上下文中
     */
    private double importanceThreshold = 0.3;

    /**
     * 当前活跃的情节（sessionId -> Episode）
     */
    private Map<String, Episode> activeEpisodes = new HashMap<>();

    /**
     * 情节记忆的内存键
     */
    private String episodicMemoryKey = "episodic_memory";

    public EpisodicMemory() {
        this.episodeStore = new InMemoryEpisodeStore();
    }

    public EpisodicMemory(EpisodeStore episodeStore) {
        this.episodeStore = episodeStore;
    }

    /**
     * Builder模式
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EpisodeStore episodeStore;
        private BaseChatModel llm;
        private boolean autoCreateEpisodes = true;
        private long episodeTimeout = 30 * 60 * 1000L;
        private int maxEpisodesInContext = 5;
        private int maxEventsInContext = 20;
        private double importanceThreshold = 0.3;

        public Builder episodeStore(EpisodeStore episodeStore) {
            this.episodeStore = episodeStore;
            return this;
        }

        public Builder llm(BaseChatModel llm) {
            this.llm = llm;
            return this;
        }

        public Builder autoCreateEpisodes(boolean autoCreateEpisodes) {
            this.autoCreateEpisodes = autoCreateEpisodes;
            return this;
        }

        public Builder episodeTimeout(long episodeTimeout) {
            this.episodeTimeout = episodeTimeout;
            return this;
        }

        public Builder maxEpisodesInContext(int maxEpisodesInContext) {
            this.maxEpisodesInContext = maxEpisodesInContext;
            return this;
        }

        public Builder maxEventsInContext(int maxEventsInContext) {
            this.maxEventsInContext = maxEventsInContext;
            return this;
        }

        public Builder importanceThreshold(double importanceThreshold) {
            this.importanceThreshold = importanceThreshold;
            return this;
        }

        public EpisodicMemory build() {
            EpisodicMemory memory = new EpisodicMemory();

            if (episodeStore != null) {
                memory.setEpisodeStore(episodeStore);
            } else {
                memory.setEpisodeStore(new InMemoryEpisodeStore());
            }

            memory.setLlm(llm);
            memory.setAutoCreateEpisodes(autoCreateEpisodes);
            memory.setEpisodeTimeout(episodeTimeout);
            memory.setMaxEpisodesInContext(maxEpisodesInContext);
            memory.setMaxEventsInContext(maxEventsInContext);
            memory.setImportanceThreshold(importanceThreshold);

            return memory;
        }
    }

    @Override
    public List<String> memoryVariables() {
        return Arrays.asList(episodicMemoryKey);
    }

    @Override
    public Map<String, Object> loadMemoryVariables(String sessionId, Map<String, Object> inputs) {
        Map<String, Object> result = new HashMap<>();

        // 加载相关情节
        String context = loadEpisodicContext(sessionId, inputs);
        result.put(episodicMemoryKey, context);

        return result;
    }

    /**
     * 加载情节记忆上下文
     */
    private String loadEpisodicContext(String sessionId, Map<String, Object> inputs) {
        StringBuilder context = new StringBuilder();

        // 1. 获取最近的重要情节
        List<Episode> recentEpisodes = getRecentImportantEpisodes(sessionId, maxEpisodesInContext);

        if (recentEpisodes.isEmpty()) {
            return "暂无相关历史记忆";
        }

        context.append("=== 相关历史情节 ===\n\n");

        for (int i = 0; i < recentEpisodes.size(); i++) {
            Episode episode = recentEpisodes.get(i);
            context.append(String.format("[情节 %d] %s\n", i + 1, episode.getTitle() != null ? episode.getTitle() : "未命名情节"));

            // 添加情节摘要
            if (episode.getSummary() != null) {
                context.append(episode.getSummary()).append("\n");
            } else {
                // 如果没有摘要，显示关键事件
                List<Event> topEvents = episode.getTopEvents(3);
                if (!topEvents.isEmpty()) {
                    context.append("关键事件：\n");
                    for (Event event : topEvents) {
                        context.append("  - ").append(event.getShortDescription()).append("\n");
                    }
                }
            }

            context.append("\n");
        }

        return context.toString();
    }

    /**
     * 获取最近的重要情节
     */
    private List<Episode> getRecentImportantEpisodes(String sessionId, int limit) {
        List<Episode> sessionEpisodes = episodeStore.getEpisodesBySession(sessionId);

        return sessionEpisodes.stream()
            .filter(ep -> ep.getImportance() != null && ep.getImportance() >= importanceThreshold)
            .sorted((e1, e2) -> {
                Long t1 = e1.getStartTime() != null ? e1.getStartTime() : e1.getCreatedAt();
                Long t2 = e2.getStartTime() != null ? e2.getStartTime() : e2.getCreatedAt();
                return Long.compare(t2 != null ? t2 : 0, t1 != null ? t1 : 0);
            })
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public void saveContext(String sessionId, Map<String, Object> inputs, Map<String, Object> outputs) {
        // 创建事件
        List<Event> events = createEventsFromContext(inputs, outputs);

        if (events.isEmpty()) {
            return;
        }

        // 获取或创建当前情节
        Episode episode = getOrCreateActiveEpisode(sessionId);

        // 添加事件到情节
        for (Event event : events) {
            episode.addEvent(event);
        }

        // 检查是否需要完成情节
        if (shouldCompleteEpisode(episode)) {
            completeEpisode(sessionId, episode);
        } else {
            // 更新活跃情节
            activeEpisodes.put(sessionId, episode);
            episodeStore.updateEpisode(episode);
        }
    }

    /**
     * 从上下文创建事件
     */
    private List<Event> createEventsFromContext(Map<String, Object> inputs, Map<String, Object> outputs) {
        List<Event> events = new ArrayList<>();

        // 从inputs创建事件
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof String) {
                Event event = Event.builder()
                    .content((String) value)
                    .eventType(Event.EventType.QUESTION)
                    .addParticipant("Human")
                    .importance(0.5)
                    .build();
                events.add(event);
            } else if (value instanceof BaseMessage) {
                events.add(createEventFromMessage((BaseMessage) value));
            } else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof BaseMessage) {
                        events.add(createEventFromMessage((BaseMessage) item));
                    }
                }
            }
        }

        // 从outputs创建事件
        for (Map.Entry<String, Object> entry : outputs.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof String) {
                Event event = Event.builder()
                    .content((String) value)
                    .eventType(Event.EventType.ANSWER)
                    .addParticipant("AI")
                    .importance(0.5)
                    .build();
                events.add(event);
            } else if (value instanceof BaseMessage) {
                events.add(createEventFromMessage((BaseMessage) value));
            } else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof BaseMessage) {
                        events.add(createEventFromMessage((BaseMessage) item));
                    }
                }
            }
        }

        return events;
    }

    /**
     * 从消息创建事件
     */
    private Event createEventFromMessage(BaseMessage message) {
        Event.EventType eventType;
        String participant;

        if (message instanceof HumanMessage) {
            eventType = Event.EventType.QUESTION;
            participant = "Human";
        } else if (message instanceof AIMessage) {
            eventType = Event.EventType.ANSWER;
            participant = "AI";
        } else {
            eventType = Event.EventType.CONVERSATION;
            participant = "System";
        }

        return Event.builder()
            .content(message.getContent())
            .eventType(eventType)
            .addParticipant(participant)
            .importance(0.5)
            .build();
    }

    /**
     * 获取或创建活跃情节
     */
    private Episode getOrCreateActiveEpisode(String sessionId) {
        Episode episode = activeEpisodes.get(sessionId);

        if (episode != null) {
            // 检查是否超时
            long timeSinceLastEvent = System.currentTimeMillis() - episode.getUpdatedAt();
            if (timeSinceLastEvent > episodeTimeout) {
                // 超时，完成旧情节并创建新情节
                completeEpisode(sessionId, episode);
                episode = null;
            }
        }

        if (episode == null && autoCreateEpisodes) {
            episode = Episode.builder()
                .title("对话情节-" + new Date())
                .episodeType(Episode.EpisodeType.CONVERSATION)
                .build();

            episode.getMetadata().put("session_id", sessionId);
            activeEpisodes.put(sessionId, episode);
            episodeStore.addEpisode(episode);
        }

        return episode;
    }

    /**
     * 判断是否应该完成情节
     */
    private boolean shouldCompleteEpisode(Episode episode) {
        if (episode == null) {
            return false;
        }

        // 检查事件数量
        if (episode.getEventCount() >= 100) {
            return true;
        }

        // 检查持续时间（超过1小时）
        if (episode.getDuration() > 60 * 60 * 1000L) {
            return true;
        }

        return false;
    }

    /**
     * 完成情节
     */
    private void completeEpisode(String sessionId, Episode episode) {
        if (episode == null) {
            return;
        }

        episode.complete();

        // 生成摘要（如果有LLM）
        if (llm != null && episode.getSummary() == null) {
            try {
                String summary = generateEpisodeSummary(episode);
                episode.setSummary(summary);
            } catch (Exception e) {
                log.warn("Failed to generate episode summary", e);
                episode.generateSummary();  // 使用默认摘要生成
            }
        } else if (episode.getSummary() == null) {
            episode.generateSummary();
        }

        episodeStore.updateEpisode(episode);
        activeEpisodes.remove(sessionId);

        log.debug("Episode completed: {} with {} events",
            episode.getTitle(), episode.getEventCount());
    }

    /**
     * 使用LLM生成情节摘要
     */
    private String generateEpisodeSummary(Episode episode) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下情节生成简洁的摘要（2-3句话）：\n\n");
        prompt.append("标题：").append(episode.getTitle()).append("\n");
        prompt.append("事件数量：").append(episode.getEventCount()).append("\n\n");
        prompt.append("主要事件：\n");

        List<Event> topEvents = episode.getTopEvents(5);
        for (int i = 0; i < topEvents.size(); i++) {
            prompt.append(i + 1).append(". ").append(topEvents.get(i).getContent()).append("\n");
        }

        return llm.predict(prompt.toString());
    }

    @Override
    public void clear(String sessionId) {
        if (sessionId == null) {
            // 清空所有
            episodeStore.clear();
            activeEpisodes.clear();
        } else {
            // 清空指定会话
            Episode active = activeEpisodes.remove(sessionId);
            if (active != null) {
                completeEpisode(sessionId, active);
            }
            episodeStore.clearSession(sessionId);
        }
    }

    // ==================== 便捷方法 ====================

    /**
     * 手动开始新情节
     */
    public Episode startEpisode(String sessionId, String title, Episode.EpisodeType type) {
        // 完成当前情节
        Episode current = activeEpisodes.get(sessionId);
        if (current != null) {
            completeEpisode(sessionId, current);
        }

        // 创建新情节
        Episode episode = Episode.builder()
            .title(title)
            .episodeType(type)
            .build();

        episode.getMetadata().put("session_id", sessionId);
        activeEpisodes.put(sessionId, episode);
        episodeStore.addEpisode(episode);

        return episode;
    }

    /**
     * 手动完成当前情节
     */
    public void finishCurrentEpisode(String sessionId) {
        Episode episode = activeEpisodes.get(sessionId);
        if (episode != null) {
            completeEpisode(sessionId, episode);
        }
    }

    /**
     * 添加单个事件
     */
    public void addEvent(String sessionId, Event event) {
        Episode episode = getOrCreateActiveEpisode(sessionId);
        if (episode != null) {
            episode.addEvent(event);
            episodeStore.updateEpisode(episode);
        }
    }

    /**
     * 获取今天的情节
     */
    public List<Episode> getTodayEpisodes(String sessionId) {
        return episodeStore.getTodayEpisodes().stream()
            .filter(ep -> sessionId.equals(ep.getMetadata().get("session_id")))
            .collect(Collectors.toList());
    }

    /**
     * 获取本周的情节
     */
    public List<Episode> getThisWeekEpisodes(String sessionId) {
        return episodeStore.getThisWeekEpisodes().stream()
            .filter(ep -> sessionId.equals(ep.getMetadata().get("session_id")))
            .collect(Collectors.toList());
    }

    /**
     * 搜索情节
     */
    public List<Episode> searchEpisodes(String sessionId, String keyword) {
        return episodeStore.searchEpisodes(keyword).stream()
            .filter(ep -> sessionId.equals(ep.getMetadata().get("session_id")))
            .collect(Collectors.toList());
    }

    /**
     * 按时间范围获取情节
     */
    public List<Episode> getEpisodesBetween(String sessionId, Long startTime, Long endTime) {
        return episodeStore.getEpisodesBetween(startTime, endTime).stream()
            .filter(ep -> sessionId.equals(ep.getMetadata().get("session_id")))
            .collect(Collectors.toList());
    }

    /**
     * 获取情节详情
     */
    public Episode getEpisode(String episodeId) {
        return episodeStore.getEpisode(episodeId);
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        return episodeStore.getStatistics();
    }

    /**
     * 获取统计信息（指定会话）
     */
    public Map<String, Object> getStatistics(String sessionId) {
        List<Episode> sessionEpisodes = episodeStore.getEpisodesBySession(sessionId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_episodes", sessionEpisodes.size());
        stats.put("total_events", sessionEpisodes.stream()
            .mapToInt(Episode::getEventCount)
            .sum());
        stats.put("ongoing_episodes", sessionEpisodes.stream()
            .filter(ep -> ep.getStatus() == Episode.EpisodeStatus.ONGOING)
            .count());
        stats.put("completed_episodes", sessionEpisodes.stream()
            .filter(ep -> ep.getStatus() == Episode.EpisodeStatus.COMPLETED)
            .count());

        return stats;
    }
}
