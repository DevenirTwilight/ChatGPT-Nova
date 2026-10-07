# 本地 Deep Research 正文恢复

## 已确认的源与归属

官方导出标准 `library_files.json` 中 `library_artifact_type=deep_research_report` 的记录，以 `origination_thread_id` 精确关联外层会话 official id，以 canonical `file_id` 精确关联 ZIP 中唯一同名 `.dat`。不依赖 filename-map、文件名称、正文相似度、ZIP 顺序或普通附件引用。`backing_conversation_id` 是研究任务的独立会话，不作为外层聊天归属。报告及 origination message id 可能都不在聊天 mapping，因此不推断精确聊天插入位置。

支持已观察的 JSON version 1：`widget_state.status=completed`，`widget_state.report_message` 的 author role=assistant、content type=text、metadata.is_complete=true，parts 为至少一个非空纯字符串。仅恢复这个明确的最终正文消息；不导入 activity_messages、thoughts、source_searches，也不将任意 widget 字段当报告。

本次授权输入在仓库外独立核对：标准 inventory 635 条中 19 条研究报告，用户指定会话关联 4 条完成报告，正文20797/24339/25569/26872字符，总97577。真实名称、会话/文件/消息标识、正文及二进制不进Git、CI或日志。

## 阅读与导出

会话阅读器新增“研究报告 (数量)”入口，切到独立报告阅读，可返回聊天。报告区同时包含于会话 HTML/Markdown/System Print PDF，在“主链”和“全部分支”之间切换不移除报告。它们保持独立报告身份，不拼进某条聊天分支或冒称精确气泡位置。HTML 使用既有安全 CommonMark 渲染；Markdown 保留原文字；PDF 打印相同离线 HTML。引用元数据保留，不在本次实现额外 citation restoration、来源网页抓取或外部链接认证。

## 持久化与再导入

SQLite schema 3 非破坏新增 research_reports 表，按 conversation + official file identity 唯一；保存完成 report_message、标题/状态/时间以及 first/latest source。schema 1→3保留原聊天并补建资产表，schema 2→3保留聊天/资产并补报告表。升级应用不会自动取得旧ZIP，需手动重新导入一次补回报告。重新导入去重；已完成正文不会被缺失/损坏/未完成报告覆盖，已知较早正文也不会覆盖较新正文；所有写入在现有导入事务中，失败/取消回滚。删除本地档案会删除报告，不修改在线登录、浏览器数据或外部已保存文件。

缺失/格式不支持/不唯一的同名文件明确不可用；未完成报告明确尚无完成正文。坏 report JSON 不被当正文；坏 inventory、重复身份、超限或取消仍是必需失败，不静默截断。

## 有界与验证边界

沿用现有 ZIP 检查、路径/重复名/CRC/UTF-8/Gson STRICT/词法字符串和深度限制、300秒协作取消、数据库空间限制。新增 inventory/单报告4MiB、合计32MiB、128报告/import、32报告/conversation；完成消息UTF-8 1MiB、每会话持久化正文4Mi字符。未知/不支持格式不猜测。阻塞文件I/O和monitor仍只能协作取消，不承诺瞬时中断。

本地 JVM 原93+新增8=101测试通过，新增覆盖精确归属、无mapping报告、缺失/坏JSON/不唯一、pending/hidden/incomplete、重复身份/版本、数量预算、三格式/分支独立性与取消。初次新增测试暴露坏report JSON整体失败和空ZIP取消漏检，已修正后101全部通过；失败历史在交接保留。

新增Android原生4项包括SQLite重复导入/源文件删除重开、schema2迁移、坏重导入保留/超限与取消事务回滚、真实Reader入口/重建/离线设置。原Archive实际HTML/MD/System Print PDF测试附带虚构报告并检查首尾/代码/表格及思考排除，原旧断言不删除。新Android/CI尚未执行完，不提前写通过。真实手机报告恢复仍需新包安装、重新导入与用户核对；本地源正文恢复不是手机恢复结论。
