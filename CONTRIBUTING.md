# Contributing / 贡献指南

This project hosts multiple SDKs and tooling components (Python, Java, front-end, and shared infrastructure). The steps below describe how to propose changes consistently across the repository.

## English Guide

### Before You Start
- Read the relevant README for the component you plan to modify.
- Check existing issues and discussions before opening a new one to avoid duplicates.
- For significant changes, please open an issue first so maintainers can provide early feedback.

### Development Workflow
1. Fork the repository and create a feature branch from the latest `main`.
2. Set up your environment following the instructions in the component-specific README (e.g., `ali-agentic-adk-python/README.md`).
3. Keep changes focused; separate unrelated fixes into different branches/PRs.
4. Run formatting and linting tools required for the component you are touching (for example, `ruff`/`pytest` for Python, `npm run lint`/`npm test` for the front-end, or `mvn test` for the Java module).
5. Add or update tests whenever you alter behavior or introduce new features.

### Commit and Pull Request Guidelines
- Write clear commit messages that explain the *why* and the *what* of the change.
- Format pull request titles using Conventional Commit prefixes such as `feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, or other suitable categories.
- Follow the pull request template located at `.github/pull_request_template.md`:
  - Describe the motivation and implementation details.
  - Note whether the change closes an existing issue.
  - Document how reviewers can verify the change.
  - Call out any follow-up actions or risks.
- Ensure all checks pass before requesting a review.
- Address review feedback promptly and keep conversations polite and constructive.

### Additional Recommendations
- Include documentation updates when behavior or public APIs change.
- Avoid committing generated artifacts or secrets.
- If you are unsure about a decision, leave a comment in the PR so reviewers can provide direction.

## 中文指南

### 开始之前
- 请先阅读与您要修改模块对应的 README 文档，了解基础环境与依赖。
- 提交新 Issue 之前，请搜索现有 Issue 与讨论，避免重复。
- 对于影响较大的改动，建议先创建 Issue 并与维护者讨论，提前达成共识。

### 开发流程
1. Fork 本仓库，并基于最新的 `main` 分支创建功能分支。
2. 根据模块 README 的指引完成本地环境搭建 。
3. 专注于单一主题，不同问题请拆分到独立分支与PR。
4. 在提交前运行相应模块所需的格式化与检查工具（如 Python 模块使用 `ruff`/`pytest`，前端模块使用 `npm run lint`/`npm test`，Java 模块运行 `mvn test` 等）。
5. 当行为变更或新增功能时，请同步补充或更新测试用例。

### 提交与合并请求规范
- 编写清晰的 commit message，说明修改的动机与内容。
- Pull Request 标题请遵循 Conventional Commit 前缀，例如 `feat:`、`fix:`、`chore:`、`docs:`、`refactor:` 等，以便快速了解改动类型。
- 创建 Pull Request 时请使用 `.github/pull_request_template.md` 中的模板：
  - 描述改动目的与实现方式。
  - 说明是否关闭了某个 Issue。
  - 告知审阅者验证此改动的步骤。
  - 提前告知潜在风险或后续工作。
- 在请求评审之前，确认所有自动检查均已通过。
- 积极响应评审意见，保持专业、友好的交流。

### 其他建议
- 若改动会影响文档或公开接口，请同步更新相应说明。
- 避免提交生成文件或敏感信息。
- 如果存在不确定的设计，请在 PR 中留言说明，方便审阅者给出建议。

感谢每一位贡献者的投入！
