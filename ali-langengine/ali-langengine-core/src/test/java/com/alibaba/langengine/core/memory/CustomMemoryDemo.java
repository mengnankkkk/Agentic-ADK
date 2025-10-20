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
package com.alibaba.langengine.core.memory;

import com.alibaba.langengine.core.memory.impl.TaskMemory;
import com.alibaba.langengine.core.memory.impl.TopicTrackingMemory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义记忆系统演示
 *
 * 本类展示了如何使用四种自定义记忆系统：
 * 1. 话题跟踪记忆 - 跟踪用户兴趣话题
 * 2. 任务记忆管理 - 跟踪和管理任务
 * 3. 知识图谱记忆 - 基于图结构的知识关系管理
 * 4. 临时记忆清单 - 临时信息存储与自动清理机制
 *
 * @author xiaoxuan.lp
 */
public class CustomMemoryDemo {

    public static void main(String[] args) {
        System.out.println("=== 自定义记忆系统演示 ===\n");

        // 1. 话题跟踪记忆演示
        demonstrateTopicTrackingMemory();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 2. 任务记忆管理演示
        demonstrateTaskMemory();
    }

    /**
     * 话题跟踪记忆演示
     */
    private static void demonstrateTopicTrackingMemory() {
        System.out.println("1. 话题跟踪记忆演示");
        System.out.println("-".repeat(30));

        TopicTrackingMemory topicMemory = new TopicTrackingMemory();

        // 设置自定义话题关键词
        topicMemory.setTopicKeywords(Arrays.asList(
            "AI", "机器学习", "编程", "Java", "Python", "旅行", "美食", "音乐"
        ));

        // 模拟多轮对话
        String sessionId = "demo_session";

        // 第一轮对话
        Map<String, Object> inputs1 = new HashMap<>();
        inputs1.put("input", "我最近对机器学习很感兴趣，想深入学习一下。");

        Map<String, Object> outputs1 = new HashMap<>();
        outputs1.put("output", "机器学习是一个很有前景的领域，包含监督学习、无监督学习等多种技术。");

        topicMemory.saveContext(sessionId, inputs1, outputs1);

        // 第二轮对话
        Map<String, Object> inputs2 = new HashMap<>();
        inputs2.put("input", "我也在学习Java编程，感觉很有趣。");

        Map<String, Object> outputs2 = new HashMap<>();
        outputs2.put("output", "Java是一门优秀的编程语言，有着广泛的应用场景。");

        topicMemory.saveContext(sessionId, inputs2, outputs2);

        // 第三轮对话
        Map<String, Object> inputs3 = new HashMap<>();
        inputs3.put("input", "除了技术，我还喜欢旅行和美食。");

        Map<String, Object> outputs3 = new HashMap<>();
        outputs3.put("output", "旅行和美食都是很好的放松方式，能丰富生活体验。");

        topicMemory.saveContext(sessionId, inputs3, outputs3);

        // 获取记忆变量
        Map<String, Object> memories = topicMemory.loadMemoryVariables(sessionId, new HashMap<>());

        System.out.println("话题记录：" + memories.get("topics"));
        System.out.println("兴趣分析：" + memories.get("interests"));

        // 获取热门兴趣
        List<String> topInterests = topicMemory.getTopInterests(3);
        System.out.println("热门兴趣：" + topInterests);

        // 获取话题统计
        Map<String, Object> stats = topicMemory.getTopicStats();
        System.out.println("话题统计：" + stats);

        System.out.println();
    }


    /**
     * 任务记忆管理演示
     */
    private static void demonstrateTaskMemory() {
        System.out.println("3. 任务记忆管理演示");
        System.out.println("-".repeat(30));

        TaskMemory taskMemory = new TaskMemory();

        // 设置自定义任务关键词
        taskMemory.setTaskKeywords(Arrays.asList(
            "需要", "要", "计划", "准备", "安排", "todo", "task"
        ));

        String sessionId = "task_session";

        // 第一轮对话 - 创建任务
        Map<String, Object> inputs1 = new HashMap<>();
        inputs1.put("input", "我需要完成项目报告，需要明天提交。");

        Map<String, Object> outputs1 = new HashMap<>();
        outputs1.put("output", "好的，我帮你记录这个任务。记得明天提交项目报告。");

        taskMemory.saveContext(sessionId, inputs1, outputs1);

        // 第二轮对话 - 创建另一个任务
        Map<String, Object> inputs2 = new HashMap<>();
        inputs2.put("input", "还要准备下周的产品演示，要提前安排时间。");

        Map<String, Object> outputs2 = new HashMap<>();
        outputs2.put("output", "收到，我记录了产品演示的任务。");

        taskMemory.saveContext(sessionId, inputs2, outputs2);

        // 获取记忆变量
        Map<String, Object> memories = taskMemory.loadMemoryVariables(sessionId, new HashMap<>());

        System.out.println("任务概览：" + memories.get("tasks"));
        System.out.println("任务提醒：" + memories.get("task_reminders"));

        // 获取待处理任务
        List<TaskMemory.TaskInfo> pendingTasks = taskMemory.getTasksByStatus(TaskMemory.TaskStatus.PENDING);
        System.out.println("待处理任务：");
        for (TaskMemory.TaskInfo task : pendingTasks) {
            System.out.println("  - " + task.getTitle() + " (优先级: " + task.getPriority().getDisplayName() + ")");
            if (task.getDueTime() != null) {
                System.out.println("    截止时间: " + task.getDueTime().toString());
            }
        }

        // 更新任务状态（模拟完成任务）
        if (!pendingTasks.isEmpty()) {
            TaskMemory.TaskInfo firstTask = pendingTasks.get(0);
            boolean updated = taskMemory.updateTaskStatus(firstTask.getTaskId(), TaskMemory.TaskStatus.COMPLETED);
            System.out.println("任务完成状态更新：" + (updated ? "成功" : "失败"));
        }

        // 获取任务统计
        Map<String, Object> stats = taskMemory.getTaskStats(sessionId);
        System.out.println("任务统计：" + stats);

        System.out.println();
    }
}
