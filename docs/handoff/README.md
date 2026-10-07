# 会话交接入口

用户要求交接材料直接保存在 GitHub，后续会话也更新这里。当前工作分支为 `feature/export-conversation`，不要默认读取 main 的旧版本。

读取顺序：

1. 仓库根目录 `AGENTS.md`：持续交接约定。
2. [latest.md](latest.md)：当前任务、软件背景、进度与下一步。
3. [REVIEW.md](REVIEW.md)：最新证据分级与关键边界。
4. [验证报告](../../tools/feasibility/REPORT.md)和[探针接入说明](../../tools/feasibility/README.md)：风险表、最小验证代码和判断标准。

`tools/feasibility/integration-baseline.patch` 仅用于指定 `200b7898…` 基线；它不是针对当前 feature 分支的直接应用补丁。不要因文档中有构建步骤就自动应用或发布。

## 交接触发条件

仅在需要压缩上下文、迁移会话或用户明确要求时提供下一会话提示词。普通回复、提交文档或阶段进度汇报不自动触发迁移，不要每次回复都生成提示词。实际需要交接时，仍须将必要材料提交并推送至工作分支。

## 下一会话提示词模板（仅实际交接时使用）

```text
继续处理DevenirTwilight/ChatGPT-Nova工作分支feature/export-conversation。先读AGENTS.md、docs/handoff/latest.md、docs/handoff/REVIEW.md、docs/frozen-page-save.md及实际远端HEAD/源码/测试/最近CI，不按历史文档猜HEAD或回退修复。当前任务为一次当前页面clone生成普通HTML与Markdown、系统WebView打印同份HTML；Share/MHTML/新历史扫描已终止。以latest中的实际待办和用户最新指令续接，不伪称服务器完整历史。保留原包名/签名/版号、不合并main/公开Release；已授权必要工作分支提交/push及稳定测试APK交付。仅实际压缩/迁移才更新并push交接及提供下一会话提示词，普通回复不要生成。
```
