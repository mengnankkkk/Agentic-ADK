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
package com.alibaba.langengine.turbopuffer.vectorstore;

import com.alibaba.langengine.core.embeddings.FakeEmbeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.google.common.collect.Lists;

import java.util.List;


public class TurbopufferDemo {

    public static void main(String[] args) {
        // 创建Turbopuffer实例
        TurbopufferParam param = new TurbopufferParam();
        param.initParam.fieldEmbeddingsDimension = 1536;
        
        Turbopuffer turbopuffer = new Turbopuffer("demo_namespace", param);
        turbopuffer.setEmbedding(new FakeEmbeddings());

        try {
            // 初始化向量存储
            turbopuffer.init();

            // 准备测试文档
            List<Document> documents = Lists.newArrayList();
            
            Document doc1 = new Document();
            doc1.setPageContent("Hello world, this is a test document.");
            doc1.setUniqueId("doc1");
            documents.add(doc1);

            Document doc2 = new Document();
            doc2.setPageContent("Turbopuffer is a high-performance vector database.");
            doc2.setUniqueId("doc2");
            documents.add(doc2);

            Document doc3 = new Document();
            doc3.setPageContent("Vector search enables semantic similarity matching.");
            doc3.setUniqueId("doc3");
            documents.add(doc3);

            // 添加文档到向量存储
            System.out.println("Adding documents to Turbopuffer...");
            turbopuffer.addDocuments(documents);
            System.out.println("Documents added successfully!");

            // 执行相似度搜索
            System.out.println("\nPerforming similarity search...");
            String query = "vector database";
            List<Document> results = turbopuffer.similaritySearch(query, 2);

            System.out.println("Search results for query: '" + query + "'");
            for (int i = 0; i < results.size(); i++) {
                Document result = results.get(i);
                System.out.println((i + 1) + ". ID: " + result.getUniqueId());
                System.out.println("   Content: " + result.getPageContent());
                System.out.println("   Score: " + result.getScore());
                System.out.println();
            }

            // 演示删除功能
            System.out.println("Demonstrating document deletion...");
            turbopuffer.deleteVectors(Lists.newArrayList("doc1"));
            System.out.println("Document deleted successfully!");

        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭连接
            turbopuffer.close();
        }
    }

}
