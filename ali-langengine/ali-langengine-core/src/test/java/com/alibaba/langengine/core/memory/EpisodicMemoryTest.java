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
package com.alibaba.langengine.core.memory;

import com.alibaba.langengine.core.memory.episodic.*;
import com.alibaba.langengine.core.memory.impl.EpisodicMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EpisodicMemory 单元测试
 *
 * @author xiaoxuan.lp
 */
public class EpisodicMemoryTest {

    private EpisodicMemory memory;
    private String sessionId;

    @BeforeEach
    public void setUp() {
        memory = EpisodicMemory.builder()
            .autoCreateEpisodes(true)
            .episodeTimeout(30 * 60 * 1000L)
            .maxEpisodesInContext(5)
            .build();

        sessionId = "test-session-" + System.currentTimeMillis();
    }

    @Test
    public void testBasicEpisodicMemoryCreation() {
        assertNotNull(memory);
        assertNotNull(memory.getEpisodeStore());
        assertTrue(memory.isAutoCreateEpisodes());
        assertEquals(30 * 60 * 1000L, memory.getEpisodeTimeout());
    }

    @Test
    public void testMemoryVariables() {
        List<String> variables = memory.memoryVariables();
        assertNotNull(variables);
        assertEquals(1, variables.size());
        assertTrue(variables.contains("episodic_memory"));
    }

    @Test
    public void testEventCreation() {
        Event event = Event.builder()
            .content("测试事件")
            .eventType(Event.EventType.CONVERSATION)
            .location("测试地点")
            .addParticipant("用户A")
            .importance(0.7)
            .addTag("测试")
            .build();

        assertNotNull(event);
        assertNotNull(event.getId());
        assertEquals("测试事件", event.getContent());
        assertEquals(Event.EventType.CONVERSATION, event.getEventType());
        assertEquals(0.7, event.getImportance());
        assertTrue(event.hasTag("测试"));
        assertTrue(event.hasParticipant("用户A"));
    }

    @Test
    public void testEpisodeCreation() {
        Episode episode = Episode.builder()
            .title("测试情节")
            .episodeType(Episode.EpisodeType.CONVERSATION)
            .location("客服对话")
            .addParticipant("客服")
            .addTag("客服")
            .build();

        assertNotNull(episode);
        assertNotNull(episode.getId());
        assertEquals("测试情节", episode.getTitle());
        assertEquals(Episode.EpisodeType.CONVERSATION, episode.getEpisodeType());
        assertEquals(Episode.EpisodeStatus.ONGOING, episode.getStatus());
    }

    @Test
    public void testAddEventToEpisode() {
        Episode episode = new Episode("测试情节", Episode.EpisodeType.TASK);

        Event event1 = new Event("事件1", Event.EventType.ACTION);
        Event event2 = new Event("事件2", Event.EventType.OBSERVATION);

        episode.addEvent(event1);
        episode.addEvent(event2);

        assertEquals(2, episode.getEventCount());
        assertNotNull(episode.getStartTime());
        assertNotNull(episode.getEndTime());
    }

    @Test
    public void testEpisodeCompletion() {
        Episode episode = new Episode("测试情节", Episode.EpisodeType.CONVERSATION);
        assertEquals(Episode.EpisodeStatus.ONGOING, episode.getStatus());

        episode.complete();

        assertEquals(Episode.EpisodeStatus.COMPLETED, episode.getStatus());
        assertNotNull(episode.getEndTime());
    }

    @Test
    public void testEpisodeStoreOperations() {
        EpisodeStore store = memory.getEpisodeStore();

        Episode episode = new Episode("测试情节", Episode.EpisodeType.CONVERSATION);
        episode.getMetadata().put("session_id", sessionId);

        store.addEpisode(episode);

        Episode retrieved = store.getEpisode(episode.getId());
        assertNotNull(retrieved);
        assertEquals("测试情节", retrieved.getTitle());

        store.deleteEpisode(episode.getId());
        assertNull(store.getEpisode(episode.getId()));
    }

    @Test
    public void testSaveContext() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", "你好，请问产品价格？");

        Map<String, Object> outputs = new HashMap<>();
        outputs.put("output", "产品价格是100元");

        memory.saveContext(sessionId, inputs, outputs);

        // 验证情节被创建
        List<Episode> episodes = memory.getEpisodeStore().getEpisodesBySession(sessionId);
        assertFalse(episodes.isEmpty());

        Episode episode = episodes.get(0);
        assertTrue(episode.getEventCount() >= 2);
    }

    @Test
    public void testLoadMemoryVariables() {
        // 先保存一些上下文
        for (int i = 0; i < 3; i++) {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("input", "问题 " + (i + 1));

            Map<String, Object> outputs = new HashMap<>();
            outputs.put("output", "回答 " + (i + 1));

            memory.saveContext(sessionId, inputs, outputs);
        }

        // 完成当前情节以便能够检索
        memory.finishCurrentEpisode(sessionId);

        // 加载记忆变量
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", "总结一下");

        Map<String, Object> context = memory.loadMemoryVariables(sessionId, inputs);

        assertNotNull(context);
        assertTrue(context.containsKey("episodic_memory"));
    }

    @Test
    public void testTimeRangeQuery() throws InterruptedException {
        Episode episode1 = new Episode("早上的对话", Episode.EpisodeType.CONVERSATION);
        episode1.getMetadata().put("session_id", sessionId);
        episode1.addEvent(new Event("早上好", Event.EventType.CONVERSATION));

        Thread.sleep(100);  // 确保时间差

        Episode episode2 = new Episode("下午的对话", Episode.EpisodeType.CONVERSATION);
        episode2.getMetadata().put("session_id", sessionId);
        episode2.addEvent(new Event("下午好", Event.EventType.CONVERSATION));

        memory.getEpisodeStore().addEpisode(episode1);
        memory.getEpisodeStore().addEpisode(episode2);

        Long midTime = System.currentTimeMillis();

        List<Episode> recent = memory.getEpisodesBetween(sessionId,
            episode1.getStartTime(), midTime);

        assertTrue(recent.size() >= 1);
    }

    @Test
    public void testSearchEpisodes() {
        Episode episode = new Episode("关于价格的咨询", Episode.EpisodeType.CONVERSATION);
        episode.getMetadata().put("session_id", sessionId);
        episode.addEvent(new Event("产品价格是多少？", Event.EventType.QUESTION));

        memory.getEpisodeStore().addEpisode(episode);

        List<Episode> results = memory.searchEpisodes(sessionId, "价格");

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getTitle().contains("价格"));
    }

    @Test
    public void testManualEpisodeManagement() {
        // 手动开始新情节
        Episode episode = memory.startEpisode(sessionId, "客户咨询", Episode.EpisodeType.CONVERSATION);

        assertNotNull(episode);
        assertEquals("客户咨询", episode.getTitle());
        assertEquals(Episode.EpisodeStatus.ONGOING, episode.getStatus());

        // 添加事件
        Event event = Event.builder()
            .content("询问产品功能")
            .eventType(Event.EventType.QUESTION)
            .build();

        memory.addEvent(sessionId, event);

        // 完成情节
        memory.finishCurrentEpisode(sessionId);

        Episode completed = memory.getEpisode(episode.getId());
        assertEquals(Episode.EpisodeStatus.COMPLETED, completed.getStatus());
    }

    @Test
    public void testTodayEpisodes() {
        // 创建今天的情节
        Episode todayEpisode = new Episode("今天的对话", Episode.EpisodeType.CONVERSATION);
        todayEpisode.getMetadata().put("session_id", sessionId);
        todayEpisode.addEvent(new Event("今天的事件", Event.EventType.CONVERSATION));

        memory.getEpisodeStore().addEpisode(todayEpisode);

        List<Episode> todayEpisodes = memory.getTodayEpisodes(sessionId);

        assertFalse(todayEpisodes.isEmpty());
    }

    @Test
    public void testStatistics() {
        // 添加一些情节
        for (int i = 0; i < 3; i++) {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("input", "测试 " + i);

            Map<String, Object> outputs = new HashMap<>();
            outputs.put("output", "响应 " + i);

            memory.saveContext(sessionId, inputs, outputs);
        }

        Map<String, Object> stats = memory.getStatistics(sessionId);

        assertNotNull(stats);
        assertTrue(stats.containsKey("total_episodes"));
        assertTrue(stats.containsKey("total_events"));
    }

    @Test
    public void testClearSession() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", "测试消息");

        Map<String, Object> outputs = new HashMap<>();
        outputs.put("output", "收到");

        memory.saveContext(sessionId, inputs, outputs);

        // 验证情节存在
        List<Episode> before = memory.getEpisodeStore().getEpisodesBySession(sessionId);
        assertFalse(before.isEmpty());

        // 清除会话
        memory.clear(sessionId);

        // 验证情节已清除
        List<Episode> after = memory.getEpisodeStore().getEpisodesBySession(sessionId);
        assertTrue(after.isEmpty());
    }

    @Test
    public void testEventSimilarity() {
        Event event1 = Event.builder()
            .content("询问价格")
            .eventType(Event.EventType.QUESTION)
            .location("客服")
            .addParticipant("用户A")
            .addTag("价格")
            .build();

        Event event2 = Event.builder()
            .content("查询费用")
            .eventType(Event.EventType.QUESTION)
            .location("客服")
            .addParticipant("用户A")
            .addTag("价格")
            .build();

        double similarity = event1.similarityTo(event2);
        assertTrue(similarity > 0.5);  // 应该有较高相似度
    }

    @Test
    public void testEpisodeSimilarity() {
        Episode ep1 = Episode.builder()
            .title("产品咨询")
            .episodeType(Episode.EpisodeType.CONVERSATION)
            .location("客服")
            .addParticipant("客服A")
            .addTag("咨询")
            .build();

        Episode ep2 = Episode.builder()
            .title("服务咨询")
            .episodeType(Episode.EpisodeType.CONVERSATION)
            .location("客服")
            .addParticipant("客服A")
            .addTag("咨询")
            .build();

        double similarity = ep1.similarityTo(ep2);
        assertTrue(similarity > 0.5);
    }

    @Test
    public void testBuilderPattern() {
        EpisodicMemory customMemory = EpisodicMemory.builder()
            .autoCreateEpisodes(false)
            .episodeTimeout(60 * 1000L)
            .maxEpisodesInContext(10)
            .maxEventsInContext(50)
            .importanceThreshold(0.5)
            .build();

        assertFalse(customMemory.isAutoCreateEpisodes());
        assertEquals(60 * 1000L, customMemory.getEpisodeTimeout());
        assertEquals(10, customMemory.getMaxEpisodesInContext());
        assertEquals(50, customMemory.getMaxEventsInContext());
        assertEquals(0.5, customMemory.getImportanceThreshold());
    }
}
