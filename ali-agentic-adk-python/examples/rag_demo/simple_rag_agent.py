# Copyright (C) 2025 AIDC-AI
# This project incorporates components from the Open Source Software below.
# The original copyright notices and the licenses under which we received such components are set forth below for informational purposes.
#
# Open Source Software Licensed under the MIT License:
# --------------------------------------------------------------------
# 1. vscode-extension-updater-gitlab 3.0.1 https://www.npmjs.com/package/vscode-extension-updater-gitlab
# Copyright (c) Microsoft Corporation. All rights reserved.
# Copyright (c) 2015 David Owens II
# Copyright (c) Microsoft Corporation.
# Terms of the MIT:
# --------------------------------------------------------------------
# MIT License
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.


"""
简单RAG Agent实现

功能：
- 文档管理
- 向量检索
- 上下文增强的问答
- 知识库更新
"""

import asyncio
import logging
from typing import Dict, Any, List, Optional
from datetime import datetime
from dataclasses import dataclass, field
import json

from google.adk.agents import LlmAgent
from google.adk.tools import FunctionTool
from google.adk.sessions import InMemorySessionService
from google.adk.runners import Runner
from google.genai import types

from ali_agentic_adk_python.config import get_runtime_settings
from ali_agentic_adk_python.core.model.dashscope_llm import DashscopeLLM
from ali_agentic_adk_python.core.embedding.dashscope_embedding import DashscopeEmbedding

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class Document:
    """文档"""
    id: str
    content: str
    metadata: Dict[str, Any] = field(default_factory=dict)
    embedding: Optional[List[float]] = None
    created_at: str = field(default_factory=lambda: datetime.now().isoformat())


class VectorStore:
    """向量存储（简化版）"""
    
    def __init__(self):
        self.documents: List[Document] = []
        self.embedder = None
        
    def add_document(self, doc: Document):
        """添加文档"""
        self.documents.append(doc)
        logger.info(f"文档已添加: {doc.id}")
    
    def search(self, query_embedding: List[float], top_k: int = 3) -> List[Document]:
        """搜索相似文档（简化实现）"""
        # 简化版：随机返回top_k个文档
        # 实际应计算余弦相似度
        results = self.documents[:min(top_k, len(self.documents))]
        logger.info(f"检索到 {len(results)} 个相关文档")
        return results
    
    def get_stats(self) -> Dict[str, Any]:
        """获取统计信息"""
        return {
            "total_documents": len(self.documents),
            "indexed_documents": sum(1 for d in self.documents if d.embedding is not None)
        }


# 全局知识库
knowledge_base = VectorStore()


# ========== 工具函数 ==========

async def add_knowledge(content: str, source: str = "manual", category: str = "general") -> Dict[str, Any]:
    """
    添加知识到知识库
    
    Args:
        content: 知识内容
        source: 来源
        category: 分类
        
    Returns:
        添加结果
    """
    logger.info(f"添加知识: {content[:50]}...")
    
    doc_id = f"DOC_{datetime.now().strftime('%Y%m%d%H%M%S')}_{len(knowledge_base.documents)}"
    
    doc = Document(
        id=doc_id,
        content=content,
        metadata={
            "source": source,
            "category": category,
            "length": len(content)
        }
    )
    
    # 实际应用中这里应该生成embedding
    # doc.embedding = await embedder.embed(content)
    
    knowledge_base.add_document(doc)
    
    return {
        "success": True,
        "document_id": doc_id,
        "content_length": len(content),
        "message": f"知识已添加到知识库，ID: {doc_id}"
    }


async def search_knowledge(query: str, top_k: int = 3) -> Dict[str, Any]:
    """
    检索相关知识
    
    Args:
        query: 查询文本
        top_k: 返回top K个结果
        
    Returns:
        检索结果
    """
    logger.info(f"检索知识: {query}")
    
    if not knowledge_base.documents:
        return {
            "success": False,
            "results": [],
            "message": "知识库为空，请先添加知识"
        }
    
    # 实际应用中这里应该使用embedding进行相似度搜索
    # query_embedding = await embedder.embed(query)
    # results = knowledge_base.search(query_embedding, top_k)
    
    # 简化版：返回前top_k个文档
    results = knowledge_base.documents[:min(top_k, len(knowledge_base.documents))]
    
    return {
        "success": True,
        "query": query,
        "results": [
            {
                "id": doc.id,
                "content": doc.content,
                "source": doc.metadata.get("source", "unknown"),
                "category": doc.metadata.get("category", "general")
            }
            for doc in results
        ],
        "count": len(results),
        "message": f"找到 {len(results)} 个相关文档"
    }


async def rag_answer(question: str) -> Dict[str, Any]:
    """
    使用RAG回答问题
    
    Args:
        question: 问题
        
    Returns:
        答案和相关上下文
    """
    logger.info(f"RAG问答: {question}")
    
    # 1. 检索相关知识
    search_result = await search_knowledge(question, top_k=3)
    
    if not search_result['success']:
        return {
            "success": False,
            "question": question,
            "answer": "抱歉，知识库中没有相关信息。",
            "context": []
        }
    
    # 2. 构建上下文
    context_docs = search_result['results']
    context_text = "\n\n".join([
        f"[文档{i+1}] {doc['content']}"
        for i, doc in enumerate(context_docs)
    ])
    
    # 3. 生成答案（这里简化，实际应调用LLM）
    answer = f"""基于知识库中的信息，{question}

相关内容摘要：
{context_text[:200]}...

（此处为模拟答案，实际应使用LLM基于上下文生成）"""
    
    return {
        "success": True,
        "question": question,
        "answer": answer,
        "context": context_docs,
        "context_count": len(context_docs),
        "message": f"已基于{len(context_docs)}个文档生成答案"
    }


async def batch_add_knowledge(documents: List[Dict[str, str]]) -> Dict[str, Any]:
    """
    批量添加知识
    
    Args:
        documents: 文档列表，每个文档包含content, source, category
        
    Returns:
        批量添加结果
    """
    logger.info(f"批量添加 {len(documents)} 个文档")
    
    success_count = 0
    failed_count = 0
    doc_ids = []
    
    for doc_data in documents:
        try:
            result = await add_knowledge(
                content=doc_data.get("content", ""),
                source=doc_data.get("source", "batch"),
                category=doc_data.get("category", "general")
            )
            if result['success']:
                success_count += 1
                doc_ids.append(result['document_id'])
        except Exception as e:
            logger.error(f"添加文档失败: {e}")
            failed_count += 1
    
    return {
        "success": True,
        "total": len(documents),
        "success_count": success_count,
        "failed_count": failed_count,
        "document_ids": doc_ids,
        "message": f"批量添加完成: {success_count}/{len(documents)} 成功"
    }


async def get_knowledge_stats() -> Dict[str, Any]:
    """
    获取知识库统计信息
    
    Returns:
        统计信息
    """
    logger.info("获取知识库统计")
    
    stats = knowledge_base.get_stats()
    
    # 按类别统计
    category_stats = {}
    for doc in knowledge_base.documents:
        category = doc.metadata.get("category", "unknown")
        category_stats[category] = category_stats.get(category, 0) + 1
    
    stats['categories'] = category_stats
    stats['message'] = f"知识库包含 {stats['total_documents']} 个文档"
    
    return stats


async def update_knowledge(doc_id: str, new_content: str) -> Dict[str, Any]:
    """
    更新知识
    
    Args:
        doc_id: 文档ID
        new_content: 新内容
        
    Returns:
        更新结果
    """
    logger.info(f"更新知识: {doc_id}")
    
    for doc in knowledge_base.documents:
        if doc.id == doc_id:
            old_content = doc.content
            doc.content = new_content
            doc.metadata['updated_at'] = datetime.now().isoformat()
            doc.metadata['previous_length'] = len(old_content)
            
            # 重新生成embedding
            # doc.embedding = await embedder.embed(new_content)
            
            return {
                "success": True,
                "document_id": doc_id,
                "message": "知识已更新"
            }
    
    return {
        "success": False,
        "document_id": doc_id,
        "message": "文档不存在"
    }


# ========== Agent定义 ==========

def create_rag_agent() -> LlmAgent:
    """创建RAG Agent"""
    
    runtime_settings = get_runtime_settings()
    dashscope_settings = runtime_settings.dashscope()
    
    if not dashscope_settings:
        raise RuntimeError("需要配置DashScope")
    
    model = DashscopeLLM.from_settings(dashscope_settings)
    
    tools = [
        FunctionTool(
            name="add_knowledge",
            description="添加知识到知识库。参数：content=内容，source=来源(可选)，category=分类(可选)",
            func=add_knowledge
        ),
        FunctionTool(
            name="search_knowledge",
            description="检索相关知识。参数：query=查询文本，top_k=返回数量(默认3)",
            func=search_knowledge
        ),
        FunctionTool(
            name="rag_answer",
            description="使用RAG回答问题，自动检索相关知识并生成答案。参数：question=问题",
            func=rag_answer
        ),
        FunctionTool(
            name="batch_add_knowledge",
            description="批量添加知识。参数：documents=文档列表",
            func=batch_add_knowledge
        ),
        FunctionTool(
            name="get_knowledge_stats",
            description="获取知识库统计信息",
            func=get_knowledge_stats
        ),
        FunctionTool(
            name="update_knowledge",
            description="更新已有知识。参数：doc_id=文档ID，new_content=新内容",
            func=update_knowledge
        ),
    ]
    
    agent = LlmAgent(
        name="ragAgent",
        model=model,
        instruction="""你是一个RAG（检索增强生成）智能助手。你可以：

1. **知识管理**
   - 使用 add_knowledge 添加新知识
   - 使用 batch_add_knowledge 批量添加
   - 使用 update_knowledge 更新知识
   - 使用 get_knowledge_stats 查看统计

2. **知识检索**
   - 使用 search_knowledge 检索相关内容
   - 自动找到最相关的top K个文档

3. **智能问答**
   - 使用 rag_answer 回答问题
   - 自动检索相关知识作为上下文
   - 基于检索到的知识生成准确答案

工作流程：
1. 理解用户需求
2. 如果是问题，使用rag_answer获取答案
3. 如果需要添加知识，使用add_knowledge或batch_add_knowledge
4. 如果需要查找信息，使用search_knowledge
5. 提供准确、有依据的回答

请确保：
- 基于检索到的知识回答
- 说明信息来源
- 如果知识库中没有相关信息，如实告知""",
        description="RAG智能问答Agent",
        tools=tools
    )
    
    return agent


# ========== 主程序 ==========

async def setup_demo_knowledge_base():
    """设置演示知识库"""
    demo_docs = [
        {
            "content": "Python是一种高级编程语言，以其简洁的语法和强大的功能而闻名。它广泛应用于Web开发、数据科学、人工智能等领域。",
            "source": "编程基础",
            "category": "programming"
        },
        {
            "content": "机器学习是人工智能的一个分支，它使计算机能够从数据中学习并做出决策，而无需明确编程。常见算法包括线性回归、决策树、神经网络等。",
            "source": "AI基础",
            "category": "ai"
        },
        {
            "content": "RESTful API是一种软件架构风格，用于设计网络应用程序。它使用HTTP请求来访问和操作数据，主要操作包括GET、POST、PUT、DELETE等。",
            "source": "Web开发",
            "category": "web"
        },
        {
            "content": "Docker是一个开源的容器化平台，它允许开发者将应用及其依赖打包到一个可移植的容器中。这提高了应用的部署效率和一致性。",
            "source": "DevOps",
            "category": "devops"
        },
        {
            "content": "大语言模型(LLM)如GPT、BERT等，是基于Transformer架构的深度学习模型。它们在自然语言理解和生成任务中表现出色。",
            "source": "AI前沿",
            "category": "ai"
        }
    ]
    
    await batch_add_knowledge(demo_docs)
    logger.info(f"演示知识库已设置，包含 {len(demo_docs)} 个文档")


async def main():
    """主函数 - 演示RAG功能"""
    
    print(f"\n{'='*60}")
    print("RAG (检索增强生成) 示例")
    print(f"{'='*60}\n")
    
    # 重置知识库
    global knowledge_base
    knowledge_base = VectorStore()
    
    # 设置演示知识库
    print("[1] 设置知识库...")
    await setup_demo_knowledge_base()
    
    # 获取统计信息
    print("\n[2] 知识库统计:")
    stats = await get_knowledge_stats()
    print(f"  - 总文档数: {stats['total_documents']}")
    print(f"  - 分类统计: {json.dumps(stats['categories'], ensure_ascii=False)}")
    
    # 测试检索
    print("\n[3] 测试知识检索:")
    query = "什么是Python"
    search_result = await search_knowledge(query, top_k=2)
    print(f"  查询: {query}")
    print(f"  结果: 找到 {search_result['count']} 个相关文档")
    for i, doc in enumerate(search_result['results'], 1):
        print(f"    {i}. [{doc['category']}] {doc['content'][:50]}...")
    
    # 测试RAG问答
    print("\n[4] 测试RAG问答:")
    questions = [
        "Python有什么特点？",
        "什么是机器学习？",
        "如何使用Docker？"
    ]
    
    for question in questions:
        print(f"\n  问题: {question}")
        answer_result = await rag_answer(question)
        if answer_result['success']:
            print(f"  答案: {answer_result['answer'][:100]}...")
            print(f"  参考文档: {answer_result['context_count']} 个")
    
    # 添加新知识
    print("\n[5] 添加新知识:")
    new_knowledge = "Kubernetes（K8s）是一个开源的容器编排平台，用于自动化应用容器的部署、扩展和管理。"
    add_result = await add_knowledge(new_knowledge, source="DevOps", category="devops")
    print(f"  {add_result['message']}")
    
    # 最终统计
    print("\n[6] 最终统计:")
    final_stats = await get_knowledge_stats()
    print(f"  知识库包含 {final_stats['total_documents']} 个文档")
    
    print(f"\n{'='*60}\n")


if __name__ == "__main__":
    asyncio.run(main())

