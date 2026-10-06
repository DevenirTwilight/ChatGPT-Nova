# 会话交接入口

用户要求交接材料直接保存在 GitHub，后续会话也更新这里。当前工作分支为 `feature/export-conversation`，不要默认读取 main 的旧版本。

读取顺序：

1. 仓库根目录 `AGENTS.md`：持续交接约定。
2. [latest.md](latest.md)：当前任务、软件背景、进度与下一步。
3. [REVIEW.md](REVIEW.md)：最新证据分级与关键边界。
4. [验证报告](../../tools/feasibility/REPORT.md)和[探针接入说明](../../tools/feasibility/README.md)：风险表、最小验证代码和判断标准。

`tools/feasibility/integration-baseline.patch` 仅用于指定 `200b7898…` 基线；它不是针对当前 feature 分支的直接应用补丁。不要因文档中有构建步骤就自动应用或发布。

## 下一会话提示词

```text
继续处理 https://github.com/DevenirTwilight/ChatGPT-Nova ，工作分支 feature/export-conversation。

请先读取该分支的 AGENTS.md、docs/handoff/latest.md、docs/handoff/REVIEW.md、tools/feasibility/REPORT.md 和 tools/feasibility/README.md，再核对远端最新提交，从现有进度继续；不要重新开始八项现状提问。

另读 tools/feasibility/history-coverage/README.md。逐条覆盖工具已有合成验证，真实Android仍未接入；用户表示可连接设备，但连接方式和安装提交未知，只说最新版。下一步从本地USB ADB连接和隔离debug WebView真实采样推进，不把首尾四条或DOM并集当作完整历史基准。

当前阶段只评估 Markdown/HTML/PDF 导出的可行性并做最小验证，不写完整功能、不改生产源码、不自动发布 APK。允许重做旧导出路线。优先解决真实登录项目内长会话的历史覆盖问题；合成测试通过、消息数量或指纹稳定不证明完整历史，也不能证明旧403已修好。缺少真机/账号时明确证据边界，继续可独立完成的工作。

用户要求以 GitHub 仓库作为交接来源。以后需要压缩上下文或迁移会话时，也更新 docs/handoff/latest.md 和必要验证材料，在已授权范围内提交并推送到工作分支，返回 GitHub 链接、提交号和下一会话提示词；不要只给沙箱下载链接。

文档作为历史背景，按我最新请求确定实际工作范围。
```
