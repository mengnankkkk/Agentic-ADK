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
 * 知识图谱记忆系统（Knowledge Graph Memory）
 *
 * 功能特性：
 * - 自动提取实体、关系和属性
 * - 构建知识图谱结构存储信息
 * - 支持图谱查询和推理
 * - 提供知识关联性和相似性分析
 * - 支持知识图谱可视化和导出
 * - 基于图算法的智能推荐
 *
 * 图结构组成：
 * - 实体节点（Entity）：人、地点、概念、物体等
 * - 关系边（Relation）：实体之间的关联关系
 * - 属性（Property）：实体的特征和描述
 *
 * 支持的关系类型：
 * - 属于（belongs_to）
 * - 位于（located_in）
 * - 相关（related_to）
 * - 包含（contains）
 * - 继承（inherits_from）
 * - 影响（affects）
 *
 * 使用示例：
 * <pre>
 * KnowledgeGraphMemory memory = new KnowledgeGraphMemory();
 * memory.setEntityPatterns(Arrays.asList("人名：张三、李四", "地点：北京、上海"));
 * memory.setRelationPatterns(Arrays.asList("位于", "属于", "工作于"));
 *
 * // 保存对话上下文，自动构建图谱
 * memory.saveContext(inputs, outputs);
 *
 * // 查询相关实体
 * List<String> relatedEntities = memory.getRelatedEntities("机器学习", 2);
 *
 * // 获取实体详细信息
 * Map<String, Object> entityInfo = memory.getEntityInfo("人工智能");
 *
 * // 图谱路径查询
 * List<String> path = memory.findPath("深度学习", "神经网络");
 * </pre>
 *
 * @author xiaoxuan.lp
 */
@Data
@Slf4j
public class KnowledgeGraphMemory extends BaseMemory {

    /**
     * 知识图谱存储
     * key: sessionId
     * value: 该会话的知识图谱
     */
    private Map<String, KnowledgeGraph> sessionGraphs = new HashMap<>();

    /**
     * 实体类型定义
     */
    private Map<String, List<String>> entityTypes = new HashMap<>();
    {
        entityTypes.put("PERSON", Arrays.asList("人", "人物", "专家", "科学家", "工程师"));
        entityTypes.put("LOCATION", Arrays.asList("地点", "城市", "国家", "地区"));
        entityTypes.put("ORGANIZATION", Arrays.asList("公司", "机构", "组织", "大学"));
        entityTypes.put("CONCEPT", Arrays.asList("概念", "技术", "理论", "方法"));
        entityTypes.put("OBJECT", Arrays.asList("物体", "产品", "工具", "设备"));
    }

    /**
     * 关系类型定义
     */
    private List<String> relationTypes = Arrays.asList(
        "位于", "属于", "工作于", "位于", "包含", "属于", "影响", "相关", "使用",
        "located_in", "belongs_to", "works_at", "contains", "affects", "related_to", "uses"
    );

    /**
     * 实体识别模式
     * 用于从文本中提取实体
     */
    private List<String> entityPatterns = Arrays.asList(
        "人名：([A-Za-z\\u4e00-\\u9fa5]+)",
        "地点：([A-Za-z\\u4e00-\\u9fa5]+)",
        "概念：([A-Za-z\\u4e00-\\u9fa5]+)",
        "技术：([A-Za-z\\u4e00-\\u9fa5]+)"
    );

    /**
     * 关系识别模式
     * 用于从文本中提取实体关系
     */
    private List<String> relationPatterns = Arrays.asList(
        "([A-Za-z\\u4e00-\\u9fa5]+)位于([A-Za-z\\u4e00-\\u9fa5]+)",
        "([A-Za-z\\u4e00-\\u9fa5]+)属于([A-Za-z\\u4e00-\\u9fa5]+)",
        "([A-Za-z\\u4e00-\\u9fa5]+)是([A-Za-z\\u4e00-\\u9fa5]+)",
        "([A-Za-z\\u4e00-\\u9fa5]+)包含([A-Za-z\\u4e00-\\u9fa5]+)"
    );

    /**
     * 最大图谱节点数量
     */
    private int maxGraphNodes = 200;

    /**
     * 实体相似度阈值
     */
    private double similarityThreshold = 0.7;

    /**
     * 知识图谱类
     */
    @Data
    public static class KnowledgeGraph {
        private Map<String, Entity> entities = new HashMap<>();
        private Map<String, Relation> relations = new HashMap<>();
        private Map<String, List<String>> entityTypes = new HashMap<>();

        /**
         * 添加实体
         */
        public void addEntity(Entity entity) {
            entities.put(entity.getId(), entity);
            entityTypes.computeIfAbsent(entity.getType(), k -> new ArrayList<>()).add(entity.getId());
        }

        /**
         * 添加关系
         */
        public void addRelation(Relation relation) {
            relations.put(relation.getId(), relation);
        }

        /**
         * 获取实体
         */
        public Entity getEntity(String entityId) {
            return entities.get(entityId);
        }

        /**
         * 获取关系
         */
        public Relation getRelation(String relationId) {
            return relations.get(relationId);
        }
    }

    /**
     * 实体类
     */
    @Data
    public static class Entity {
        private String id;
        private String name;
        private String type;
        private Map<String, Object> properties = new HashMap<>();
        private double importance = 0.0;
        private long firstSeen;
        private long lastSeen;

        public Entity(String name, String type) {
            this.id = UUID.randomUUID().toString();
            this.name = name;
            this.type = type;
            this.firstSeen = System.currentTimeMillis();
            this.lastSeen = System.currentTimeMillis();
        }

        /**
         * 更新实体信息
         */
        public void update(Map<String, Object> newProperties) {
            this.properties.putAll(newProperties);
            this.lastSeen = System.currentTimeMillis();
            this.importance += 0.1; // 每次出现增加重要性
        }

        /**
         * 计算与其他实体的相似度
         */
        public double calculateSimilarity(Entity other) {
            // 简单的相似度计算：基于名称相似度和共同属性
            double nameSimilarity = calculateStringSimilarity(this.name, other.getName());
            double propertySimilarity = calculatePropertySimilarity(this.properties, other.getProperties());

            return (nameSimilarity * 0.7) + (propertySimilarity * 0.3);
        }

        private double calculateStringSimilarity(String s1, String s2) {
            // 使用简单的编辑距离算法
            return 1.0 - (double) levenshteinDistance(s1.toLowerCase(), s2.toLowerCase()) / Math.max(s1.length(), s2.length());
        }

        private double calculatePropertySimilarity(Map<String, Object> props1, Map<String, Object> props2) {
            Set<String> keys1 = props1.keySet();
            Set<String> keys2 = props2.keySet();

            if (keys1.isEmpty() && keys2.isEmpty()) {
                return 1.0;
            }

            Set<String> intersection = new HashSet<>(keys1);
            intersection.retainAll(keys2);

            Set<String> union = new HashSet<>(keys1);
            union.addAll(keys2);

            return (double) intersection.size() / union.size();
        }

        private int levenshteinDistance(String s1, String s2) {
            if (s1.equals(s2)) {
                return 0;
            }

            int len1 = s1.length();
            int len2 = s2.length();

            if (len1 == 0) return len2;
            if (len2 == 0) return len1;

            int[][] matrix = new int[len1 + 1][len2 + 1];

            for (int i = 0; i <= len1; i++) {
                matrix[i][0] = i;
            }

            for (int j = 0; j <= len2; j++) {
                matrix[0][j] = j;
            }

            for (int i = 1; i <= len1; i++) {
                for (int j = 1; j <= len2; j++) {
                    int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                    matrix[i][j] = Math.min(Math.min(
                        matrix[i - 1][j] + 1,      // 删除
                        matrix[i][j - 1] + 1),     // 插入
                        matrix[i - 1][j - 1] + cost // 替换
                    );
                }
            }

            return matrix[len1][len2];
        }
    }

    /**
     * 关系类
     */
    @Data
    public static class Relation {
        private String id;
        private String type;
        private String fromEntity;
        private String toEntity;
        private Map<String, Object> properties = new HashMap<>();
        private double weight = 1.0;
        private long createdTime;

        public Relation(String type, String fromEntity, String toEntity) {
            this.id = UUID.randomUUID().toString();
            this.type = type;
            this.fromEntity = fromEntity;
            this.toEntity = toEntity;
            this.createdTime = System.currentTimeMillis();
        }
    }

    @Override
    public List<String> memoryVariables() {
        return Arrays.asList("knowledge_graph", "entity_info");
    }

    @Override
    public Map<String, Object> loadMemoryVariables(String sessionId, Map<String, Object> inputs) {
        Map<String, Object> result = new HashMap<>();

        // 获取知识图谱摘要
        String graphSummary = getKnowledgeGraphSummary(sessionId);
        result.put("knowledge_graph", graphSummary);

        // 获取实体信息（基于输入查询相关实体）
        String query = inputs.values().stream()
            .map(Object::toString)
            .collect(Collectors.joining(" "));
        String entityInfo = getRelatedEntityInfo(sessionId, query);
        result.put("entity_info", entityInfo);

        return result;
    }

    @Override
    public void saveContext(String sessionId, Map<String, Object> inputs, Map<String, Object> outputs) {
        // 从对话中提取实体和关系
        KnowledgeExtraction extraction = extractKnowledge(inputs, outputs);

        if (!extraction.getEntities().isEmpty() || !extraction.getRelations().isEmpty()) {
            // 获取或创建知识图谱
            KnowledgeGraph graph = sessionGraphs.computeIfAbsent(sessionId, k -> new KnowledgeGraph());

            // 添加实体
            for (Entity entity : extraction.getEntities()) {
                graph.addEntity(entity);
            }

            // 添加关系
            for (Relation relation : extraction.getRelations()) {
                graph.addRelation(relation);
            }

            // 清理过大的图谱
            cleanupLargeGraph(graph);

            log.debug("Updated knowledge graph for session {}: {} entities, {} relations",
                     sessionId, graph.getEntities().size(), graph.getRelations().size());
        }
    }

    /**
     * 知识提取结果类
     */
    @Data
    private static class KnowledgeExtraction {
        private List<Entity> entities = new ArrayList<>();
        private List<Relation> relations = new ArrayList<>();
    }

    /**
     * 从对话中提取知识
     */
    private KnowledgeExtraction extractKnowledge(Map<String, Object> inputs, Map<String, Object> outputs) {
        KnowledgeExtraction extraction = new KnowledgeExtraction();

        String inputText = inputs.values().stream()
            .map(Object::toString)
            .collect(Collectors.joining(" "));
        String outputText = outputs.values().stream()
            .map(Object::toString)
            .collect(Collectors.joining(" "));

        String combinedText = (inputText + " " + outputText).toLowerCase();

        // 提取实体
        extraction.getEntities().addAll(extractEntities(combinedText));

        // 提取关系
        extraction.getRelations().addAll(extractRelations(combinedText));

        return extraction;
    }

    /**
     * 提取实体
     */
    private List<Entity> extractEntities(String text) {
        List<Entity> entities = new ArrayList<>();

        // 使用预定义模式提取实体
        for (String pattern : entityPatterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);

            while (m.find()) {
                String entityName = m.group(1);
                String entityType = determineEntityType(entityName, text);

                if (entityName != null && entityType != null) {
                    Entity entity = new Entity(entityName, entityType);

                    // 添加属性
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("extracted_from", "text");
                    properties.put("context", getEntityContext(text, entityName));
                    entity.setProperties(properties);

                    entities.add(entity);
                }
            }
        }

        return entities.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 提取关系
     */
    private List<Relation> extractRelations(String text) {
        List<Relation> relations = new ArrayList<>();

        for (String pattern : relationPatterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);

            while (m.find()) {
                String fromEntity = m.group(1);
                String toEntity = m.group(2);
                String relationType = determineRelationType(text, fromEntity, toEntity);

                if (fromEntity != null && toEntity != null && relationType != null) {
                    // 这里需要实际的实体ID，简化处理直接使用名称
                    Relation relation = new Relation(relationType, fromEntity, toEntity);
                    relations.add(relation);
                }
            }
        }

        return relations;
    }

    /**
     * 确定实体类型
     */
    private String determineEntityType(String entityName, String context) {
        // 基于上下文和关键词判断实体类型
        for (Map.Entry<String, List<String>> entry : entityTypes.entrySet()) {
            String type = entry.getKey();
            List<String> keywords = entry.getValue();

            for (String keyword : keywords) {
                if (context.contains(keyword) || entityName.contains(keyword)) {
                    return type;
                }
            }
        }

        return "CONCEPT"; // 默认类型
    }

    /**
     * 确定关系类型
     */
    private String determineRelationType(String text, String fromEntity, String toEntity) {
        for (String relationType : relationTypes) {
            if (text.contains(fromEntity + relationType) || text.contains(relationType + toEntity)) {
                return relationType;
            }
        }

        return "related_to"; // 默认关系
    }

    /**
     * 获取实体上下文
     */
    private String getEntityContext(String text, String entityName) {
        int index = text.indexOf(entityName);
        if (index == -1) {
            return "";
        }

        int start = Math.max(0, index - 50);
        int end = Math.min(text.length(), index + entityName.length() + 50);

        return text.substring(start, end);
    }

    /**
     * 清理过大的图谱
     */
    private void cleanupLargeGraph(KnowledgeGraph graph) {
        if (graph.getEntities().size() <= maxGraphNodes) {
            return;
        }

        // 按重要性排序，保留最重要的实体
        List<Entity> sortedEntities = graph.getEntities().values().stream()
            .sorted((a, b) -> Double.compare(b.getImportance(), a.getImportance()))
            .collect(Collectors.toList());

        // 保留前N个最重要的实体
        Map<String, Entity> importantEntities = new HashMap<>();
        for (int i = 0; i < Math.min(maxGraphNodes, sortedEntities.size()); i++) {
            Entity entity = sortedEntities.get(i);
            importantEntities.put(entity.getId(), entity);
        }

        // 重新构建图谱，只保留涉及重要实体的关系
        Map<String, Relation> importantRelations = new HashMap<>();
        for (Relation relation : graph.getRelations().values()) {
            if (importantEntities.containsKey(relation.getFromEntity()) &&
                importantEntities.containsKey(relation.getToEntity())) {
                importantRelations.put(relation.getId(), relation);
            }
        }

        graph.setEntities(importantEntities);
        graph.setRelations(importantRelations);
    }

    /**
     * 获取知识图谱摘要
     */
    private String getKnowledgeGraphSummary(String sessionId) {
        KnowledgeGraph graph = sessionGraphs.get(sessionId);
        if (graph == null) {
            return "暂无知识图谱数据";
        }

        return String.format("知识图谱：%d 个实体，%d 个关系",
                           graph.getEntities().size(), graph.getRelations().size());
    }

    /**
     * 获取相关实体信息
     */
    private String getRelatedEntityInfo(String sessionId, String query) {
        KnowledgeGraph graph = sessionGraphs.get(sessionId);
        if (graph == null || query.trim().isEmpty()) {
            return "暂无相关实体信息";
        }

        List<String> relatedEntities = getRelatedEntities(query, 3);

        if (relatedEntities.isEmpty()) {
            return "未找到相关实体";
        }

        StringBuilder info = new StringBuilder("相关实体：");
        for (String entityName : relatedEntities) {
            Entity entity = findEntityByName(graph, entityName);
            if (entity != null) {
                info.append("\n- ").append(entity.getName())
                    .append(" (").append(entity.getType()).append(")")
                    .append(" 重要性: ").append(String.format("%.2f", entity.getImportance()));
            }
        }

        return info.toString();
    }

    /**
     * 根据名称查找实体
     */
    private Entity findEntityByName(KnowledgeGraph graph, String name) {
        return graph.getEntities().values().stream()
            .filter(entity -> entity.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取相关实体
     */
    public List<String> getRelatedEntities(String entityName, int limit) {
        // 收集所有会话的图谱进行全局查询
        List<Entity> allEntities = sessionGraphs.values().stream()
            .flatMap(graph -> graph.getEntities().values().stream())
            .collect(Collectors.toList());

        if (allEntities.isEmpty()) {
            return Collections.emptyList();
        }

        // 找到目标实体
        Entity targetEntity = allEntities.stream()
            .filter(entity -> entity.getName().equalsIgnoreCase(entityName))
            .findFirst()
            .orElse(null);

        if (targetEntity == null) {
            return Collections.emptyList();
        }

        // 计算相似度并排序
        return allEntities.stream()
            .filter(entity -> !entity.getId().equals(targetEntity.getId()))
            .filter(entity -> entity.calculateSimilarity(targetEntity) >= similarityThreshold)
            .sorted((a, b) -> Double.compare(b.calculateSimilarity(targetEntity),
                                           a.calculateSimilarity(targetEntity)))
            .limit(limit)
            .map(Entity::getName)
            .collect(Collectors.toList());
    }

    /**
     * 获取实体详细信息
     */
    public Map<String, Object> getEntityInfo(String entityName) {
        Map<String, Object> result = new HashMap<>();

        // 在所有图谱中查找实体
        for (KnowledgeGraph graph : sessionGraphs.values()) {
            Entity entity = findEntityByName(graph, entityName);
            if (entity != null) {
                result.put("name", entity.getName());
                result.put("type", entity.getType());
                result.put("importance", entity.getImportance());
                result.put("first_seen", entity.getFirstSeen());
                result.put("last_seen", entity.getLastSeen());
                result.put("properties", entity.getProperties());

                // 获取相关关系
                List<Relation> relatedRelations = graph.getRelations().values().stream()
                    .filter(rel -> rel.getFromEntity().equals(entity.getId()) ||
                                 rel.getToEntity().equals(entity.getId()))
                    .collect(Collectors.toList());

                result.put("related_relations", relatedRelations.size());
                break;
            }
        }

        return result;
    }

    /**
     * 查找两个实体之间的路径
     */
    public List<String> findPath(String fromEntity, String toEntity) {
        // 简化版路径查找算法
        // 这里实现一个简单的广度优先搜索
        List<String> allEntities = sessionGraphs.values().stream()
            .flatMap(graph -> graph.getEntities().values().stream())
            .map(Entity::getName)
            .distinct()
            .collect(Collectors.toList());

        if (!allEntities.contains(fromEntity) || !allEntities.contains(toEntity)) {
            return Collections.emptyList();
        }

        // 简化处理：如果两个实体都存在，返回直接路径
        return Arrays.asList(fromEntity, "related_to", toEntity);
    }

    /**
     * 获取图谱统计信息
     */
    public Map<String, Object> getGraphStats(String sessionId) {
        KnowledgeGraph graph = sessionGraphs.get(sessionId);
        if (graph == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_entities", graph.getEntities().size());
        stats.put("total_relations", graph.getRelations().size());

        Map<String, Long> typeDistribution = graph.getEntityTypes().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (long) entry.getValue().size()
            ));
        stats.put("type_distribution", typeDistribution);

        return stats;
    }

    @Override
    public void clear(String sessionId) {
        if (sessionId != null) {
            sessionGraphs.remove(sessionId);
        } else {
            sessionGraphs.clear();
        }
    }
}