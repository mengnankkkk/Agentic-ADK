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


from .basic_embedding import BasicEmbedding
from .openai_embedding import OpenAIEmbedding
from .azure_openai_embedding import AzureOpenAIEmbedding
from .aws_embedding import AWSEmbedding
from .bailian_embedding import BailianEmbedding
from .cohere_embedding import CohereEmbedding
from .google_embedding import GoogleEmbedding
from .huggingface_embedding import HuggingFaceEmbedding
from .jina_embedding import JinaEmbedding
from .mistral_embedding import MistralEmbedding
from .minimax_embedding import MiniMaxEmbedding
from .moonshot_embedding import MoonshotEmbedding
from .qianfan_embedding import QianfanEmbedding
from .doubao_embedding import DoubaoEmbedding
from .anthropic_embedding import AnthropicEmbedding
from .deepseek_embedding import DeepSeekEmbedding
from .tencent_embedding import TencentEmbedding
from .voyage_embedding import VoyageEmbedding
from .zhipu_embedding import ZhipuEmbedding
from .yi_embedding import YiEmbedding
from .sensenova_embedding import SenseNovaEmbedding
from .siliconflow_embedding import SiliconFlowEmbedding
from .ollama_embedding import OllamaEmbedding


__all__ = [
    "BasicEmbedding",
    "OpenAIEmbedding",
    "AzureOpenAIEmbedding",
    "AWSEmbedding",
    "BailianEmbedding",
    "CohereEmbedding",
    "GoogleEmbedding",
    "HuggingFaceEmbedding",
    "JinaEmbedding",
    "MistralEmbedding",
    "MiniMaxEmbedding",
    "MoonshotEmbedding",
    "QianfanEmbedding",
    "DoubaoEmbedding",
    "AnthropicEmbedding",
    "DeepSeekEmbedding",
    "TencentEmbedding",
    "VoyageEmbedding",
    "ZhipuEmbedding",
    "YiEmbedding",
    "SenseNovaEmbedding",
    "SiliconFlowEmbedding",
    "OllamaEmbedding",
]
