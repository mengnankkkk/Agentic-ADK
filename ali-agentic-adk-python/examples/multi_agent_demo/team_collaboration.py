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
团队协作Agent系统

模拟软件开发团队的协作流程：
- 产品经理Agent: 需求分析
- 架构师Agent: 系统设计
- 开发Agent: 编码实现
- 测试Agent: 质量保证
- DevOps Agent: 部署运维
"""

import asyncio
import logging
from typing import Dict, Any, List
from datetime import datetime
from dataclasses import dataclass, field

from google.adk.agents import LlmAgent, SequentialAgent
from google.adk.tools import FunctionTool
from google.adk.sessions import InMemorySessionService
from google.adk.runners import Runner
from google.genai import types

from ali_agentic_adk_python.config import get_runtime_settings
from ali_agentic_adk_python.core.model.dashscope_llm import DashscopeLLM

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class Task:
    """任务定义"""
    id: str
    title: str
    description: str
    assignee: str
    status: str = "pending"  # pending, in_progress, completed
    result: Dict[str, Any] = field(default_factory=dict)
    created_at: str = field(default_factory=lambda: datetime.now().isoformat())


@dataclass
class Project:
    """项目状态"""
    name: str
    requirements: List[str] = field(default_factory=list)
    architecture: Dict[str, Any] = field(default_factory=dict)
    code: Dict[str, str] = field(default_factory=dict)
    test_results: Dict[str, Any] = field(default_factory=dict)
    deployment: Dict[str, Any] = field(default_factory=dict)
    tasks: List[Task] = field(default_factory=list)
    
    def add_task(self, task: Task):
        """添加任务"""
        self.tasks.append(task)
        logger.info(f"[项目] 新任务: {task.title} -> {task.assignee}")
    
    def update_task_status(self, task_id: str, status: str, result: Dict[str, Any] = None):
        """更新任务状态"""
        for task in self.tasks:
            if task.id == task_id:
                task.status = status
                if result:
                    task.result = result
                logger.info(f"[项目] 任务更新: {task.title} -> {status}")
                break
    
    def get_summary(self) -> str:
        """获取项目摘要"""
        completed = sum(1 for t in self.tasks if t.status == "completed")
        total = len(self.tasks)
        
        summary = f"""
项目: {self.name}
进度: {completed}/{total} 任务完成
状态:
  - 需求: {'✓' if self.requirements else '○'}
  - 架构: {'✓' if self.architecture else '○'}
  - 开发: {'✓' if self.code else '○'}
  - 测试: {'✓' if self.test_results else '○'}
  - 部署: {'✓' if self.deployment else '○'}
"""
        return summary


# 全局项目状态
current_project = None


# ========== Agent工具函数 ==========

async def analyze_requirements(project_name: str, description: str) -> Dict[str, Any]:
    """产品经理: 分析需求"""
    logger.info(f"[产品经理] 分析项目需求: {project_name}")
    
    # 模拟需求分析
    requirements = [
        "用户认证和授权系统",
        "数据管理CRUD功能",
        "RESTful API接口",
        "Web前端界面",
        "数据库设计与实现",
        "日志和监控系统"
    ]
    
    current_project.requirements = requirements
    current_project.add_task(Task(
        id="REQ001",
        title="需求分析",
        description="分析项目需求并制定功能列表",
        assignee="产品经理",
        status="completed",
        result={"requirements": requirements}
    ))
    
    return {
        "project": project_name,
        "requirements": requirements,
        "priority": "high",
        "estimated_duration": "2周",
        "message": f"需求分析完成，共{len(requirements)}项功能需求"
    }


async def design_architecture(requirements: List[str]) -> Dict[str, Any]:
    """架构师: 设计系统架构"""
    logger.info(f"[架构师] 设计系统架构，基于{len(requirements)}项需求")
    
    architecture = {
        "frontend": {
            "framework": "React",
            "components": ["LoginPage", "Dashboard", "UserManagement"]
        },
        "backend": {
            "framework": "Spring Boot",
            "services": ["AuthService", "UserService", "DataService"],
            "apis": ["/api/auth", "/api/users", "/api/data"]
        },
        "database": {
            "type": "PostgreSQL",
            "tables": ["users", "roles", "data", "logs"]
        },
        "infrastructure": {
            "deployment": "Kubernetes",
            "monitoring": "Prometheus + Grafana",
            "logging": "ELK Stack"
        }
    }
    
    current_project.architecture = architecture
    current_project.add_task(Task(
        id="ARCH001",
        title="架构设计",
        description="设计系统整体架构",
        assignee="架构师",
        status="completed",
        result={"architecture": architecture}
    ))
    
    return {
        "architecture": architecture,
        "tech_stack": "React + Spring Boot + PostgreSQL",
        "scalability": "支持水平扩展",
        "message": "架构设计完成"
    }


async def implement_backend() -> Dict[str, Any]:
    """开发Agent (后端): 实现后端代码"""
    logger.info("[后端开发] 开始实现后端服务")
    
    # 模拟代码生成
    code = {
        "AuthService.java": """
@Service
public class AuthService {
    public User authenticate(String username, String password) {
        // 认证逻辑
        return user;
    }
}
""",
        "UserService.java": """
@Service
public class UserService {
    public User createUser(UserDTO dto) {
        // 创建用户
        return user;
    }
}
""",
        "DataService.java": """
@Service
public class DataService {
    public List<Data> getAllData() {
        // 获取数据
        return dataList;
    }
}
"""
    }
    
    current_project.code.update({f"backend/{k}": v for k, v in code.items()})
    current_project.add_task(Task(
        id="DEV001",
        title="后端开发",
        description="实现后端服务和API",
        assignee="后端开发",
        status="completed",
        result={"files": list(code.keys()), "lines": 50}
    ))
    
    return {
        "status": "completed",
        "files_created": len(code),
        "total_lines": 50,
        "services": ["AuthService", "UserService", "DataService"],
        "message": "后端代码实现完成"
    }


async def implement_frontend() -> Dict[str, Any]:
    """开发Agent (前端): 实现前端代码"""
    logger.info("[前端开发] 开始实现前端界面")
    
    code = {
        "LoginPage.tsx": "// Login组件",
        "Dashboard.tsx": "// Dashboard组件",
        "UserManagement.tsx": "// 用户管理组件"
    }
    
    current_project.code.update({f"frontend/{k}": v for k, v in code.items()})
    current_project.add_task(Task(
        id="DEV002",
        title="前端开发",
        description="实现前端界面组件",
        assignee="前端开发",
        status="completed",
        result={"files": list(code.keys()), "components": 3}
    ))
    
    return {
        "status": "completed",
        "files_created": len(code),
        "components": ["LoginPage", "Dashboard", "UserManagement"],
        "message": "前端代码实现完成"
    }


async def run_tests() -> Dict[str, Any]:
    """测试Agent: 执行测试"""
    logger.info("[测试] 执行单元测试和集成测试")
    
    test_results = {
        "unit_tests": {
            "total": 45,
            "passed": 43,
            "failed": 2,
            "coverage": "87%"
        },
        "integration_tests": {
            "total": 15,
            "passed": 14,
            "failed": 1,
            "coverage": "75%"
        },
        "issues": [
            "AuthService.authenticate 边界条件测试失败",
            "UserManagement 组件渲染性能问题"
        ]
    }
    
    current_project.test_results = test_results
    current_project.add_task(Task(
        id="TEST001",
        title="质量测试",
        description="执行单元测试和集成测试",
        assignee="测试工程师",
        status="completed",
        result=test_results
    ))
    
    return {
        "status": "completed_with_issues",
        "test_results": test_results,
        "overall_pass_rate": "95%",
        "message": f"测试完成，发现{len(test_results['issues'])}个问题"
    }


async def deploy_application() -> Dict[str, Any]:
    """DevOps Agent: 部署应用"""
    logger.info("[DevOps] 部署应用到生产环境")
    
    deployment = {
        "environment": "production",
        "cluster": "k8s-prod-cluster",
        "namespace": "app-prod",
        "replicas": 3,
        "services": {
            "backend": "http://api.example.com",
            "frontend": "https://app.example.com"
        },
        "monitoring": "http://monitoring.example.com",
        "status": "healthy"
    }
    
    current_project.deployment = deployment
    current_project.add_task(Task(
        id="DEPLOY001",
        title="应用部署",
        description="部署应用到生产环境",
        assignee="DevOps工程师",
        status="completed",
        result=deployment
    ))
    
    return {
        "status": "success",
        "deployment": deployment,
        "health_check": "all_green",
        "message": "应用已成功部署到生产环境"
    }


async def generate_report() -> Dict[str, Any]:
    """生成项目报告"""
    logger.info("[报告] 生成项目完成报告")
    
    report = f"""
# 项目完成报告: {current_project.name}

## 项目概况
{current_project.get_summary()}

## 需求实现
{len(current_project.requirements)} 项功能需求已全部实现：
{chr(10).join(f"- {req}" for req in current_project.requirements)}

## 技术架构
- 前端: {current_project.architecture.get('frontend', {}).get('framework', 'N/A')}
- 后端: {current_project.architecture.get('backend', {}).get('framework', 'N/A')}
- 数据库: {current_project.architecture.get('database', {}).get('type', 'N/A')}

## 开发成果
- 代码文件: {len(current_project.code)} 个
- 后端服务: {len(current_project.architecture.get('backend', {}).get('services', []))} 个
- 前端组件: {len(current_project.architecture.get('frontend', {}).get('components', []))} 个

## 测试结果
- 单元测试通过率: {current_project.test_results.get('unit_tests', {}).get('passed', 0)}/{current_project.test_results.get('unit_tests', {}).get('total', 0)}
- 代码覆盖率: {current_project.test_results.get('unit_tests', {}).get('coverage', 'N/A')}

## 部署信息
- 环境: {current_project.deployment.get('environment', 'N/A')}
- 状态: {current_project.deployment.get('status', 'N/A')}
- 访问地址: {current_project.deployment.get('services', {}).get('frontend', 'N/A')}

## 任务完成情况
{chr(10).join(f"- [{task.status}] {task.title} ({task.assignee})" for task in current_project.tasks)}

生成时间: {datetime.now().isoformat()}
"""
    
    return {
        "report": report,
        "project_name": current_project.name,
        "tasks_completed": sum(1 for t in current_project.tasks if t.status == "completed"),
        "total_tasks": len(current_project.tasks)
    }


# ========== 主程序 ==========

async def run_team_collaboration(project_name: str, project_desc: str) -> str:
    """运行团队协作流程"""
    logger.info(f"========== 开始团队协作项目 ==========")
    logger.info(f"项目: {project_name}")
    
    # 初始化项目
    global current_project
    current_project = Project(name=project_name)
    
    # 执行完整的开发流程
    logger.info("\n[阶段1] 需求分析")
    req_result = await analyze_requirements(project_name, project_desc)
    print(f"✓ {req_result['message']}")
    
    logger.info("\n[阶段2] 架构设计")
    arch_result = await design_architecture(current_project.requirements)
    print(f"✓ {arch_result['message']}")
    
    logger.info("\n[阶段3] 并行开发")
    # 模拟并行开发
    backend_task = implement_backend()
    frontend_task = implement_frontend()
    backend_result, frontend_result = await asyncio.gather(backend_task, frontend_task)
    print(f"✓ {backend_result['message']}")
    print(f"✓ {frontend_result['message']}")
    
    logger.info("\n[阶段4] 质量测试")
    test_result = await run_tests()
    print(f"✓ {test_result['message']}")
    
    logger.info("\n[阶段5] 生产部署")
    deploy_result = await deploy_application()
    print(f"✓ {deploy_result['message']}")
    
    logger.info("\n[阶段6] 生成报告")
    report_result = await generate_report()
    
    logger.info(f"\n========== 项目完成 ==========")
    
    return report_result['report']


async def main():
    """主函数"""
    project_name = "企业用户管理系统"
    project_desc = "开发一个企业级的用户管理系统，支持用户认证、权限管理和数据操作"
    
    print(f"\n{'='*60}")
    print(f"团队协作示例: {project_name}")
    print(f"{'='*60}\n")
    
    try:
        report = await run_team_collaboration(project_name, project_desc)
        
        print(f"\n{'='*60}")
        print("项目完成报告")
        print(f"{'='*60}")
        print(report)
        print(f"{'='*60}\n")
        
    except Exception as e:
        logger.error(f"项目执行失败: {e}", exc_info=True)


if __name__ == "__main__":
    asyncio.run(main())

