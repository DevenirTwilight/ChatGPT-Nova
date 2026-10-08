# 本地 Deep Research 正文恢复

## 已确认的源与归属

官方导出标准 `library_files.json` 中 `library_artifact_type=deep_research_report` 的记录，以 `origination_thread_id` 精确关联外层会话 official id，以 canonical `file_id` 精确关联 ZIP 中唯一同名 `.dat`。不依赖 filename-map、文件名称、正文相似度、ZIP 顺序或普通附件引用。`backing_conversation_id` 是研究任务的独立会话，不作为外层聊天归属。报告及 origination message id 可能都不在聊天 mapping，因此不推断精确聊天插入位置。

支持已观察的 JSON version 1：`widget_state.status=completed`，`widget_state.report_message` 的 author role=assistant、content type=text、metadata.is_complete=true，parts 为至少一个非空纯字符串。仅恢复这个明确的最终正文消息；不导入 activity_messages、thoughts、source_searches，也不将任意 widget 字段当报告。

本次授权输入在仓库外独立核对：标准 inventory 635 条中 19 条研究报告，用户指定会话关联 4 条完成报告，正文20797/24339/25569/26872字符，总97577。当前122个导入会话中仅这4份报告的外层thread精确匹配，另15条report的外层thread不在会话输入，不猜归属或称全部19已恢复。真实名称、会话/文件/消息标识、正文及二进制不进Git、CI或日志。

## 阅读与导出

默认会话阅读流现在按报告消息时间插入完整研究正文，并标注“按时间恢复位置”。HTML/Markdown/System Print PDF使用同一ArchiveTimeline。报告使用report_message.create_time，与chat create_time比较；不是推测最终完成时刻。原parent/branch selection和聊天次序保留，报告独立身份保留，不将显示位置写进原树。多个报告按时间和official identity稳定排序。

插入点必须同时满足左侧所有可见聊天时间≤报告、右侧所有可见聊天时间>报告；同时间报告放在同时间聊天之后。不要求整个分支单调，但缺chat/report时间或无一致cut时保留在末尾并标“位置未确定”。因此不是精确原气泡/分支关系复原。顶部“研究报告”仍是可选快捷阅读，默认正文已在聊天流，不需要先点按钮。

报告单独快捷视图按时间排列，HTML使用既有安全CommonMark，Markdown保留原文，PDF打印同一离线HTML。引用元数据保留，本次未增加citation restoration、来源网页抓取或外部链接认证。仅显示修正使用schema3已经保存的created/message，已有4份报告的用户覆盖安装即可；缺正文才需完整ZIP重导。

## 持久化与再导入

SQLite schema 3 非破坏新增 research_reports 表，按 conversation + official file identity 唯一；保存完成 report_message、标题/状态/时间以及 first/latest source。schema 1→3保留原聊天并补建资产表，schema 2→3保留聊天/资产并补报告表。升级应用不会自动取得旧ZIP，需手动重新导入一次补回报告。重新导入去重；已完成正文不会被缺失/损坏/未完成报告覆盖，已知较早正文也不会覆盖较新正文；所有写入在现有导入事务中，失败/取消回滚。删除本地档案会删除报告，不修改在线登录、浏览器数据或外部已保存文件。

缺失/格式不支持/不唯一的同名文件明确不可用；未完成报告明确尚无完成正文。坏 report JSON 不被当正文；坏 inventory、重复身份、超限或取消仍是必需失败，不静默截断。

## 有界与验证边界

沿用现有 ZIP 检查、路径/重复名/CRC/UTF-8/Gson STRICT/词法字符串和深度限制、300秒协作取消、数据库空间限制。新增 inventory/单报告4MiB、合计32MiB、128报告/import、32报告/conversation；完成消息UTF-8 1MiB、每会话持久化正文4Mi Java UTF-16单元（写入与读取一致，不使用SQLite码点LENGTH）。未知/不支持格式不猜测。阻塞文件I/O和monitor仍只能协作取消，不承诺瞬时中断。

本地 JVM 原93+新增8=101测试通过，新增覆盖精确归属、无mapping报告、缺失/坏JSON/不唯一、pending/hidden/incomplete、重复身份/版本、数量预算、三格式/分支独立性与取消。初次新增测试暴露坏report JSON整体失败和空ZIP取消漏检，已修正后101全部通过；失败历史在交接保留。

新增Android原生5项包括SQLite重复导入/源文件删除重开、schema2迁移、坏重导入保留/超限与取消事务回滚、补充平面Unicode aggregate超限回滚并保持旧31份可读正文、真实Reader入口/重建/离线设置。原Archive实际HTML/MD/System Print PDF测试附带虚构报告并检查首尾/代码/表格及思考排除，原旧断言不删除。最终源码15651dc正式37704403878与Legacy37704403572全部job通过；独立核JVM101、API26原生19、35/36各Archive39+Stable43及实际HTML/MD/17页Archive PDF报告首尾/代码/表格/旧markers与12×8图片像素，详tools/archive/evidence/research-1565-runtime-35/36.json。真实手机报告恢复仍需同签名新包覆盖安装、手动重新导入完整导出ZIP与用户核对；本地源正文恢复和fixture验收不是手机恢复结论。

## 最终测试包

实际生产源码15651dc5d0ea9289128c753900b6f65aef0de847，正式[APK artifact11518783109](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37704403878/artifacts/11518783109)，2110961bytes，SHA256217bf895a5fa285ef15ac38e1df37f99a5091faf6f63f40ece353df928e70923。原包com.example.chatgptnova、原certificate、code14/name1.4.0-scroll-trial保持；DEX revision1565/defaultLegacy=false独立验证，无公开Release。覆盖安装后手动重新导入包含library_files.json和matching .dat的完整ZIP，打开原会话“研究报告”入口查看；不能用缺少这些源文件的旧裁剪ZIP补回报告。

## 时间聊天流验证状态

用户明确选择按时间插入及位置标注。本轮本地107 JVM通过，新增6项位置/排序/缺时间/非单调/分支/原树保留回归；真实本地4份时间插入/0unknown/正文4逐字97577chars、顺序4→3→2→1/原树未变。新Android Reader与实际HTML/MD/PDF中间插入和原regressions待本轮CI验证，前一1565签名包验收是独立报告恢复基线，不是本次聊天流实现。
