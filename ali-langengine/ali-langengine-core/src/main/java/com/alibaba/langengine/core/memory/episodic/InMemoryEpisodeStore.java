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

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存情节存储实现
 *
 * 基于内存的情节存储，适合中小规模的情节数据
 *
 * 特点：
 * - 快速读写，无需外部依赖
 * - 线程安全（使用ConcurrentHashMap）
 * - 多维度索引优化
 * - 适合原型开发和测试
 *
 * @author xiaoxuan.lp
 */
public class InMemoryEpisodeStore implements EpisodeStore {

    /**
     * 情节存储
     */
    private final Map<String, Episode> episodes;

    /**
     * 会话索引（sessionId -> episodeIds）
     */
    private final Map<String, Set<String>> sessionIndex;

    /**
     * 类型索引（episodeType -> episodeIds）
     */
    private final Map<Episode.EpisodeType, Set<String>> typeIndex;

    /**
     * 状态索引（status -> episodeIds）
     */
    private final Map<Episode.EpisodeStatus, Set<String>> statusIndex;

    /**
     * 参与者索引（participant -> episodeIds）
     */
    private final Map<String, Set<String>> participantIndex;

    /**
     * 地点索引（location -> episodeIds）
     */
    private final Map<String, Set<String>> locationIndex;

    /**
     * 标签索引（tag -> episodeIds）
     */
    private final Map<String, Set<String>> tagIndex;

    /**
     * 时间索引（按天）（date -> episodeIds）
     */
    private final Map<String, Set<String>> dateIndex;

    public InMemoryEpisodeStore() {
        this.episodes = new ConcurrentHashMap<>();
        this.sessionIndex = new ConcurrentHashMap<>();
        this.typeIndex = new ConcurrentHashMap<>();
        this.statusIndex = new ConcurrentHashMap<>();
        this.participantIndex = new ConcurrentHashMap<>();
        this.locationIndex = new ConcurrentHashMap<>();
        this.tagIndex = new ConcurrentHashMap<>();
        this.dateIndex = new ConcurrentHashMap<>();
    }

    // ==================== 情节操作 ====================

    @Override
    public void addEpisode(Episode episode) {
        if (episode == null || episode.getId() == null) {
            return;
        }

        episodes.put(episode.getId(), episode);
        updateIndices(episode);
    }

    @Override
    public void addEpisodes(List<Episode> episodes) {
        if (episodes != null) {
            episodes.forEach(this::addEpisode);
        }
    }

    @Override
    public Episode getEpisode(String episodeId) {
        return episodes.get(episodeId);
    }

    @Override
    public void updateEpisode(Episode episode) {
        if (episode == null || episode.getId() == null) {
            return;
        }

        // 删除旧索引
        Episode old = episodes.get(episode.getId());
        if (old != null) {
            removeFromIndices(old);
        }

        // 更新情节
        episodes.put(episode.getId(), episode);
        updateIndices(episode);
    }

    @Override
    public void deleteEpisode(String episodeId) {
        Episode episode = episodes.remove(episodeId);
        if (episode != null) {
            removeFromIndices(episode);
        }
    }

    @Override
    public List<Episode> getAllEpisodes() {
        return new ArrayList<>(episodes.values());
    }

    @Override
    public List<Episode> getEpisodesBySession(String sessionId) {
        Set<String> episodeIds = sessionIndex.get(sessionId);
        if (episodeIds == null) {
            return new ArrayList<>();
        }

        return episodeIds.stream()
            .map(episodes::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    // ==================== 时间查询 ====================

    @Override
    public List<Episode> getEpisodesBetween(Long startTime, Long endTime) {
        return episodes.values().stream()
            .filter(ep -> {
                if (ep.getStartTime() == null) {
                    return false;
                }
                if (startTime != null && ep.getStartTime() < startTime) {
                    return false;
                }
                if (endTime != null && ep.getStartTime() > endTime) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<Episode> getRecentEpisodes(int n) {
        return episodes.values().stream()
            .sorted((e1, e2) -> {
                Long t1 = e1.getStartTime() != null ? e1.getStartTime() : e1.getCreatedAt();
                Long t2 = e2.getStartTime() != null ? e2.getStartTime() : e2.getCreatedAt();
                return Long.compare(t2 != null ? t2 : 0, t1 != null ? t1 : 0);
            })
            .limit(n)
            .collect(Collectors.toList());
    }

    @Override
    public List<Episode> getTodayEpisodes() {
        LocalDate today = LocalDate.now();
        long startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return getEpisodesBetween(startOfDay, endOfDay);
    }

    @Override
    public List<Episode> getThisWeekEpisodes() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        long startTime = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTime = System.currentTimeMillis();
        return getEpisodesBetween(startTime, endTime);
    }

    @Override
    public List<Episode> getThisMonthEpisodes() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        long startTime = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTime = System.currentTimeMillis();
        return getEpisodesBetween(startTime, endTime);
    }

    // ==================== 类型和状态查询 ====================

    @Override
    public List<Episode> getEpisodesByType(Episode.EpisodeType episodeType) {
        Set<String> episodeIds = typeIndex.get(episodeType);
        if (episodeIds == null) {
            return new ArrayList<>();
        }

        return episodeIds.stream()
            .map(episodes::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public List<Episode> getEpisodesByStatus(Episode.EpisodeStatus status) {
        Set<String> episodeIds = statusIndex.get(status);
        if (episodeIds == null) {
            return new ArrayList<>();
        }

        return episodeIds.stream()
            .map(episodes::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public List<Episode> getOngoingEpisodes() {
        return getEpisodesByStatus(Episode.EpisodeStatus.ONGOING);
    }

    @Override
    public List<Episode> getCompletedEpisodes() {
        return getEpisodesByStatus(Episode.EpisodeStatus.COMPLETED);
    }

    // ==================== 参与者和地点查询 ====================

    @Override
    public List<Episode> getEpisodesByParticipant(String participant) {
        Set<String> episodeIds = participantIndex.get(participant);
        if (episodeIds == null) {
            return new ArrayList<>();
        }

        return episodeIds.stream()
            .map(episodes::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public List<Episode> getEpisodesByLocation(String location) {
        Set<String> episodeIds = locationIndex.get(location);
        if (episodeIds == null) {
            return new ArrayList<>();
        }

        return episodeIds.stream()
            .map(episodes::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public List<Episode> getEpisodesByTag(String tag) {
        Set<String> episodeIds = tagIndex.get(tag);
        if (episodeIds == null) {
            return new ArrayList<>();
        }

        return episodeIds.stream()
            .map(episodes::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    // ==================== 重要性查询 ====================

    @Override
    public List<Episode> getTopEpisodes(int topN) {
        return episodes.values().stream()
            .sorted((e1, e2) -> {
                double imp1 = e1.getImportance() != null ? e1.getImportance() : 0.0;
                double imp2 = e2.getImportance() != null ? e2.getImportance() : 0.0;
                return Double.compare(imp2, imp1);
            })
            .limit(topN)
            .collect(Collectors.toList());
    }

    @Override
    public List<Episode> getEpisodesByImportance(double threshold) {
        return episodes.values().stream()
            .filter(ep -> ep.getImportance() != null && ep.getImportance() >= threshold)
            .sorted((e1, e2) -> Double.compare(e2.getImportance(), e1.getImportance()))
            .collect(Collectors.toList());
    }

    // ==================== 相似度查询 ====================

    @Override
    public List<Episode> findSimilarEpisodes(Episode episode, int topN) {
        if (episode == null) {
            return new ArrayList<>();
        }

        return episodes.values().stream()
            .filter(ep -> !ep.getId().equals(episode.getId()))
            .map(ep -> new SimilarityPair(ep, episode.similarityTo(ep)))
            .filter(pair -> pair.similarity > 0)
            .sorted((p1, p2) -> Double.compare(p2.similarity, p1.similarity))
            .limit(topN)
            .map(pair -> pair.episode)
            .collect(Collectors.toList());
    }

    @Override
    public List<Episode> findSimilarEpisodes(String episodeId, int topN) {
        Episode episode = episodes.get(episodeId);
        if (episode == null) {
            return new ArrayList<>();
        }
        return findSimilarEpisodes(episode, topN);
    }

    private static class SimilarityPair {
        Episode episode;
        double similarity;

        SimilarityPair(Episode episode, double similarity) {
            this.episode = episode;
            this.similarity = similarity;
        }
    }

    // ==================== 搜索 ====================

    @Override
    public List<Episode> searchEpisodes(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return new ArrayList<>();
        }

        String lowerKeyword = keyword.toLowerCase();
        return episodes.values().stream()
            .filter(ep -> matchesKeyword(ep, lowerKeyword))
            .collect(Collectors.toList());
    }

    private boolean matchesKeyword(Episode episode, String keyword) {
        // 搜索标题
        if (episode.getTitle() != null && episode.getTitle().toLowerCase().contains(keyword)) {
            return true;
        }

        // 搜索描述
        if (episode.getDescription() != null && episode.getDescription().toLowerCase().contains(keyword)) {
            return true;
        }

        // 搜索摘要
        if (episode.getSummary() != null && episode.getSummary().toLowerCase().contains(keyword)) {
            return true;
        }

        // 搜索事件内容
        for (Event event : episode.getEvents()) {
            if (event.getContent() != null && event.getContent().toLowerCase().contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<Episode> searchEpisodes(EpisodeSearchCriteria criteria) {
        if (criteria == null) {
            return new ArrayList<>();
        }

        return episodes.values().stream()
            .filter(ep -> matchesCriteria(ep, criteria))
            .limit(criteria.getLimit() != null ? criteria.getLimit() : Integer.MAX_VALUE)
            .collect(Collectors.toList());
    }

    private boolean matchesCriteria(Episode episode, EpisodeSearchCriteria criteria) {
        // 关键词匹配
        if (criteria.getKeyword() != null && !matchesKeyword(episode, criteria.getKeyword().toLowerCase())) {
            return false;
        }

        // 类型匹配
        if (criteria.getEpisodeType() != null && episode.getEpisodeType() != criteria.getEpisodeType()) {
            return false;
        }

        // 状态匹配
        if (criteria.getStatus() != null && episode.getStatus() != criteria.getStatus()) {
            return false;
        }

        // 参与者匹配
        if (criteria.getParticipant() != null && !episode.getParticipants().contains(criteria.getParticipant())) {
            return false;
        }

        // 地点匹配
        if (criteria.getLocation() != null && !criteria.getLocation().equals(episode.getLocation())) {
            return false;
        }

        // 标签匹配
        if (criteria.getTag() != null && !episode.getTags().contains(criteria.getTag())) {
            return false;
        }

        // 时间范围匹配
        if (episode.getStartTime() != null) {
            if (criteria.getStartTime() != null && episode.getStartTime() < criteria.getStartTime()) {
                return false;
            }
            if (criteria.getEndTime() != null && episode.getStartTime() > criteria.getEndTime()) {
                return false;
            }
        }

        // 重要性匹配
        if (criteria.getMinImportance() != null) {
            if (episode.getImportance() == null || episode.getImportance() < criteria.getMinImportance()) {
                return false;
            }
        }

        return true;
    }

    // ==================== 统计和分析 ====================

    @Override
    public int getEpisodeCount() {
        return episodes.size();
    }

    @Override
    public int getTotalEventCount() {
        return episodes.values().stream()
            .mapToInt(Episode::getEventCount)
            .sum();
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("episode_count", getEpisodeCount());
        stats.put("total_event_count", getTotalEventCount());
        stats.put("episode_count_by_type", getEpisodeCountByType());
        stats.put("episode_count_by_status", getEpisodeCountByStatus());

        // 平均事件数
        double avgEvents = episodes.isEmpty() ? 0 :
            (double) getTotalEventCount() / episodes.size();
        stats.put("avg_events_per_episode", avgEvents);

        // 平均重要性
        double avgImportance = episodes.values().stream()
            .filter(ep -> ep.getImportance() != null)
            .mapToDouble(Episode::getImportance)
            .average()
            .orElse(0.0);
        stats.put("avg_importance", avgImportance);

        return stats;
    }

    @Override
    public Map<Episode.EpisodeType, Integer> getEpisodeCountByType() {
        Map<Episode.EpisodeType, Integer> counts = new HashMap<>();
        for (Episode.EpisodeType type : Episode.EpisodeType.values()) {
            Set<String> ids = typeIndex.get(type);
            counts.put(type, ids != null ? ids.size() : 0);
        }
        return counts;
    }

    @Override
    public Map<Episode.EpisodeStatus, Integer> getEpisodeCountByStatus() {
        Map<Episode.EpisodeStatus, Integer> counts = new HashMap<>();
        for (Episode.EpisodeStatus status : Episode.EpisodeStatus.values()) {
            Set<String> ids = statusIndex.get(status);
            counts.put(status, ids != null ? ids.size() : 0);
        }
        return counts;
    }

    // ==================== 维护操作 ====================

    @Override
    public void clear() {
        episodes.clear();
        clearAllIndices();
    }

    @Override
    public void clearSession(String sessionId) {
        Set<String> episodeIds = sessionIndex.get(sessionId);
        if (episodeIds != null) {
            new HashSet<>(episodeIds).forEach(this::deleteEpisode);
            sessionIndex.remove(sessionId);
        }
    }

    @Override
    public int deleteEpisodesBefore(Long beforeTime) {
        List<String> toDelete = episodes.values().stream()
            .filter(ep -> ep.getStartTime() != null && ep.getStartTime() < beforeTime)
            .map(Episode::getId)
            .collect(Collectors.toList());

        toDelete.forEach(this::deleteEpisode);
        return toDelete.size();
    }

    @Override
    public int deleteLowImportanceEpisodes(double threshold) {
        List<String> toDelete = episodes.values().stream()
            .filter(ep -> ep.getImportance() != null && ep.getImportance() < threshold)
            .map(Episode::getId)
            .collect(Collectors.toList());

        toDelete.forEach(this::deleteEpisode);
        return toDelete.size();
    }

    @Override
    public void archiveEpisode(String episodeId) {
        Episode episode = episodes.get(episodeId);
        if (episode != null) {
            episode.getMetadata().put("archived", true);
            episode.getMetadata().put("archived_at", System.currentTimeMillis());
            updateEpisode(episode);
        }
    }

    @Override
    public int archiveEpisodesBefore(Long beforeTime) {
        List<Episode> toArchive = episodes.values().stream()
            .filter(ep -> ep.getStartTime() != null && ep.getStartTime() < beforeTime)
            .collect(Collectors.toList());

        toArchive.forEach(ep -> archiveEpisode(ep.getId()));
        return toArchive.size();
    }

    // ==================== 索引管理 ====================

    /**
     * 更新所有索引
     */
    private void updateIndices(Episode episode) {
        String episodeId = episode.getId();

        // 会话索引
        String sessionId = (String) episode.getMetadata().get("session_id");
        if (sessionId != null) {
            sessionIndex.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(episodeId);
        }

        // 类型索引
        if (episode.getEpisodeType() != null) {
            typeIndex.computeIfAbsent(episode.getEpisodeType(), k -> ConcurrentHashMap.newKeySet())
                .add(episodeId);
        }

        // 状态索引
        if (episode.getStatus() != null) {
            statusIndex.computeIfAbsent(episode.getStatus(), k -> ConcurrentHashMap.newKeySet())
                .add(episodeId);
        }

        // 参与者索引
        if (episode.getParticipants() != null) {
            for (String participant : episode.getParticipants()) {
                participantIndex.computeIfAbsent(participant, k -> ConcurrentHashMap.newKeySet())
                    .add(episodeId);
            }
        }

        // 地点索引
        if (episode.getLocation() != null) {
            locationIndex.computeIfAbsent(episode.getLocation(), k -> ConcurrentHashMap.newKeySet())
                .add(episodeId);
        }

        // 标签索引
        if (episode.getTags() != null) {
            for (String tag : episode.getTags()) {
                tagIndex.computeIfAbsent(tag, k -> ConcurrentHashMap.newKeySet())
                    .add(episodeId);
            }
        }

        // 日期索引
        if (episode.getStartTime() != null) {
            String dateKey = getDateKey(episode.getStartTime());
            dateIndex.computeIfAbsent(dateKey, k -> ConcurrentHashMap.newKeySet())
                .add(episodeId);
        }
    }

    /**
     * 从所有索引中删除情节
     */
    private void removeFromIndices(Episode episode) {
        String episodeId = episode.getId();

        // 会话索引
        String sessionId = (String) episode.getMetadata().get("session_id");
        if (sessionId != null) {
            Set<String> set = sessionIndex.get(sessionId);
            if (set != null) set.remove(episodeId);
        }

        // 类型索引
        if (episode.getEpisodeType() != null) {
            Set<String> set = typeIndex.get(episode.getEpisodeType());
            if (set != null) set.remove(episodeId);
        }

        // 状态索引
        if (episode.getStatus() != null) {
            Set<String> set = statusIndex.get(episode.getStatus());
            if (set != null) set.remove(episodeId);
        }

        // 参与者索引
        if (episode.getParticipants() != null) {
            for (String participant : episode.getParticipants()) {
                Set<String> set = participantIndex.get(participant);
                if (set != null) set.remove(episodeId);
            }
        }

        // 地点索引
        if (episode.getLocation() != null) {
            Set<String> set = locationIndex.get(episode.getLocation());
            if (set != null) set.remove(episodeId);
        }

        // 标签索引
        if (episode.getTags() != null) {
            for (String tag : episode.getTags()) {
                Set<String> set = tagIndex.get(tag);
                if (set != null) set.remove(episodeId);
            }
        }

        // 日期索引
        if (episode.getStartTime() != null) {
            String dateKey = getDateKey(episode.getStartTime());
            Set<String> set = dateIndex.get(dateKey);
            if (set != null) set.remove(episodeId);
        }
    }

    /**
     * 清空所有索引
     */
    private void clearAllIndices() {
        sessionIndex.clear();
        typeIndex.clear();
        statusIndex.clear();
        participantIndex.clear();
        locationIndex.clear();
        tagIndex.clear();
        dateIndex.clear();
    }

    /**
     * 获取日期键（YYYY-MM-DD格式）
     */
    private String getDateKey(Long timestamp) {
        LocalDate date = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
        return date.toString();
    }
}
