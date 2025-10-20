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
数据采集Agent - 智能网页数据抓取

功能：
- 结构化数据提取
- 表格数据解析
- 多页面数据采集
- 数据清洗和验证
- 导出为多种格式
"""

import asyncio
import json
import csv
from typing import Dict, Any, List
from datetime import datetime
from dataclasses import dataclass, field

from google.adk.agents import LlmAgent
from google.adk.tools import FunctionTool

from ali_agentic_adk_python.config import get_runtime_settings
from ali_agentic_adk_python.core.model.dashscope_llm import DashscopeLLM

import logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class ScrapedData:
    """采集的数据"""
    source_url: str
    data_type: str
    content: Dict[str, Any]
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())
    metadata: Dict[str, Any] = field(default_factory=dict)


class DataStorage:
    """数据存储"""
    
    def __init__(self):
        self.data: List[ScrapedData] = []
    
    def add(self, data: ScrapedData):
        """添加数据"""
        self.data.append(data)
        logger.info(f"已存储数据: {data.data_type} from {data.source_url}")
    
    def get_by_type(self, data_type: str) -> List[ScrapedData]:
        """按类型获取数据"""
        return [d for d in self.data if d.data_type == data_type]
    
    def export_json(self) -> str:
        """导出为JSON"""
        return json.dumps([
            {
                "url": d.source_url,
                "type": d.data_type,
                "content": d.content,
                "timestamp": d.timestamp
            }
            for d in self.data
        ], ensure_ascii=False, indent=2)
    
    def export_csv(self, filename: str):
        """导出为CSV"""
        if not self.data:
            return
        
        # 获取所有字段
        all_fields = set()
        for d in self.data:
            all_fields.update(d.content.keys())
        
        with open(filename, 'w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=sorted(all_fields))
            writer.writeheader()
            for d in self.data:
                writer.writerow(d.content)
        
        logger.info(f"数据已导出到: {filename}")


# 全局数据存储
data_storage = DataStorage()


# ========== 工具函数 ==========

async def extract_article(url: str) -> Dict[str, Any]:
    """
    提取文章内容
    
    Args:
        url: 文章URL
        
    Returns:
        文章数据
    """
    logger.info(f"提取文章: {url}")
    
    # 模拟文章数据
    article_data = {
        "title": "人工智能的未来发展趋势",
        "author": "张三",
        "publish_date": "2025-01-15",
        "category": "科技",
        "tags": ["人工智能", "技术趋势", "未来科技"],
        "content": "人工智能技术正在快速发展...",
        "word_count": 2500,
        "read_time": "5分钟"
    }
    
    scraped = ScrapedData(
        source_url=url,
        data_type="article",
        content=article_data
    )
    data_storage.add(scraped)
    
    return {
        "success": True,
        "url": url,
        "data": article_data,
        "message": "文章提取成功"
    }


async def extract_table(url: str, table_index: int = 0) -> Dict[str, Any]:
    """
    提取表格数据
    
    Args:
        url: 页面URL
        table_index: 表格索引
        
    Returns:
        表格数据
    """
    logger.info(f"提取表格: {url}, 索引={table_index}")
    
    # 模拟表格数据
    table_data = {
        "headers": ["产品名称", "价格", "库存", "销量"],
        "rows": [
            ["iPhone 15", "¥5999", "充足", "1.2万"],
            ["Samsung S24", "¥5499", "紧张", "8千"],
            ["Xiaomi 14", "¥3999", "充足", "1.5万"],
        ],
        "row_count": 3,
        "column_count": 4
    }
    
    scraped = ScrapedData(
        source_url=url,
        data_type="table",
        content=table_data
    )
    data_storage.add(scraped)
    
    return {
        "success": True,
        "url": url,
        "table_index": table_index,
        "data": table_data,
        "message": f"表格提取成功，共{table_data['row_count']}行"
    }


async def extract_product_list(url: str, page: int = 1) -> Dict[str, Any]:
    """
    提取商品列表
    
    Args:
        url: 列表页URL
        page: 页码
        
    Returns:
        商品列表数据
    """
    logger.info(f"提取商品列表: {url}, 第{page}页")
    
    # 模拟商品列表
    products = []
    for i in range(1, 11):
        product = {
            "id": f"PROD{page}{i:02d}",
            "name": f"商品{i + (page-1)*10}",
            "price": 99.99 + i * 10,
            "rating": 4.0 + (i % 5) * 0.2,
            "reviews": 100 + i * 50,
            "image_url": f"https://example.com/product{i}.jpg"
        }
        products.append(product)
        
        scraped = ScrapedData(
            source_url=f"{url}?page={page}",
            data_type="product",
            content=product
        )
        data_storage.add(scraped)
    
    return {
        "success": True,
        "url": url,
        "page": page,
        "count": len(products),
        "products": products,
        "has_next_page": page < 5,  # 假设有5页
        "message": f"成功提取第{page}页，共{len(products)}个商品"
    }


async def extract_contact_info(url: str) -> Dict[str, Any]:
    """
    提取联系信息
    
    Args:
        url: 页面URL
        
    Returns:
        联系信息
    """
    logger.info(f"提取联系信息: {url}")
    
    contact_data = {
        "company": "示例科技有限公司",
        "email": "contact@example.com",
        "phone": "+86 010-12345678",
        "address": "北京市朝阳区示例大街123号",
        "website": "https://www.example.com",
        "social_media": {
            "weibo": "@example",
            "wechat": "example_wechat",
            "linkedin": "example-company"
        }
    }
    
    scraped = ScrapedData(
        source_url=url,
        data_type="contact",
        content=contact_data
    )
    data_storage.add(scraped)
    
    return {
        "success": True,
        "url": url,
        "data": contact_data,
        "message": "联系信息提取成功"
    }


async def validate_data(data_type: str) -> Dict[str, Any]:
    """
    验证采集的数据
    
    Args:
        data_type: 数据类型
        
    Returns:
        验证结果
    """
    logger.info(f"验证数据: {data_type}")
    
    data_list = data_storage.get_by_type(data_type)
    
    if not data_list:
        return {
            "success": False,
            "message": f"没有找到类型为'{data_type}'的数据"
        }
    
    # 简单验证逻辑
    valid_count = len(data_list)
    invalid_count = 0
    
    validation_result = {
        "data_type": data_type,
        "total_count": len(data_list),
        "valid_count": valid_count,
        "invalid_count": invalid_count,
        "validity_rate": valid_count / len(data_list) * 100 if data_list else 0,
        "issues": []
    }
    
    logger.info(f"验证完成: {valid_count}/{len(data_list)} 条数据有效")
    
    return {
        "success": True,
        "result": validation_result,
        "message": f"验证完成，{valid_count}条有效数据"
    }


async def export_data(format: str = "json", filename: str = None) -> Dict[str, Any]:
    """
    导出数据
    
    Args:
        format: 导出格式 (json, csv)
        filename: 文件名
        
    Returns:
        导出结果
    """
    logger.info(f"导出数据: format={format}")
    
    if not data_storage.data:
        return {
            "success": False,
            "message": "没有数据可导出"
        }
    
    if filename is None:
        filename = f"scraped_data_{datetime.now().strftime('%Y%m%d_%H%M%S')}.{format}"
    
    if format == "json":
        json_data = data_storage.export_json()
        with open(filename, 'w', encoding='utf-8') as f:
            f.write(json_data)
        
        return {
            "success": True,
            "format": "json",
            "filename": filename,
            "record_count": len(data_storage.data),
            "file_size_kb": len(json_data.encode('utf-8')) / 1024,
            "message": f"成功导出{len(data_storage.data)}条数据到{filename}"
        }
    
    elif format == "csv":
        data_storage.export_csv(filename)
        
        return {
            "success": True,
            "format": "csv",
            "filename": filename,
            "record_count": len(data_storage.data),
            "message": f"成功导出{len(data_storage.data)}条数据到{filename}"
        }
    
    else:
        return {
            "success": False,
            "message": f"不支持的格式: {format}"
        }


async def batch_scrape(urls: List[str], data_type: str) -> Dict[str, Any]:
    """
    批量采集
    
    Args:
        urls: URL列表
        data_type: 数据类型
        
    Returns:
        批量采集结果
    """
    logger.info(f"批量采集: {len(urls)}个URL, 类型={data_type}")
    
    results = []
    success_count = 0
    
    for url in urls:
        # 根据类型调用相应的提取函数
        if data_type == "article":
            result = await extract_article(url)
        elif data_type == "table":
            result = await extract_table(url)
        elif data_type == "product":
            result = await extract_product_list(url)
        else:
            result = {"success": False, "message": f"未知类型: {data_type}"}
        
        results.append(result)
        if result.get("success"):
            success_count += 1
    
    return {
        "success": True,
        "total": len(urls),
        "success_count": success_count,
        "failed_count": len(urls) - success_count,
        "results": results,
        "message": f"批量采集完成: {success_count}/{len(urls)} 成功"
    }


# ========== Agent定义 ==========

def create_scraper_agent() -> LlmAgent:
    """创建数据采集Agent"""
    
    runtime_settings = get_runtime_settings()
    dashscope_settings = runtime_settings.dashscope()
    
    if not dashscope_settings:
        raise RuntimeError("需要配置DashScope")
    
    model = DashscopeLLM.from_settings(dashscope_settings)
    
    tools = [
        FunctionTool(
            name="extract_article",
            description="从URL提取文章内容，包括标题、作者、日期、正文等",
            func=extract_article
        ),
        FunctionTool(
            name="extract_table",
            description="从URL提取表格数据，参数：url=页面地址，table_index=表格索引(默认0)",
            func=extract_table
        ),
        FunctionTool(
            name="extract_product_list",
            description="从URL提取商品列表，参数：url=列表页地址，page=页码(默认1)",
            func=extract_product_list
        ),
        FunctionTool(
            name="extract_contact_info",
            description="从URL提取联系信息，包括邮箱、电话、地址等",
            func=extract_contact_info
        ),
        FunctionTool(
            name="validate_data",
            description="验证采集的数据质量，参数：data_type=数据类型",
            func=validate_data
        ),
        FunctionTool(
            name="export_data",
            description="导出采集的数据，参数：format=格式(json/csv)，filename=文件名(可选)",
            func=export_data
        ),
        FunctionTool(
            name="batch_scrape",
            description="批量采集多个URL，参数：urls=URL列表，data_type=数据类型",
            func=batch_scrape
        ),
    ]
    
    agent = LlmAgent(
        name="dataScraperAgent",
        model=model,
        instruction="""你是一个专业的网页数据采集助手。你可以：

1. **文章采集** - 使用extract_article提取文章内容
2. **表格提取** - 使用extract_table提取结构化表格数据
3. **商品列表** - 使用extract_product_list提取电商商品信息
4. **联系信息** - 使用extract_contact_info提取联系方式
5. **批量采集** - 使用batch_scrape批量处理多个URL
6. **数据验证** - 使用validate_data检查数据质量
7. **数据导出** - 使用export_data导出为JSON或CSV

工作流程：
1. 理解用户的采集需求
2. 选择合适的采集工具
3. 执行数据提取
4. 验证数据完整性
5. 按用户要求的格式导出

请确保：
- 正确识别数据类型
- 处理多页面的情况
- 验证数据质量
- 提供清晰的采集报告""",
        description="智能网页数据采集Agent",
        tools=tools
    )
    
    return agent


async def main():
    """主函数 - 测试数据采集场景"""
    
    test_scenarios = [
        "帮我从 https://example.com/news/article123 提取文章内容",
        "从 https://example.com/products 提取前3页的商品列表",
        "提取 https://example.com/about 页面的联系信息",
        "验证所有采集的product类型数据，并导出为CSV"
    ]
    
    # 使用第二个场景作为示例
    request = test_scenarios[1]
    
    print(f"\n{'='*60}")
    print("数据采集场景测试")
    print(f"{'='*60}\n")
    print(f"请求: {request}\n")
    
    # 重置存储
    global data_storage
    data_storage = DataStorage()
    
    # 创建Agent（简化版，直接调用工具）
    logger.info("开始采集...")
    
    # 模拟采集3页商品
    for page in range(1, 4):
        result = await extract_product_list("https://example.com/products", page)
        print(f"第{page}页: {result['message']}")
    
    # 验证数据
    validation = await validate_data("product")
    print(f"\n数据验证: {validation['result']['message'] if validation['success'] else validation['message']}")
    
    # 导出数据
    export_result = await export_data("json", "products.json")
    print(f"导出结果: {export_result['message']}")
    
    print(f"\n总共采集了 {len(data_storage.data)} 条数据")
    print(f"\n{'='*60}\n")


if __name__ == "__main__":
    asyncio.run(main())

