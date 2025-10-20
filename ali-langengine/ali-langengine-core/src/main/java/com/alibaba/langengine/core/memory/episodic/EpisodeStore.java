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

import java.util.List;
import java.util.Map;

/**
 * 情节存储接口
 *
 * 定义情节记忆的标准存储操作，支持多种后端实现：
 * - InMemoryEpisodeStore: 内存存储（快速、易用）
 * - DatabaseEpisodeStore: 数据库存储（持久化）
 * - VectorEpisodeStore: 向量数据库存储（语义检索）
 *
 * @author xiaoxuan.lp
 */
public interface EpisodeStore {

    // ==================== 情节操作 ====================

    /**
     * 添加情节
     *
     * @param episode 情节对象
     */
    void addEpisode(Episode episode);

    /**
     * 批量添加情节
     *
     * @param episodes 情节列表
     */
    void addEpisodes(List<Episode> episodes);

    /**
     * 获取情节
     *
     * @param episodeId 情节ID
     * @return 情节对象，不存在返回null
     */
    Episode getEpisode(String episodeId);

    /**
     * 更新情节
     *
     * @param episode 情节对象
     */
    void updateEpisode(Episode episode);

    /**
     * 删除情节
     *
     * @param episodeId 情节ID
     */
    void deleteEpisode(String episodeId);

    /**
     * 获取所有情节
     *
     * @return 情节列表
     */
    List<Episode> getAllEpisodes();

    /**
     * 获取会话的所有情节
     *
     * @param sessionId 会话ID
     * @return 情节列表
     */
    List<Episode> getEpisodesBySession(String sessionId);

    // ==================== 时间查询 ====================

    /**
     * 按时间范围查询情节
     *
     * @param startTime 开始时间（毫秒）
     * @param endTime 结束时间（毫秒）
     * @return 情节列表
     */
    List<Episode> getEpisodesBetween(Long startTime, Long endTime);

    /**
     * 获取最近的N个情节
     *
     * @param n 数量
     * @return 情节列表（按时间倒序）
     */
    List<Episode> getRecentEpisodes(int n);

    /**
     * 获取今天的情节
     *
     * @return 情节列表
     */
    List<Episode> getTodayEpisodes();

    /**
     * 获取本周的情节
     *
     * @return 情节列表
     */
    List<Episode> getThisWeekEpisodes();

    /**
     * 获取本月的情节
     *
     * @return 情节列表
     */
    List<Episode> getThisMonthEpisodes();

    // ==================== 类型和状态查询 ====================

    /**
     * 按类型查询情节
     *
     * @param episodeType 情节类型
     * @return 情节列表
     */
    List<Episode> getEpisodesByType(Episode.EpisodeType episodeType);

    /**
     * 按状态查询情节
     *
     * @param status 情节状态
     * @return 情节列表
     */
    List<Episode> getEpisodesByStatus(Episode.EpisodeStatus status);

    /**
     * 获取进行中的情节
     *
     * @return 情节列表
     */
    List<Episode> getOngoingEpisodes();

    /**
     * 获取已完成的情节
     *
     * @return 情节列表
     */
    List<Episode> getCompletedEpisodes();

    // ==================== 参与者和地点查询 ====================

    /**
     * 按参与者查询情节
     *
     * @param participant 参与者
     * @return 情节列表
     */
    List<Episode> getEpisodesByParticipant(String participant);

    /**
     * 按地点查询情节
     *
     * @param location 地点
     * @return 情节列表
     */
    List<Episode> getEpisodesByLocation(String location);

    /**
     * 按标签查询情节
     *
     * @param tag 标签
     * @return 情节列表
     */
    List<Episode> getEpisodesByTag(String tag);

    // ==================== 重要性查询 ====================

    /**
     * 获取最重要的N个情节
     *
     * @param topN 数量
     * @return 情节列表（按重要性降序）
     */
    List<Episode> getTopEpisodes(int topN);

    /**
     * 按重要性阈值查询情节
     *
     * @param threshold 重要性阈值 (0-1)
     * @return 情节列表
     */
    List<Episode> getEpisodesByImportance(double threshold);

    // ==================== 相似度查询 ====================

    /**
     * 查找与给定情节相似的情节
     *
     * @param episode 参考情节
     * @param topN 返回数量
     * @return 相似情节列表（按相似度降序）
     */
    List<Episode> findSimilarEpisodes(Episode episode, int topN);

    /**
     * 查找与给定情节相似的情节（排除自身）
     *
     * @param episodeId 参考情节ID
     * @param topN 返回数量
     * @return 相似情节列表
     */
    List<Episode> findSimilarEpisodes(String episodeId, int topN);

    // ==================== 搜索 ====================

    /**
     * 关键词搜索情节
     *
     * @param keyword 关键词
     * @return 情节列表
     */
    List<Episode> searchEpisodes(String keyword);

    /**
     * 复杂条件搜索情节
     *
     * @param criteria 搜索条件
     * @return 情节列表
     */
    List<Episode> searchEpisodes(EpisodeSearchCriteria criteria);

    /**
     * 情节搜索条件
     */
    class EpisodeSearchCriteria {
        private String keyword;
        private Episode.EpisodeType episodeType;
        private Episode.EpisodeStatus status;
        private String participant;
        private String location;
        private String tag;
        private Long startTime;
        private Long endTime;
        private Double minImportance;
        private Integer limit;

        // Getters and Setters
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public Episode.EpisodeType getEpisodeType() { return episodeType; }
        public void setEpisodeType(Episode.EpisodeType episodeType) { this.episodeType = episodeType; }

        public Episode.EpisodeStatus getStatus() { return status; }
        public void setStatus(Episode.EpisodeStatus status) { this.status = status; }

        public String getParticipant() { return participant; }
        public void setParticipant(String participant) { this.participant = participant; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }

        public Long getStartTime() { return startTime; }
        public void setStartTime(Long startTime) { this.startTime = startTime; }

        public Long getEndTime() { return endTime; }
        public void setEndTime(Long endTime) { this.endTime = endTime; }

        public Double getMinImportance() { return minImportance; }
        public void setMinImportance(Double minImportance) { this.minImportance = minImportance; }

        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
    }

    // ==================== 统计和分析 ====================

    /**
     * 获取情节总数
     *
     * @return 情节数量
     */
    int getEpisodeCount();

    /**
     * 获取事件总数
     *
     * @return 事件数量
     */
    int getTotalEventCount();

    /**
     * 获取统计信息
     *
     * @return 统计信息Map
     */
    Map<String, Object> getStatistics();

    /**
     * 按类型统计情节数量
     *
     * @return 类型->数量的映射
     */
    Map<Episode.EpisodeType, Integer> getEpisodeCountByType();

    /**
     * 按状态统计情节数量
     *
     * @return 状态->数量的映射
     */
    Map<Episode.EpisodeStatus, Integer> getEpisodeCountByStatus();

    // ==================== 维护操作 ====================

    /**
     * 清空所有情节
     */
    void clear();

    /**
     * 清空指定会话的情节
     *
     * @param sessionId 会话ID
     */
    void clearSession(String sessionId);

    /**
     * 删除指定时间之前的情节
     *
     * @param beforeTime 时间戳
     * @return 删除的情节数量
     */
    int deleteEpisodesBefore(Long beforeTime);

    /**
     * 删除低重要性情节
     *
     * @param threshold 重要性阈值
     * @return 删除的情节数量
     */
    int deleteLowImportanceEpisodes(double threshold);

    /**
     * 归档情节（标记为已归档但不删除）
     *
     * @param episodeId 情节ID
     */
    void archiveEpisode(String episodeId);

    /**
     * 批量归档情节
     *
     * @param beforeTime 归档此时间之前的情节
     * @return 归档的情节数量
     */
    int archiveEpisodesBefore(Long beforeTime);
}
