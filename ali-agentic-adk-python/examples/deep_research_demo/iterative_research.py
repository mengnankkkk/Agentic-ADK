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
迭代式深度研究Agent - 使用LoopAgent实现

这个示例展示了如何使用Google ADK的LoopAgent来实现：
- 自动迭代搜索
- 质量评估与决策
- 智能终止条件
"""

import asyncio
import logging
from typing import Dict, Any, List
from datetime import datetime

from google.adk.agents import LlmAgent, LoopAgent, SequentialAgent
from google.adk.tools import FunctionTool
from google.adk.sessions import InMemorySessionService
from google.adk.runners import Runner
from google.genai import types

from ali_agentic_adk_python.config import get_runtime_settings
from ali_agentic_adk_python.core.model.dashscope_llm import DashscopeLLM

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 研究状态
class IterativeResearchState:
    def __init__(self):
        self.iterations: List[Dict[str, Any]] = []
        self.current_iteration = 0
        self.quality_threshold = 0.8
        self.max_iterations = 5
        
    def add_iteration(self, query: str, result: str, quality: float):
        self.current_iteration += 1
        self.iterations.append({
            "iteration": self.current_iteration,
            "query": query,
            "result": result,
            "quality": quality,
            "timestamp": datetime.now().isoformat()
        })
        
    def should_continue(self) -> bool:
        """决定是否继续迭代"""
        if self.current_iteration >= self.max_iterations:
            logger.info(f"达到最大迭代次数: {self.max_iterations}")
            return False
            
        if not self.iterations:
            return True
            
        # 检查最近3次的平均质量
        recent = self.iterations[-3:]
        avg_quality = sum(it['quality'] for it in recent) / len(recent)
        
        if avg_quality >= self.quality_threshold:
            logger.info(f"质量达标: {avg_quality:.2f} >= {self.quality_threshold}")
            return False
            
        logger.info(f"继续迭代: 当前质量 {avg_quality:.2f}")
        return True
        
    def get_summary(self) -> str:
        """获取研究摘要"""
        if not self.iterations:
            return "尚无研究结果"
            
        summary = f"完成{self.current_iteration}轮迭代研究\n\n"
        
        for it in self.iterations:
            summary += f"## 迭代 {it['iteration']}\n"
            summary += f"- 查询: {it['query']}\n"
            summary += f"- 质量: {it['quality']:.2f}\n"
            summary += f"- 结果摘要: {it['result'][:200]}...\n\n"
            
        return summary


# 全局状态
state = IterativeResearchState()


# ========== 工具函数 ==========

async def iterative_search(context: str) -> Dict[str, Any]:
    """
    执行单次迭代搜索
    
    Args:
        context: 当前研究上下文
        
    Returns:
        搜索结果
    """
    query = f"迭代{state.current_iteration + 1}: {context}"
    
    # 模拟搜索
    result = f"这是第{state.current_iteration + 1}次搜索的结果，提供了关于'{context}'的详细信息。"
    
    # 模拟质量评分（逐渐提升）
    quality = min(0.3 + (state.current_iteration * 0.15), 1.0)
    
    state.add_iteration(query, result, quality)
    
    logger.info(f"迭代 {state.current_iteration}: 质量={quality:.2f}")
    
    return {
        "query": query,
        "result": result,
        "quality": quality,
        "iteration": state.current_iteration,
        "should_continue": state.should_continue()
    }


async def quality_check(search_result: Dict[str, Any]) -> Dict[str, Any]:
    """
    质量检查
    
    Args:
        search_result: 搜索结果
        
    Returns:
        质量评估结果
    """
    quality = search_result.get("quality", 0.0)
    
    assessment = {
        "quality": quality,
        "is_satisfactory": quality >= state.quality_threshold,
        "needs_refinement": quality < state.quality_threshold,
        "recommendation": "继续搜索" if quality < state.quality_threshold else "可以结束"
    }
    
    logger.info(f"质量评估: {assessment['recommendation']}")
    
    return assessment


async def generate_final_report() -> Dict[str, Any]:
    """生成最终研究报告"""
    report = f"""
# 迭代式深度研究报告

{state.get_summary()}

## 结论
- 总迭代次数: {state.current_iteration}
- 研究状态: {'质量达标' if state.iterations and state.iterations[-1]['quality'] >= state.quality_threshold else '需要进一步研究'}
- 建议: 基于当前结果，可以进行下一步分析

生成时间: {datetime.now().isoformat()}
"""
    
    logger.info("最终报告已生成")
    
    return {
        "report": report,
        "iterations": state.current_iteration,
        "success": state.iterations and state.iterations[-1]['quality'] >= state.quality_threshold
    }


# ========== Agent系统构建 ==========

def create_loop_research_agent() -> LoopAgent:
    """
    创建使用LoopAgent的研究系统
    
    Returns:
        配置好的LoopAgent
    """
    runtime_settings = get_runtime_settings()
    dashscope_settings = runtime_settings.dashscope()
    
    if not dashscope_settings:
        raise RuntimeError("需要配置DashScope")
    
    model = DashscopeLLM.from_settings(dashscope_settings)
    
    # 创建搜索Agent
    search_agent = LlmAgent(
        name="searchAgent",
        model=model,
        instruction="""你是一个搜索专家。根据当前研究进展，使用iterative_search工具执行搜索。
每次搜索都要针对性地深入某个方面，不要重复之前的查询。""",
        description="执行迭代搜索",
        tools=[FunctionTool(name="iterative_search", description="执行搜索", func=iterative_search)]
    )
    
    # 创建评估Agent
    eval_agent = LlmAgent(
        name="evaluatorAgent",
        model=model,
        instruction="""你是质量评估专家。使用quality_check工具评估搜索结果。
根据评估结果决定是否需要继续搜索。""",
        description="评估搜索质量",
        tools=[FunctionTool(name="quality_check", description="检查质量", func=quality_check)]
    )
    
    # 组合成顺序Agent
    research_cycle = SequentialAgent(
        name="researchCycle",
        agents=[search_agent, eval_agent],
        description="单轮研究循环"
    )
    
    # 创建循环Agent
    loop_agent = LoopAgent(
        name="loopResearchAgent",
        agent=research_cycle,
        max_iterations=state.max_iterations,
        stop_condition=lambda: not state.should_continue(),
        description="迭代式研究Agent"
    )
    
    return loop_agent


async def run_iterative_research(topic: str) -> str:
    """
    运行迭代式研究
    
    Args:
        topic: 研究主题
        
    Returns:
        研究结果
    """
    logger.info(f"========== 开始迭代式研究 ==========")
    logger.info(f"主题: {topic}")
    
    # 重置状态
    global state
    state = IterativeResearchState()
    
    # 创建Agent（注意：LoopAgent可能需要特殊配置，这里展示概念）
    # 由于Google ADK的LoopAgent API可能不同，我们用LlmAgent模拟
    
    runtime_settings = get_runtime_settings()
    dashscope_settings = runtime_settings.dashscope()
    model = DashscopeLLM.from_settings(dashscope_settings)
    
    # 创建主Agent
    main_agent = LlmAgent(
        name="iterativeResearcher",
        model=model,
        instruction=f"""你是一个迭代式研究专家。对主题"{topic}"进行深度研究。

研究流程：
1. 使用iterative_search工具进行搜索
2. 使用quality_check工具评估结果质量
3. 如果质量不足，调整策略继续搜索
4. 重复以上步骤，直到质量达标或达到最大迭代次数
5. 使用generate_final_report生成最终报告

请开始研究，并根据质量反馈不断优化搜索策略。""",
        description="迭代式研究Agent",
        tools=[
            FunctionTool(name="iterative_search", func=iterative_search),
            FunctionTool(name="quality_check", func=quality_check),
            FunctionTool(name="generate_final_report", func=generate_final_report)
        ]
    )
    
    # 设置Session
    session_service = InMemorySessionService()
    session_id = f"iter_research_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    await session_service.create_session(
        app_name="iterative_research",
        user_id="researcher",
        session_id=session_id
    )
    
    # 创建Runner
    runner = Runner(
        agent=main_agent,
        app_name="iterative_research",
        session_service=session_service
    )
    
    # 运行
    user_message = types.Content(
        role='user',
        parts=[types.Part(text=f"请对'{topic}'进行迭代式深度研究，直到获得高质量结果。")]
    )
    
    final_response = ""
    
    try:
        events = runner.run_async(
            user_id="researcher",
            session_id=session_id,
            new_message=user_message
        )
        
        async for event in events:
            if event.is_final_response() and event.content and event.content.parts:
                final_response = event.content.parts[0].text
                
    except Exception as e:
        logger.error(f"研究失败: {e}", exc_info=True)
        final_response = f"错误: {str(e)}"
    
    logger.info(f"========== 研究完成 ==========")
    
    return final_response


async def main():
    """主函数"""
    topic = "边缘计算在物联网中的应用"
    
    try:
        result = await run_iterative_research(topic)
        
        print("\n" + "="*60)
        print("Agent响应")
        print("="*60)
        print(result)
        print("="*60)
        
        # 生成详细报告
        report_data = await generate_final_report()
        print("\n" + "="*60)
        print("详细研究报告")
        print("="*60)
        print(report_data['report'])
        print("="*60)
        
    except Exception as e:
        logger.error(f"执行失败: {e}", exc_info=True)


if __name__ == "__main__":
    asyncio.run(main())

