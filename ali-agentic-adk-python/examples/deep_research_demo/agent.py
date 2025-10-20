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
深度研究Agent实现

这个模块实现了一个完整的深度研究Agent，能够：
- 分析研究主题并生成关键问题
- 执行多轮迭代搜索
- 评估搜索结果质量
- 综合信息并生成报告
"""

import asyncio
import logging
from typing import List, Dict, Any, Optional
from datetime import datetime

from google.adk.agents import LlmAgent, SequentialAgent, LoopAgent
from google.adk.tools import FunctionTool
from google.adk.sessions import InMemorySessionService
from google.adk.runners import Runner
from google.adk.agents.run_config import RunConfig, StreamingMode
from google.genai import types

from ali_agentic_adk_python.config import get_runtime_settings
from ali_agentic_adk_python.core.model.dashscope_llm import DashscopeLLM
from ali_agentic_adk_python.core.tool.dashscope_app_tool import DashscopeAppTool

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 全局配置
APP_NAME = "deep_research_agent"
USER_ID = "researcher_001"
MAX_ITERATIONS = 5


class ResearchState:
    """研究状态管理"""
    
    def __init__(self):
        self.topic: str = ""
        self.questions: List[str] = []
        self.search_results: List[Dict[str, Any]] = []
        self.insights: List[str] = []
        self.iteration_count: int = 0
        self.quality_scores: List[float] = []
    
    def add_search_result(self, query: str, result: str, quality: float):
        """添加搜索结果"""
        self.search_results.append({
            "query": query,
            "result": result,
            "quality": quality,
            "timestamp": datetime.now().isoformat()
        })
        self.quality_scores.append(quality)
    
    def get_average_quality(self) -> float:
        """获取平均质量分数"""
        if not self.quality_scores:
            return 0.0
        return sum(self.quality_scores) / len(self.quality_scores)
    
    def should_continue(self) -> bool:
        """判断是否应该继续研究"""
        if self.iteration_count >= MAX_ITERATIONS:
            return False
        if self.get_average_quality() >= 0.8 and len(self.search_results) >= 3:
            return False
        return True


# 全局研究状态
research_state = ResearchState()


# ========== 工具函数定义 ==========

async def analyze_topic(topic: str) -> Dict[str, Any]:
    """
    分析研究主题并生成关键问题
    
    Args:
        topic: 研究主题
        
    Returns:
        包含关键问题的字典
    """
    logger.info(f"分析研究主题: {topic}")
    research_state.topic = topic
    
    # 生成研究问题（这里简化处理，实际应该由LLM生成）
    questions = [
        f"{topic}的基本定义和概念是什么？",
        f"{topic}的主要特点和优势有哪些？",
        f"{topic}的实际应用场景有哪些？",
        f"{topic}目前面临的挑战和未来发展趋势如何？"
    ]
    
    research_state.questions = questions
    
    return {
        "topic": topic,
        "questions": questions,
        "question_count": len(questions)
    }


async def web_search(query: str) -> Dict[str, Any]:
    """
    执行网络搜索
    
    Args:
        query: 搜索查询
        
    Returns:
        搜索结果字典
    """
    logger.info(f"执行搜索: {query}")
    research_state.iteration_count += 1
    
    # 模拟搜索结果（实际应该调用真实的搜索API）
    mock_result = f"""
    关于"{query}"的搜索结果：
    
    1. 这是一个重要的研究领域，涉及多个方面的技术和应用。
    2. 当前的研究进展显示出积极的发展趋势。
    3. 业界和学术界都在积极探索相关的创新方向。
    4. 存在一些技术挑战需要进一步研究和解决。
    
    [模拟数据 - 第{research_state.iteration_count}次迭代]
    """
    
    # 模拟质量评分（实际应该由评估Agent完成）
    quality = 0.5 + (research_state.iteration_count * 0.1)
    if quality > 1.0:
        quality = 1.0
    
    research_state.add_search_result(query, mock_result, quality)
    
    return {
        "query": query,
        "result": mock_result,
        "quality": quality,
        "iteration": research_state.iteration_count
    }


async def evaluate_result(result: Dict[str, Any]) -> Dict[str, Any]:
    """
    评估搜索结果质量
    
    Args:
        result: 搜索结果
        
    Returns:
        评估结果
    """
    logger.info(f"评估结果质量: 迭代 {result.get('iteration', 0)}")
    
    quality = result.get("quality", 0.0)
    
    evaluation = {
        "is_satisfactory": quality >= 0.75,
        "quality_score": quality,
        "needs_more_search": quality < 0.75 and research_state.iteration_count < MAX_ITERATIONS,
        "feedback": "结果质量良好" if quality >= 0.75 else "需要更多信息"
    }
    
    logger.info(f"评估结果: {evaluation}")
    return evaluation


async def synthesize_research() -> Dict[str, Any]:
    """
    综合研究结果并生成报告
    
    Returns:
        研究报告
    """
    logger.info("综合研究结果...")
    
    report = f"""
# 深度研究报告: {research_state.topic}

## 研究概述
- 研究主题: {research_state.topic}
- 研究问题数量: {len(research_state.questions)}
- 搜索迭代次数: {research_state.iteration_count}
- 平均质量分数: {research_state.get_average_quality():.2f}

## 研究问题
"""
    
    for i, question in enumerate(research_state.questions, 1):
        report += f"\n{i}. {question}"
    
    report += "\n\n## 搜索结果汇总\n"
    
    for i, result in enumerate(research_state.search_results, 1):
        report += f"\n### 搜索 {i}: {result['query']}\n"
        report += f"质量分数: {result['quality']:.2f}\n"
        report += f"时间: {result['timestamp']}\n"
        report += f"\n{result['result']}\n"
    
    report += f"\n## 结论\n"
    report += f"本次深度研究完成了{research_state.iteration_count}轮迭代搜索，"
    report += f"平均质量分数为{research_state.get_average_quality():.2f}。"
    report += "研究结果为该主题提供了全面的信息覆盖。\n"
    
    logger.info("研究报告生成完成")
    
    return {
        "report": report,
        "topic": research_state.topic,
        "iteration_count": research_state.iteration_count,
        "average_quality": research_state.get_average_quality()
    }


# ========== Agent配置 ==========

def create_deep_research_agent() -> LlmAgent:
    """
    创建深度研究Agent系统
    
    Returns:
        配置好的深度研究Agent
    """
    runtime_settings = get_runtime_settings()
    dashscope_settings = runtime_settings.dashscope()
    
    if dashscope_settings is None:
        raise RuntimeError("DashScope配置缺失。请在环境变量中设置DASHSCOPE_API_KEY")
    
    # 创建LLM模型
    model = DashscopeLLM.from_settings(dashscope_settings)
    
    # 创建工具
    search_tool = FunctionTool(
        name="web_search",
        description="执行网络搜索以获取信息",
        func=web_search
    )
    
    evaluate_tool = FunctionTool(
        name="evaluate_result",
        description="评估搜索结果的质量",
        func=evaluate_result
    )
    
    synthesize_tool = FunctionTool(
        name="synthesize_research",
        description="综合所有研究结果并生成最终报告",
        func=synthesize_research
    )
    
    # 可选：添加DashScope工具
    dashscope_tool = None
    if dashscope_settings.app_id:
        dashscope_tool = DashscopeAppTool(
            name="dashscope_search",
            description="使用阿里百炼搜索工具"
        )
        dashscope_tool.app_id = dashscope_settings.app_id
        dashscope_tool.api_key = dashscope_settings.api_key_value
    
    # 创建主研究Agent
    tools = [search_tool, evaluate_tool, synthesize_tool]
    if dashscope_tool:
        tools.append(dashscope_tool)
    
    research_agent = LlmAgent(
        name="deepResearchAgent",
        model=model,
        instruction="""你是一个专业的深度研究助手。你的任务是：

1. 分析用户提供的研究主题
2. 生成针对性的研究问题
3. 使用web_search工具执行多轮搜索
4. 使用evaluate_result工具评估每次搜索结果的质量
5. 如果质量不足，继续搜索更多信息
6. 最后使用synthesize_research工具生成完整的研究报告

请确保：
- 搜索查询要具体且针对性强
- 评估结果要客观
- 最终报告要全面且有洞察力

开始研究时，先思考需要哪些关键问题，然后逐步搜索和评估。""",
        description="执行深度研究并生成综合报告的Agent",
        tools=tools
    )
    
    return research_agent


# ========== 主程序 ==========

async def run_deep_research(topic: str) -> str:
    """
    运行深度研究
    
    Args:
        topic: 研究主题
        
    Returns:
        研究报告文本
    """
    logger.info(f"========== 开始深度研究 ==========")
    logger.info(f"研究主题: {topic}")
    
    # 重置研究状态
    global research_state
    research_state = ResearchState()
    
    # 预分析主题
    await analyze_topic(topic)
    
    # 创建Agent
    agent = create_deep_research_agent()
    
    # 设置Session
    session_service = InMemorySessionService()
    session_id = f"research_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    session = await session_service.create_session(
        app_name=APP_NAME,
        user_id=USER_ID,
        session_id=session_id
    )
    
    # 创建Runner
    runner = Runner(
        agent=agent,
        app_name=APP_NAME,
        session_service=session_service
    )
    
    # 准备用户消息
    user_message = types.Content(
        role='user',
        parts=[types.Part(text=f"请对以下主题进行深度研究：{topic}\n\n"
                               f"需要回答的关键问题：\n" + 
                               "\n".join(f"{i+1}. {q}" for i, q in enumerate(research_state.questions)))]
    )
    
    # 运行Agent
    logger.info("启动研究Agent...")
    final_response = ""
    
    try:
        events = runner.run_async(
            user_id=USER_ID,
            session_id=session_id,
            new_message=user_message,
            run_config=RunConfig(streaming_mode=StreamingMode.NONE)
        )
        
        async for event in events:
            if event.is_final_response() and event.content and event.content.parts:
                final_response = event.content.parts[0].text
                logger.info("收到最终响应")
            elif event.tool_calls:
                logger.info(f"工具调用: {[tc.name for tc in event.tool_calls]}")
    
    except Exception as e:
        logger.error(f"研究过程中出现错误: {e}", exc_info=True)
        final_response = f"研究过程出现错误: {str(e)}"
    
    logger.info(f"========== 研究完成 ==========")
    logger.info(f"总迭代次数: {research_state.iteration_count}")
    logger.info(f"平均质量: {research_state.get_average_quality():.2f}")
    
    return final_response


async def main():
    """主函数"""
    # 测试不同的研究主题
    test_topics = [
        "人工智能在医疗领域的应用",
        "量子计算的最新进展",
        "区块链技术的实际应用场景"
    ]
    
    # 选择第一个主题进行测试
    topic = test_topics[0]
    
    try:
        result = await run_deep_research(topic)
        print("\n" + "="*60)
        print("研究结果")
        print("="*60)
        print(result)
        print("="*60)
        
        # 也输出我们自己生成的详细报告
        synthesis_result = await synthesize_research()
        print("\n" + "="*60)
        print("详细研究报告")
        print("="*60)
        print(synthesis_result['report'])
        print("="*60)
        
    except Exception as e:
        logger.error(f"程序执行失败: {e}", exc_info=True)


if __name__ == "__main__":
    asyncio.run(main())

