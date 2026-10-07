# 会话交接入口

用户要求交接材料直接保存在 GitHub，后续会话也更新这里。当前工作分支为 `feature/export-conversation`，不要默认读取 main 的旧版本。

读取顺序：

1. 仓库根目录 `AGENTS.md`：持续交接约定。
2. [latest.md](latest.md)：当前任务、软件背景、进度与下一步。
3. [REVIEW.md](REVIEW.md)：最新证据分级与关键边界。
4. [验证报告](../../tools/feasibility/REPORT.md)和[探针接入说明](../../tools/feasibility/README.md)：风险表、最小验证代码和判断标准。

`tools/feasibility/integration-baseline.patch` 仅用于指定 `200b7898…` 基线；它不是针对当前 feature 分支的直接应用补丁。不要因文档中有构建步骤就自动应用或发布。

## 每步持续更新（所有接手 AI 必须遵守）

用户于2026-10-07明确要求：每完成一个工作步骤就更新交接文档，并让之后接手的AI持续这样做。这个约定取代旧的“仅压缩/迁移时更新文档”；下一会话提示词仍只在实际迁移等指定情形提供。

每个独立工作步骤完成后、开始下一步前：

1. 更新 [latest.md](latest.md)：最新要求、实际源码提交、已完成内容、验证结果与证据、失败/未验证项、下一步。
2. 验证结论或风险边界改变时同步 [REVIEW.md](REVIEW.md)，保留真实失败证据。
3. 核对远端变更，把本步源码/测试与交接提交并推送到工作分支。仅文档更新可用 `[skip ci]`；不改版号、不自动公开Release。

记录有独立结果的工作步骤，不把每条工具命令当一步；提交/push前的必要检查属于同一步，交接维护自身无需递归提交。未完成或等待CI必须明确标注，不能预先记为通过。

## 下一会话提示词的触发条件

仅在需要压缩上下文、迁移会话或用户明确要求时提供下一会话提示词。普通回复、提交文档或阶段进度汇报不自动触发迁移，不要每次回复都生成提示词。实际需要交接时，仍须将必要材料提交并推送至工作分支。

## 下一会话提示词模板（仅实际交接时使用）

```text
继续处理DevenirTwilight/ChatGPT-Nova工作分支feature/export-conversation。先读AGENTS.md、docs/handoff/latest.md、docs/handoff/REVIEW.md、docs/frozen-page-save.md及实际远端HEAD/源码/测试/最近CI，不按历史文档猜HEAD或回退修复。当前任务为一次当前页面clone生成普通HTML与Markdown、系统WebView打印同份HTML；Share/MHTML/新历史扫描已终止。以latest中的实际待办和用户最新指令续接，不伪称服务器完整历史。保留原包名/签名/版号、不合并main/公开Release；已授权必要工作分支提交/push及稳定测试APK交付。每完成一个工作步骤都更新latest，验证结论或边界变化时同步REVIEW，并随步骤提交push工作分支，后续接手AI也必须执行；不要等压缩/迁移才更新。仅实际压缩/迁移或用户明确要求时提供下一会话提示词，普通回复不要生成。
```
