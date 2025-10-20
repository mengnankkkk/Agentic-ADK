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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 任务记忆管理系统（Task Memory）
 *
 * 功能特性：
 * - 自动识别和提取用户任务
 * - 跟踪任务进度和状态
 * - 支持任务优先级和截止时间管理
 * - 提供任务提醒和到期通知
 * - 支持任务分类和标签管理
 * - 智能任务依赖关系分析
 *
 * 任务状态：
 * - PENDING: 待处理
 * - IN_PROGRESS: 进行中
 * - COMPLETED: 已完成
 * - CANCELLED: 已取消
 * - OVERDUE: 已过期
 *
 * 使用示例：
 * <pre>
 * TaskMemory memory = new TaskMemory();
 * memory.setTaskKeywords(Arrays.asList("需要", "要", "计划", "准备", "todo", "task"));
 * memory.setReminderEnabled(true);
 *
 * // 保存对话上下文，自动提取任务
 * memory.saveContext(inputs, outputs);
 *
 * // 获取待办任务
 * List<TaskInfo> pendingTasks = memory.getTasksByStatus(TaskStatus.PENDING);
 *
 * // 更新任务状态
 * memory.updateTaskStatus(taskId, TaskStatus.COMPLETED);
 *
 * // 获取任务提醒
 * List<TaskInfo> reminders = memory.getUpcomingReminders(24); // 24小时内
 * </pre>
 *
 * @author xiaoxuan.lp
 */
@Data
@Slf4j
public class TaskMemory extends BaseMemory {

    /**
     * 任务存储
     * key: sessionId
     * value: 该会话的任务列表
     */
    private Map<String, List<TaskInfo>> sessionTasks = new HashMap<>();

    /**
     * 全局任务存储（跨会话）
     * key: taskId
     * value: 任务信息
     */
    private Map<String, TaskInfo> globalTasks = new HashMap<>();

    /**
     * 任务关键词库
     * 用于识别任务相关语句
     */
    private List<String> taskKeywords = Arrays.asList(
        "需要", "要", "计划", "准备", "安排", "处理", "完成", "做", "执行", "实施",
        "todo", "task", "remind", "reminder", "schedule", "plan", "prepare", "arrange"
    );

    /**
     * 时间关键词库
     * 用于识别时间相关表达式
     */
    private List<String> timeKeywords = Arrays.asList(
        "今天", "明天", "后天", "下周", "本周", "下个月", "今晚", "明天上午", "下午",
        "today", "tomorrow", "next week", "this week", "next month", "tonight", "this afternoon"
    );

    /**
     * 优先级关键词库
     */
    private Map<String, TaskPriority> priorityKeywords = new HashMap<>();
    {
        priorityKeywords.put("紧急", TaskPriority.HIGH);
        priorityKeywords.put("重要", TaskPriority.HIGH);
        priorityKeywords.put("优先", TaskPriority.HIGH);
        priorityKeywords.put("urgent", TaskPriority.HIGH);
        priorityKeywords.put("important", TaskPriority.HIGH);
        priorityKeywords.put("priority", TaskPriority.HIGH);

        priorityKeywords.put("一般", TaskPriority.MEDIUM);
        priorityKeywords.put("普通", TaskPriority.MEDIUM);
        priorityKeywords.put("normal", TaskPriority.MEDIUM);

        priorityKeywords.put("低", TaskPriority.LOW);
        priorityKeywords.put("低优先级", TaskPriority.LOW);
        priorityKeywords.put("low", TaskPriority.LOW);
    }

    /**
     * 是否启用提醒功能
     */
    private boolean reminderEnabled = true;

    /**
     * 默认提醒提前时间（小时）
     */
    private int defaultReminderHours = 2;

    /**
     * 最大任务数量限制
     */
    private int maxTasksPerSession = 100;

    /**
     * 任务信息类
     */
    @Data
    public static class TaskInfo {
        private String taskId;              // 任务唯一ID
        private String sessionId;           // 所属会话ID
        private String title;               // 任务标题
        private String description;         // 任务描述
        private TaskStatus status;          // 任务状态
        private TaskPriority priority;      // 任务优先级
        private LocalDateTime createdTime;  // 创建时间
        private LocalDateTime updatedTime;  // 更新时间
        private LocalDateTime dueTime;      // 截止时间
        private LocalDateTime reminderTime; // 提醒时间
        private Set<String> tags;           // 任务标签
        private List<String> dependencies;  // 依赖任务ID列表
        private Map<String, Object> metadata; // 元数据

        public TaskInfo(String sessionId, String title, String description) {
            this.taskId = UUID.randomUUID().toString();
            this.sessionId = sessionId;
            this.title = title;
            this.description = description;
            this.status = TaskStatus.PENDING;
            this.priority = TaskPriority.MEDIUM;
            this.createdTime = LocalDateTime.now();
            this.updatedTime = LocalDateTime.now();
            this.tags = new HashSet<>();
            this.dependencies = new ArrayList<>();
            this.metadata = new HashMap<>();
        }

        /**
         * 更新任务状态
         */
        public void updateStatus(TaskStatus newStatus) {
            this.status = newStatus;
            this.updatedTime = LocalDateTime.now();
        }

        /**
         * 检查是否即将到期
         */
        public boolean isUpcoming(int hours) {
            if (dueTime == null) {
                return false;
            }
            return LocalDateTime.now().plusHours(hours).isAfter(dueTime);
        }

        /**
         * 检查是否已过期
         */
        public boolean isOverdue() {
            if (dueTime == null) {
                return false;
            }
            return LocalDateTime.now().isAfter(dueTime) && status != TaskStatus.COMPLETED;
        }
    }

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING("待处理"),
        IN_PROGRESS("进行中"),
        COMPLETED("已完成"),
        CANCELLED("已取消"),
        OVERDUE("已过期");

        private final String displayName;

        TaskStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 任务优先级枚举
     */
    public enum TaskPriority {
        LOW("低", 1),
        MEDIUM("中", 2),
        HIGH("高", 3);

        private final String displayName;
        private final int level;

        TaskPriority(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getLevel() {
            return level;
        }
    }

    @Override
    public List<String> memoryVariables() {
        return Arrays.asList("tasks", "task_reminders");
    }

    @Override
    public Map<String, Object> loadMemoryVariables(String sessionId, Map<String, Object> inputs) {
        Map<String, Object> result = new HashMap<>();

        // 获取任务摘要
        String taskSummary = getTaskSummary(sessionId);
        result.put("tasks", taskSummary);

        // 获取任务提醒
        if (reminderEnabled) {
            String reminders = getTaskReminders(sessionId, 24); // 24小时内提醒
            result.put("task_reminders", reminders);
        } else {
            result.put("task_reminders", "提醒功能已禁用");
        }

        return result;
    }

    @Override
    public void saveContext(String sessionId, Map<String, Object> inputs, Map<String, Object> outputs) {
        // 从对话中提取任务
        List<TaskExtraction> extractions = extractTasks(sessionId, inputs, outputs);

        for (TaskExtraction extraction : extractions) {
            TaskInfo task = createTask(sessionId, extraction);
            if (task != null) {
                addTask(task);
                log.debug("Created task for session {}: {}", sessionId, task.getTitle());
            }
        }

        // 更新过期任务状态
        updateOverdueTasks();
    }

    /**
     * 任务提取信息类
     */
    @Data
    private static class TaskExtraction {
        private String title;
        private String description;
        private TaskPriority priority;
        private LocalDateTime dueTime;
        private Set<String> tags;

        public TaskExtraction(String title, String description) {
            this.title = title;
            this.description = description;
            this.priority = TaskPriority.MEDIUM;
            this.tags = new HashSet<>();
        }
    }

    /**
     * 从对话中提取任务
     */
    private List<TaskExtraction> extractTasks(String sessionId, Map<String, Object> inputs, Map<String, Object> outputs) {
        List<TaskExtraction> extractions = new ArrayList<>();

        String inputText = inputs.values().stream()
            .map(Object::toString)
            .collect(Collectors.joining(" "));
        String outputText = outputs.values().stream()
            .map(Object::toString)
            .collect(Collectors.joining(" "));

        String combinedText = (inputText + " " + outputText).toLowerCase();

        // 检查是否包含任务关键词
        boolean hasTaskKeywords = taskKeywords.stream()
            .anyMatch(keyword -> combinedText.contains(keyword));

        if (!hasTaskKeywords) {
            return extractions;
        }

        // 提取任务模式：如"需要做XX"，"计划YY"等
        Pattern taskPattern = Pattern.compile("(?:需要|要|计划|准备|安排|处理)(?:去)?\\s*([^。！？\\s]{3,50})");
        Matcher taskMatcher = taskPattern.matcher(combinedText);

        while (taskMatcher.find()) {
            String taskTitle = taskMatcher.group(1);
            if (taskTitle != null && taskTitle.length() > 2) {
                TaskExtraction extraction = new TaskExtraction(taskTitle, "从对话中提取的任务");

                // 提取优先级
                extraction.setPriority(extractPriority(combinedText));

                // 提取截止时间
                extraction.setDueTime(extractDueTime(combinedText));

                // 提取标签
                extraction.setTags(extractTags(combinedText));

                extractions.add(extraction);
            }
        }

        return extractions;
    }

    /**
     * 提取优先级
     */
    private TaskPriority extractPriority(String text) {
        for (Map.Entry<String, TaskPriority> entry : priorityKeywords.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return TaskPriority.MEDIUM;
    }

    /**
     * 提取截止时间
     */
    private LocalDateTime extractDueTime(String text) {
        // 简单的日期时间提取逻辑
        LocalDateTime now = LocalDateTime.now();

        if (text.contains("今天")) {
            return now.plusHours(4); // 假设今天下午4点
        } else if (text.contains("明天")) {
            return now.plusDays(1).withHour(10); // 明天上午10点
        } else if (text.contains("下周")) {
            return now.plusWeeks(1).withHour(9); // 下周一上午9点
        } else if (text.contains("本周")) {
            return now.withHour(17); // 本周五下午5点
        }

        return null; // 无明确时间
    }

    /**
     * 提取标签
     */
    private Set<String> extractTags(String text) {
        Set<String> tags = new HashSet<>();

        // 简单的标签提取：查找#标签 或 @标签
        Pattern tagPattern = Pattern.compile("[#@]\\w+");
        Matcher tagMatcher = tagPattern.matcher(text);

        while (tagMatcher.find()) {
            tags.add(tagMatcher.group().substring(1)); // 去掉#或@前缀
        }

        return tags;
    }

    /**
     * 创建任务
     */
    private TaskInfo createTask(String sessionId, TaskExtraction extraction) {
        TaskInfo task = new TaskInfo(sessionId, extraction.getTitle(), extraction.getDescription());
        task.setPriority(extraction.getPriority());
        task.setDueTime(extraction.getDueTime());
        task.getTags().addAll(extraction.getTags());

        // 设置提醒时间
        if (task.getDueTime() != null && reminderEnabled) {
            task.setReminderTime(task.getDueTime().minusHours(defaultReminderHours));
        }

        return task;
    }

    /**
     * 添加任务到存储
     */
    private void addTask(TaskInfo task) {
        // 添加到会话任务
        sessionTasks.computeIfAbsent(task.getSessionId(), k -> new ArrayList<>());
        sessionTasks.get(task.getSessionId()).add(task);

        // 添加到全局任务（如果需要跨会话访问）
        globalTasks.put(task.getTaskId(), task);

        // 清理过多的任务
        cleanupExcessTasks(task.getSessionId());
    }

    /**
     * 清理过多的任务
     */
    private void cleanupExcessTasks(String sessionId) {
        List<TaskInfo> tasks = sessionTasks.get(sessionId);
        if (tasks == null || tasks.size() <= maxTasksPerSession) {
            return;
        }

        // 按优先级和创建时间排序，删除低优先级的旧任务
        tasks.sort((a, b) -> {
            // 首先按优先级排序（高优先级在前）
            int priorityCompare = Integer.compare(b.getPriority().getLevel(), a.getPriority().getLevel());
            if (priorityCompare != 0) {
                return priorityCompare;
            }

            // 优先级相同时，按创建时间排序（新任务在前）
            return b.getCreatedTime().compareTo(a.getCreatedTime());
        });

        // 保留前N个任务
        if (tasks.size() > maxTasksPerSession) {
            List<TaskInfo> tasksToRemove = tasks.subList(maxTasksPerSession, tasks.size());
            tasks.removeAll(tasksToRemove);

            // 从全局任务中也移除
            for (TaskInfo task : tasksToRemove) {
                globalTasks.remove(task.getTaskId());
            }
        }
    }

    /**
     * 更新任务状态
     */
    public boolean updateTaskStatus(String taskId, TaskStatus status) {
        TaskInfo task = globalTasks.get(taskId);
        if (task == null) {
            return false;
        }

        task.updateStatus(status);

        // 如果任务完成，检查依赖关系
        if (status == TaskStatus.COMPLETED) {
            checkDependentTasks(task);
        }

        return true;
    }

    /**
     * 检查依赖任务
     */
    private void checkDependentTasks(TaskInfo completedTask) {
        // 这里可以实现复杂的依赖关系检查逻辑
        // 例如：查找依赖于此任务的其他任务，并更新其状态或提醒用户
    }

    /**
     * 获取任务摘要
     */
    private String getTaskSummary(String sessionId) {
        List<TaskInfo> tasks = sessionTasks.get(sessionId);
        if (tasks == null || tasks.isEmpty()) {
            return "暂无任务记录";
        }

        long pendingCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count();
        long inProgressCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long completedCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();

        return String.format("任务概览：待处理 %d 个，进行中 %d 个，已完成 %d 个",
                           pendingCount, inProgressCount, completedCount);
    }

    /**
     * 获取任务提醒
     */
    private String getTaskReminders(String sessionId, int hours) {
        List<TaskInfo> tasks = sessionTasks.get(sessionId);
        if (tasks == null || tasks.isEmpty()) {
            return "暂无任务提醒";
        }

        List<TaskInfo> upcomingTasks = tasks.stream()
            .filter(task -> task.getReminderTime() != null)
            .filter(task -> task.isUpcoming(hours))
            .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
            .sorted((a, b) -> a.getReminderTime().compareTo(b.getReminderTime()))
            .collect(Collectors.toList());

        if (upcomingTasks.isEmpty()) {
            return "暂无即将到期的任务提醒";
        }

        StringBuilder reminders = new StringBuilder("即将到期任务：\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");

        for (TaskInfo task : upcomingTasks) {
            reminders.append(String.format("- %s (截止: %s, 优先级: %s)\n",
                task.getTitle(),
                task.getDueTime().format(formatter),
                task.getPriority().getDisplayName()));
        }

        return reminders.toString();
    }

    /**
     * 获取指定状态的任务
     */
    public List<TaskInfo> getTasksByStatus(TaskStatus status) {
        return globalTasks.values().stream()
            .filter(task -> task.getStatus() == status)
            .collect(Collectors.toList());
    }

    /**
     * 获取即将到期的任务
     */
    public List<TaskInfo> getUpcomingTasks(int hours) {
        return globalTasks.values().stream()
            .filter(task -> task.getDueTime() != null)
            .filter(task -> task.isUpcoming(hours))
            .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
            .sorted((a, b) -> a.getDueTime().compareTo(b.getDueTime()))
            .collect(Collectors.toList());
    }

    /**
     * 获取过期任务
     */
    public List<TaskInfo> getOverdueTasks() {
        return globalTasks.values().stream()
            .filter(TaskInfo::isOverdue)
            .collect(Collectors.toList());
    }

    /**
     * 更新过期任务状态
     */
    private void updateOverdueTasks() {
        List<TaskInfo> overdueTasks = getOverdueTasks();
        for (TaskInfo task : overdueTasks) {
            task.updateStatus(TaskStatus.OVERDUE);
        }
    }

    /**
     * 根据标签查找任务
     */
    public List<TaskInfo> getTasksByTag(String tag) {
        return globalTasks.values().stream()
            .filter(task -> task.getTags().contains(tag))
            .collect(Collectors.toList());
    }

    /**
     * 删除任务
     */
    public boolean deleteTask(String taskId) {
        TaskInfo task = globalTasks.remove(taskId);
        if (task == null) {
            return false;
        }

        // 从会话任务中移除
        List<TaskInfo> sessionTaskList = sessionTasks.get(task.getSessionId());
        if (sessionTaskList != null) {
            sessionTaskList.remove(task);
        }

        return true;
    }

    /**
     * 获取任务统计信息
     */
    public Map<String, Object> getTaskStats(String sessionId) {
        List<TaskInfo> tasks = sessionTasks.get(sessionId);
        if (tasks == null) {
            tasks = Collections.emptyList();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_tasks", tasks.size());

        Map<TaskStatus, Long> statusCounts = tasks.stream()
            .collect(Collectors.groupingBy(TaskInfo::getStatus, Collectors.counting()));
        stats.put("status_distribution", statusCounts);

        Map<TaskPriority, Long> priorityCounts = tasks.stream()
            .collect(Collectors.groupingBy(TaskInfo::getPriority, Collectors.counting()));
        stats.put("priority_distribution", priorityCounts);

        long overdueCount = tasks.stream().filter(TaskInfo::isOverdue).count();
        stats.put("overdue_tasks", overdueCount);

        return stats;
    }

    @Override
    public void clear(String sessionId) {
        if (sessionId != null) {
            List<TaskInfo> tasksToRemove = sessionTasks.get(sessionId);
            if (tasksToRemove != null) {
                for (TaskInfo task : tasksToRemove) {
                    globalTasks.remove(task.getTaskId());
                }
                sessionTasks.remove(sessionId);
            }
        } else {
            sessionTasks.clear();
            globalTasks.clear();
        }
    }
}
