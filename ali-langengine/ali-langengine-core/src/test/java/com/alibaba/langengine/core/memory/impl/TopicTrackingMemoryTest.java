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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 话题跟踪记忆测试类
 *
 * @author xiaoxuan.lp
 */
public class TopicTrackingMemoryTest {

    private TopicTrackingMemory memory;

    @BeforeEach
    public void setUp() {
        memory = new TopicTrackingMemory();
        // 设置一些测试关键词
        memory.setTopicKeywords(Arrays.asList("AI", "机器学习", "编程", "Java", "Python", "旅行", "美食"));
        memory.setInterestThreshold(0.2);
    }

    @Test
    public void testBasicTopicExtraction() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", "我想学习机器学习和深度学习");

        Map<String, Object> outputs = new HashMap<>();
        outputs.put("output", "机器学习是一个很有趣的领域，包含神经网络、决策树等技术。");

        memory.saveContext("session1", inputs, outputs);

        // 检查是否提取了正确的话题
        Map<String, Object> memories = memory.loadMemoryVariables("session1", new HashMap<>());
        String topics = (String) memories.get("topics");
        assertTrue(topics.contains("机器学习"));
    }

    @Test
    public void testTopicFrequencyTracking() {
        String sessionId = "session1";

        // 第一次对话
        Map<String, Object> inputs1 = new HashMap<>();
        inputs1.put("input", "我喜欢Java编程");

        Map<String, Object> outputs1 = new HashMap<>();
        outputs1.put("output", "Java是一门优秀的编程语言，有很好的生态系统。");

        memory.saveContext(sessionId, inputs1, outputs1);

        // 第二次对话
        Map<String, Object> inputs2 = new HashMap<>();
        inputs2.put("input", "Python也很不错，我正在学习Python");

        Map<String, Object> outputs2 = new HashMap<>();
        outputs2.put("output", "Python简洁易学，是AI和数据科学的首选语言。");

        memory.saveContext(sessionId, inputs2, outputs2);

        // 检查话题统计
        Map<String, Object> stats = memory.getTopicStats();
        assertEquals(2, stats.get("total_sessions"));
        assertTrue((Long) stats.get("total_interactions") >= 2);

        // 检查热门兴趣
        List<String> topInterests = memory.getTopInterests(3);
        assertTrue(topInterests.contains("编程") || topInterests.contains("Java") || topInterests.contains("Python"));
    }

    @Test
    public void testRelatedTopics() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", "机器学习很有趣，我想深入学习");

        Map<String, Object> outputs = new HashMap<>();
        outputs.put("output", "机器学习包含监督学习、无监督学习等分支。");

        memory.saveContext("session1", inputs, outputs);

        // 获取相关话题
        List<String> relatedTopics = memory.getRelatedTopics("机器学习");
        assertNotNull(relatedTopics);
        assertFalse(relatedTopics.isEmpty());

        System.out.println("机器学习相关话题：" + relatedTopics);
    }

    @Test
    public void testTopicSearch() {
        // 添加一些测试数据
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", "我喜欢Java编程，也对Python感兴趣");

        Map<String, Object> outputs = new HashMap<>();
        outputs.put("output", "Java和Python都是优秀的编程语言。");

        memory.saveContext("session1", inputs, outputs);

        // 搜索包含"编程"的话题
        List<String> programmingTopics = memory.searchTopics("编程");
        assertTrue(programmingTopics.contains("编程"));

        // 搜索包含"语言"的话题
        List<String> languageTopics = memory.searchTopics("语言");
        assertFalse(languageTopics.isEmpty());
    }

    @Test
    public void testMultipleSessions() {
        String session1 = "session1";
        String session2 = "session2";

        // Session 1: 技术话题
        Map<String, Object> inputs1 = new HashMap<>();
        inputs1.put("input", "Java编程很有趣");
        memory.saveContext(session1, inputs1, new HashMap<>());

        // Session 2: 生活话题
        Map<String, Object> inputs2 = new HashMap<>();
        inputs2.put("input", "我喜欢旅行和美食");
        memory.saveContext(session2, inputs2, new HashMap<>());

        // 检查各会话的话题
        Map<String, Object> memories1 = memory.loadMemoryVariables(session1, new HashMap<>());
        String topics1 = (String) memories1.get("topics");
        assertTrue(topics1.contains("Java") || topics1.contains("编程"));

        Map<String, Object> memories2 = memory.loadMemoryVariables(session2, new HashMap<>());
        String topics2 = (String) memories2.get("topics");
        assertTrue(topics2.contains("旅行") || topics2.contains("美食"));
    }

    @Test
    public void testInterestThreshold() {
        // 设置较低的阈值
        memory.setInterestThreshold(0.1);

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", "AI是未来的发展趋势");

        memory.saveContext("session1", inputs, new HashMap<>());

        // 应该有兴趣话题
        List<String> interests = memory.getTopInterests(5);
        assertFalse(interests.isEmpty());

        // 设置较高的阈值
        memory.setInterestThreshold(0.8);

        // 不应该有兴趣话题（除非有足够高的频率）
        List<String> highInterests = memory.getTopInterests(5);
        // 可能为空，因为只有一个对话
    }

    @Test
    public void testCustomTopicKeywords() {
        // 设置自定义关键词
        memory.setTopicKeywords(Arrays.asList("区块链", "加密货币", "元宇宙"));

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", "区块链技术很有前景，加密货币是热门话题");

        Map<String, Object> outputs = new HashMap<>();
        outputs.put("output", "是的，区块链和加密货币是当前热门的技术领域。");

        memory.saveContext("session1", inputs, outputs);

        // 检查是否提取了自定义关键词
        Map<String, Object> memories = memory.loadMemoryVariables("session1", new HashMap<>());
        String topics = (String) memories.get("topics");
        assertTrue(topics.contains("区块链") || topics.contains("加密货币"));
    }
}
