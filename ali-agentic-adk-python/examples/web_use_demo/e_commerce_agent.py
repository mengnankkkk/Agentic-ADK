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
电商场景Agent - 智能购物助手

功能：
- 商品搜索
- 价格比较
- 自动筛选
- 购物车管理
- 下单流程自动化
"""

import asyncio
import logging
from typing import Dict, Any, List, Optional
from datetime import datetime
from dataclasses import dataclass, field

from google.adk.agents import LlmAgent
from google.adk.tools import FunctionTool
from google.adk.sessions import InMemorySessionService
from google.adk.runners import Runner
from google.genai import types

from ali_agentic_adk_python.config import get_runtime_settings
from ali_agentic_adk_python.core.model.dashscope_llm import DashscopeLLM

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class Product:
    """商品信息"""
    name: str
    price: float
    rating: float
    reviews: int
    seller: str
    url: str
    in_stock: bool = True
    discount: float = 0.0
    
    def get_final_price(self) -> float:
        """获取最终价格"""
        return self.price * (1 - self.discount)
    
    def get_score(self) -> float:
        """计算综合评分"""
        price_score = max(0, 100 - self.price / 10)  # 价格越低分数越高
        rating_score = self.rating * 20  # 评分转为百分制
        review_score = min(100, self.reviews / 10)  # 评论数影响
        return (price_score * 0.4 + rating_score * 0.4 + review_score * 0.2)


@dataclass
class ShoppingCart:
    """购物车"""
    items: List[Product] = field(default_factory=list)
    
    def add_item(self, product: Product):
        """添加商品"""
        self.items.append(product)
        logger.info(f"已将 {product.name} 加入购物车")
    
    def remove_item(self, product_name: str):
        """移除商品"""
        self.items = [item for item in self.items if item.name != product_name]
        logger.info(f"已将 {product_name} 从购物车移除")
    
    def get_total(self) -> float:
        """计算总价"""
        return sum(item.get_final_price() for item in self.items)
    
    def get_summary(self) -> str:
        """获取购物车摘要"""
        if not self.items:
            return "购物车为空"
        
        summary = f"购物车（{len(self.items)}件商品）:\n"
        for i, item in enumerate(self.items, 1):
            summary += f"{i}. {item.name} - ¥{item.get_final_price():.2f}\n"
        summary += f"\n总计: ¥{self.get_total():.2f}"
        return summary


# 全局购物车
shopping_cart = ShoppingCart()


# ========== 工具函数 ==========

async def search_products(keyword: str, max_price: Optional[float] = None) -> Dict[str, Any]:
    """
    搜索商品
    
    Args:
        keyword: 搜索关键词
        max_price: 最高价格限制
        
    Returns:
        搜索结果
    """
    logger.info(f"搜索商品: {keyword}, 最高价格: {max_price}")
    
    # 模拟商品数据
    mock_products = [
        Product("iPhone 15 Pro 256GB", 7999.0, 4.8, 5200, "Apple官方旗舰店", "https://example.com/iphone15", True, 0.05),
        Product("iPhone 15 Pro 256GB", 7899.0, 4.7, 3800, "京东自营", "https://example.com/jd-iphone15", True, 0.08),
        Product("iPhone 15 Pro 256GB", 8199.0, 4.9, 6100, "天猫Apple旗舰店", "https://example.com/tmall-iphone15", True, 0.03),
        Product("iPhone 14 Pro 256GB", 6499.0, 4.7, 8900, "京东自营", "https://example.com/iphone14", True, 0.10),
        Product("Huawei Mate 60 Pro", 6999.0, 4.9, 12000, "华为官方旗舰店", "https://example.com/mate60", True, 0.05),
    ]
    
    # 筛选商品
    filtered_products = []
    for product in mock_products:
        if keyword.lower() in product.name.lower():
            if max_price is None or product.get_final_price() <= max_price:
                filtered_products.append(product)
    
    logger.info(f"找到 {len(filtered_products)} 个符合条件的商品")
    
    return {
        "keyword": keyword,
        "count": len(filtered_products),
        "products": [
            {
                "name": p.name,
                "price": p.price,
                "final_price": p.get_final_price(),
                "rating": p.rating,
                "reviews": p.reviews,
                "seller": p.seller,
                "score": p.get_score(),
                "discount": f"{p.discount*100:.0f}%"
            }
            for p in filtered_products
        ]
    }


async def compare_prices(product_name: str) -> Dict[str, Any]:
    """
    比价功能
    
    Args:
        product_name: 商品名称
        
    Returns:
        价格比较结果
    """
    logger.info(f"比价: {product_name}")
    
    # 模拟不同平台价格
    price_comparison = {
        "product": product_name,
        "platforms": [
            {"name": "京东", "price": 7899.0, "discount": "8折", "shipping": "免运费"},
            {"name": "天猫", "price": 8199.0, "discount": "9.7折", "shipping": "免运费"},
            {"name": "拼多多", "price": 7699.0, "discount": "无折扣", "shipping": "¥10"},
            {"name": "苏宁", "price": 7999.0, "discount": "无折扣", "shipping": "免运费"},
        ],
        "best_deal": "拼多多 - ¥7709.0 (含运费)",
        "average_price": 7924.0
    }
    
    logger.info(f"最优惠: {price_comparison['best_deal']}")
    
    return price_comparison


async def add_to_cart(product_name: str, seller: str = "京东自营") -> Dict[str, Any]:
    """
    添加到购物车
    
    Args:
        product_name: 商品名称
        seller: 卖家
        
    Returns:
        操作结果
    """
    logger.info(f"添加到购物车: {product_name} from {seller}")
    
    # 创建模拟商品
    product = Product(
        name=product_name,
        price=7899.0,
        rating=4.7,
        reviews=3800,
        seller=seller,
        url=f"https://example.com/{product_name.replace(' ', '-')}",
        discount=0.08
    )
    
    shopping_cart.add_item(product)
    
    return {
        "success": True,
        "product": product_name,
        "seller": seller,
        "cart_summary": shopping_cart.get_summary()
    }


async def checkout() -> Dict[str, Any]:
    """
    结算购物车
    
    Returns:
        订单信息
    """
    logger.info("开始结算")
    
    if not shopping_cart.items:
        return {
            "success": False,
            "message": "购物车为空，无法结算"
        }
    
    order_info = {
        "success": True,
        "order_id": f"ORDER_{datetime.now().strftime('%Y%m%d%H%M%S')}",
        "items": [
            {
                "name": item.name,
                "price": item.get_final_price(),
                "seller": item.seller
            }
            for item in shopping_cart.items
        ],
        "total": shopping_cart.get_total(),
        "estimated_delivery": "2-3个工作日",
        "message": "订单已生成，等待支付"
    }
    
    logger.info(f"订单生成成功: {order_info['order_id']}")
    
    return order_info


async def apply_coupon(coupon_code: str) -> Dict[str, Any]:
    """
    应用优惠券
    
    Args:
        coupon_code: 优惠券代码
        
    Returns:
        应用结果
    """
    logger.info(f"应用优惠券: {coupon_code}")
    
    # 模拟优惠券
    coupons = {
        "SAVE100": {"discount": 100.0, "min_purchase": 500.0},
        "FIRST50": {"discount": 50.0, "min_purchase": 200.0},
        "VIP200": {"discount": 200.0, "min_purchase": 1000.0},
    }
    
    if coupon_code not in coupons:
        return {
            "success": False,
            "message": "优惠券代码无效"
        }
    
    coupon = coupons[coupon_code]
    cart_total = shopping_cart.get_total()
    
    if cart_total < coupon["min_purchase"]:
        return {
            "success": False,
            "message": f"购物车金额不足，需满¥{coupon['min_purchase']}"
        }
    
    return {
        "success": True,
        "discount": coupon["discount"],
        "original_total": cart_total,
        "final_total": cart_total - coupon["discount"],
        "message": f"已优惠¥{coupon['discount']}"
    }


async def filter_products(min_rating: float = 4.5, max_price: float = 10000.0) -> Dict[str, Any]:
    """
    筛选商品
    
    Args:
        min_rating: 最低评分
        max_price: 最高价格
        
    Returns:
        筛选结果
    """
    logger.info(f"筛选商品: 评分≥{min_rating}, 价格≤¥{max_price}")
    
    result = {
        "criteria": {
            "min_rating": min_rating,
            "max_price": max_price
        },
        "filtered_count": 3,
        "message": f"找到3个符合条件的商品（评分≥{min_rating}，价格≤¥{max_price}）"
    }
    
    return result


# ========== Agent定义 ==========

def create_ecommerce_agent() -> LlmAgent:
    """
    创建电商购物Agent
    
    Returns:
        配置好的购物Agent
    """
    runtime_settings = get_runtime_settings()
    dashscope_settings = runtime_settings.dashscope()
    
    if not dashscope_settings:
        raise RuntimeError("需要配置DashScope")
    
    model = DashscopeLLM.from_settings(dashscope_settings)
    
    # 创建工具
    tools = [
        FunctionTool(
            name="search_products",
            description="搜索商品。参数：keyword(必需)=搜索关键词，max_price(可选)=最高价格",
            func=search_products
        ),
        FunctionTool(
            name="compare_prices",
            description="比较不同平台的商品价格。参数：product_name=商品名称",
            func=compare_prices
        ),
        FunctionTool(
            name="filter_products",
            description="根据条件筛选商品。参数：min_rating=最低评分(默认4.5)，max_price=最高价格(默认10000)",
            func=filter_products
        ),
        FunctionTool(
            name="add_to_cart",
            description="将商品添加到购物车。参数：product_name=商品名称，seller=卖家名称",
            func=add_to_cart
        ),
        FunctionTool(
            name="apply_coupon",
            description="应用优惠券。参数：coupon_code=优惠券代码",
            func=apply_coupon
        ),
        FunctionTool(
            name="checkout",
            description="结算购物车并生成订单",
            func=checkout
        ),
    ]
    
    # 创建Agent
    agent = LlmAgent(
        name="ecommerceShoppingAgent",
        model=model,
        instruction="""你是一个专业的电商购物助手。你的职责是帮助用户：

1. **商品搜索** - 使用search_products工具搜索商品
2. **价格比较** - 使用compare_prices工具比较不同平台价格
3. **智能筛选** - 使用filter_products工具根据评分和价格筛选
4. **购物车管理** - 使用add_to_cart工具添加商品到购物车
5. **优惠券应用** - 使用apply_coupon工具应用优惠券
6. **订单结算** - 使用checkout工具完成购买

工作流程：
1. 理解用户需求
2. 搜索相关商品
3. 比较价格和筛选最佳选项
4. 将选中的商品加入购物车
5. 应用可用的优惠券
6. 完成结算

请始终：
- 提供详细的商品信息
- 推荐性价比最高的选项
- 提醒用户注意优惠信息
- 确认每个操作步骤""",
        description="智能电商购物助手",
        tools=tools
    )
    
    return agent


# ========== 主程序 ==========

async def run_shopping_scenario(user_request: str) -> str:
    """
    运行购物场景
    
    Args:
        user_request: 用户请求
        
    Returns:
        处理结果
    """
    logger.info(f"========== 开始购物流程 ==========")
    logger.info(f"用户需求: {user_request}")
    
    # 重置购物车
    global shopping_cart
    shopping_cart = ShoppingCart()
    
    # 创建Agent
    agent = create_ecommerce_agent()
    
    # 设置Session
    session_service = InMemorySessionService()
    session_id = f"shopping_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    await session_service.create_session(
        app_name="ecommerce_agent",
        user_id="shopper_001",
        session_id=session_id
    )
    
    # 创建Runner
    runner = Runner(
        agent=agent,
        app_name="ecommerce_agent",
        session_service=session_service
    )
    
    # 运行
    user_message = types.Content(
        role='user',
        parts=[types.Part(text=user_request)]
    )
    
    final_response = ""
    tool_calls_made = []
    
    try:
        events = runner.run_async(
            user_id="shopper_001",
            session_id=session_id,
            new_message=user_message
        )
        
        async for event in events:
            if event.tool_calls:
                tool_names = [tc.name for tc in event.tool_calls]
                tool_calls_made.extend(tool_names)
                logger.info(f"调用工具: {', '.join(tool_names)}")
            
            if event.is_final_response() and event.content and event.content.parts:
                final_response = event.content.parts[0].text
                
    except Exception as e:
        logger.error(f"购物流程失败: {e}", exc_info=True)
        final_response = f"抱歉，购物过程遇到问题: {str(e)}"
    
    logger.info(f"========== 购物流程完成 ==========")
    logger.info(f"共调用了 {len(tool_calls_made)} 个工具")
    logger.info(f"购物车状态: {shopping_cart.get_summary()}")
    
    return final_response


async def main():
    """主函数 - 测试不同的购物场景"""
    
    scenarios = [
        {
            "name": "场景1：搜索并比价",
            "request": "我想买一部iPhone 15 Pro 256GB，预算在8000元左右，帮我找找最便宜的"
        },
        {
            "name": "场景2：筛选并购买",
            "request": "帮我找评分4.5以上、价格不超过8000元的iPhone，选一个最好的加入购物车"
        },
        {
            "name": "场景3：使用优惠券结算",
            "request": "把iPhone 15 Pro加入购物车，使用优惠券SAVE100，然后结算"
        },
    ]
    
    # 运行第二个场景作为示例
    scenario = scenarios[1]
    
    print(f"\n{'='*60}")
    print(f"{scenario['name']}")
    print(f"{'='*60}\n")
    
    try:
        result = await run_shopping_scenario(scenario['request'])
        
        print(f"\nAgent响应:")
        print(f"{'-'*60}")
        print(result)
        print(f"{'-'*60}\n")
        
        print(f"购物车最终状态:")
        print(shopping_cart.get_summary())
        print(f"\n{'='*60}\n")
        
    except Exception as e:
        logger.error(f"场景执行失败: {e}", exc_info=True)


if __name__ == "__main__":
    asyncio.run(main())

