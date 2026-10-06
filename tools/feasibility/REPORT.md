# 验证结论与风险表

当前试用交付：用户授权1.4.0/code14滚动缓存试用，源提交5cdb631，原证书签名APK已发布。Chromium17单快照/14滚动场景、Android35/36各10项、构建/lint/签名均通过；虚拟化夹具只挂载7条但实际SAF保存HTML含40条，取消/重试/缺ID诊断通过，PDF实物独立核对。工作流37457721588及下载证据见 [试用说明](../../docs/dom-export-trial.md)。两轮一致/观察到顶仍非完整历史证明，真实用户原长会话待复测；不修复或重测旧403，不包含附件/图片原文件。下方“未接入APK”等为之前阶段记录。

本轮源码审查确认单次DOM无滚动缓存；用户转述7条导出大段遗漏，未提供文件供本环境独立解析。设计与独立合成复现见 [历史完整性设计](../../docs/export/history-integrity.md)：3场景及原audit通过，滚动方案尚未接入APK，真实完整历史验证仍未完成。可开始有界采集试验，不宣布完整方案可行。

修正版状态：1.3.9-dom-trial/code13，源f2d0cce已签名交付；Chromium17场景、构建/lint/签名、Android35/36各7项及系统保存PDF独立核对通过。用户旧1.3.8真机D09为已知失败证据，新版针对合成复现的布局框误过滤修正且增加消息定位，用户原会话仍待复测。read-only-dom不是失败码。完整项目长会话覆盖仍未证明，生产完整功能的可行性验证未完成。详见 [最新试用证据](../../docs/dom-export-trial.md)。下方状态为此前阶段。

最新阶段：用户随后授权制作新方案试用APK。已发布原证书签名的1.3.8-dom-trial/code12，源码895d32b；构建/lint/签名、Chromium10场景与Android35五项夹具通过，系统实际保存4页PDF并核对首尾/代码/表格文字。详见 [试用说明和证据](../../docs/dom-export-trial.md)。真实完整历史仍未证明；以下“未修改生产源码”等描述属于此前可行性阶段。

## 2026-10-06 续接补充

本轮起点为远端 `fc26979`，生产源码未改。新增 [项目长会话逐条覆盖核对](history-coverage/README.md)：旧探针只输出首尾四条，不能核对中间遗漏；新独立探针输出最多2000条ID/角色，离线audit与独立有序目标分支基准比较。单次匹配与滚动并集匹配分开，完整性始终未证明，正文未测试。

本轮执行 `history-coverage/audit.test.cjs`、`history-coverage/probe.test.cjs`，均为合成检查且通过。没有执行下方历史测试。本轮未连接Android设备、未访问目标会话，用户安装提交未知；旧403仍未被真实账号复测。设备本地USB连接、隔离debug采样和独立基准是下一步依赖，不把“最新版”当作commit证明。

以下为2026-10-05历史报告，保留原范围与边界。

验证日期：2026-10-05 UTC。

结论：Android 集成条件具备；DOM 路线可以验证“已加载消息”，但目前没有真实会话证据证明它能获取完整历史。
重做应首先隔离旧捕获器，并验证数据来源，不需要先重构 Activity、输入或保存架构。

## 基线与实际范围

- 仓库：DevenirTwilight/ChatGPT-Nova。
- 检出：feature/export-conversation，`5e969c36fd2751f3bcd39500ee9ed1520394fa1d`。
- 文档规定的产品基线：fix/native-share-1.3.7，`200b7898b51cdac9e342e49e7e7a7059067d944f`。
- 新文件仅位于 tools/feasibility；没有修改生产源码，没有 push、部署或发布 APK。
- 生成独立基线接入补丁，已通过干净基线 `git apply --check`；没有将补丁应用到生产分支。

语言 Java17，minSdk26，targetSdk/compileSdk35。默认 UA；JS、DOM storage、第三方 Cookie 已开启。
ComposerWebView 负责已有 IME 事务，Activity 主线程与输入 Handler 之间已有队列；提取不能混入该编辑队列。
菜单为 PopupMenu，SAF 下载已存在；Manifest 无存储权限。
WebShareAdapter 使用 origin-scoped WebMessage 适配 navigator.share，需保留。
既有主题为 Material Light NoActionBar，启动主题转到 AppTheme。PrintManager 需要有效 Activity，未发现必须更换主题的依据。

ComposerWebView 与产品基线 SHA256 相同：
`04809c754f668a24dd5186337bd77895598ebe908d630b42d9183767b65beb02`。
WebShareAdapter 与产品基线 SHA256 相同：
`d37e4c426ad7116432ee1fa76721d7c1f4b014be7737ac09ace86e7b294b37c7`。

旧 capture.js 包装 window.fetch，尝试导入网页内部读取器、分页助手，并有 React Fiber 备用来源。
仓库记录中的 full 403、分页 recheck 和 head-metadata-changed 属于这些读取/完整性路径；不是本轮观察到的 DOM 注入失败。

## 本轮实际结果

| 检查 | 结果 | 证明边界 |
| --- | --- | --- |
| core.test.cjs | 33/33 | 合成消息树规范化与格式逻辑 |
| pagination.test.cjs | 83/83 | 合成分页、复核、双遍历完整性逻辑 |
| capture.cjs | 通过 | 夹具中内部读取器、路由、超时和失败处理；不证明真实账号有权限 |
| paged-capture.cjs | 41/41 场景 | 夹具中分页与 403 备用逻辑；不证明真实端点可访问 |
| browser.cjs | 受控地址模式通过 | 净化、320/390/844/1280px、深浅色、格式与桌面 PDF |
| browser.cjs 的 file:// 模式 | 环境阻止 | ERR_BLOCKED_BY_ADMINISTRATOR；本轮未验证本地文件直接打开 |
| 新 DOM 探针检查 | 9/9 | 模拟虚拟化、同 ID 正文变化、缺失/重复 ID、边界限制、结构检测、只读行为和 origin 拒绝 |
| 独立 Java 探针编译 | API26/API35 均通过 | Android API 符号检查；不是 APK 构建或运行 |
| 基线接入补丁 | apply --check 通过 | 补丁可应用到指定基线；未验证整个 APK 构建 |
| 桌面 PDF 实物 | 15 页 A4，191980 bytes，Skia/PDF m151 | pdftotext 检出首条、第79条和最后富文本代码；无其他分支正文；不证明 Android PrintManager |
| 真机 DOM / Android PDF / SAF / 输入回归 | 未执行 | 无真实登录账号、连接设备、adb 或 KVM |

浏览器 file:// 限制通过仓库已有 NOVA_OFFLINE_DOCUMENT_URL 模式处理：以受控 HTTPS 地址提供同一生成文档，其他请求仍被阻止。
保留原始失败边界，不把受控地址测试当成 file:// 验证。

独立 Java 编译使用 JDK21 的编译器模块及 source/target17，出现一条 system modules path 提示，无源码编译错误。
不包含完整 SDK、Gradle 构建与 instrumentation 执行。Android API26 编译采用 android.jar 作为 classpath，不声称验证全部 Java 运行库兼容性。

DOM 探针不打印消息正文，只返回 ID、字符数、指纹与结构计数。指纹不是加密摘要或历史完整性证据。
节点上限300、单条指纹采样上限128000字符、总指纹采样上限500000字符，触及限制明确标 bounded。
textContent 本身仍可能创建整条消息字符串，上述边界限制采样工作，不是 DOM 字符串分配的硬内存上限。
historyCompleteness 始终是 not-proven。

## 风险：按最小验证成本从低到高

| 风险点 | 等级 | 最小验证方法 | 判断标准 | 失败时 Plan B |
| --- | --- | --- | --- | --- |
| 配置影响登录/页面，静态核查 | 低 | 沿用现有 UA、JS、storage、Cookie 配置 | 提取无需更改配置；打印配置仅作用于独立 WebView | 调整独立打印 WebView；不为提取更换聊天 WebView |
| 选择器与可读正文，5分钟 | 高 | 真机短会话运行 DOM 探针 | 数量、角色、首尾 ID 正确，所需正文结构存在 | 调整公开 DOM 选择器；仍无正文则换来源 |
| evaluateJavascript 与线程，5分钟 | 中 | 检查 callbackMain 与耗时，同时输入/粘贴 | UI 线程调用和回调；不阻塞、不加入 IME 事务队列 | UI 只采集，后台转换/写入，限制返回结果 |
| 注入时机、SPA 与迟到回调，10分钟 | 中高 | 切页、重新生成、流式更新 | 点击时查询；导航后旧结果丢弃；转换前后内容一致 | URL/DOM 签名复核，保留现有 Client；不只靠 onPageFinished |
| SAF 权限/结果冲突，10分钟 | 低 | 独立 requestCode 探针创建、取消、回读 | UTF-8 roundTrip=true，原上传/下载正常 | 单独待保存状态和结果分支；必要时使用缓存+FileProvider |
| 完整历史是否都在 DOM，15–30分钟 | 最高 | 长会话顶部/底部比较，核对全部已知消息 | 所需历史全部存在；没有仅末尾或随滚动替换 | 允许导出已加载消息，或切换可证明完整的数据来源 |
| 富文本及 Markdown 还原，20–30分钟 | 高 | 代码、表格、公式、引用、折叠正文、附件样例 | 结构和必需正文完整，不把控件当正文 | 转为结构化消息块；缺失源文时换数据来源 |
| PDF Activity/渲染/生命周期，20–30分钟 | 高 | 固定 HTML 系统打印探针，保存/取消/改纸张 | 实际 PDF 首尾、多页、代码/表格可读，返回后聊天正常 | 修复打印 WebView 生命周期；仍失败则换 PDF 引擎 |
| 大结果/内存/卡顿，30分钟 | 中高 | 长会话与长代码观察延迟和内存 | 无截断、ANR或输入卡顿；超限明确失败 | 分块传输/处理并明确预算；不把超限文件当成功 |
| React/输入/分享/登录回归，30–60分钟 | 中；沿用旧捕获器则高 | 在不安装旧导出脚本的基线运行探针并回归 | DOM/fetch/React不修改，既有操作和登录保持 | 一次性只读提取，保留 ComposerWebView/WebShareAdapter |

## 必须换路线的条件

1. 完整历史是硬要求，而 DOM 缺失历史且无法可靠加载全部内容：单次 DOM 提取方案必须换数据来源。滚动采集或缓存见过的消息并不能单独证明全部历史已收齐。
2. 要求精确的原始 Markdown，但只有渲染 DOM：必须换来源。只能生成内容等价 Markdown，无法恢复唯一原始写法。
3. 必须保存的正文/附件/图片不存在可读取表示：DOM 转换不能补出这些内容。换来源或明确调整产品范围。
4. 固定 HTML 在目标设备仍不能通过系统打印可靠保存：仅更换 PDF 路线，HTML/Markdown 可继续。

PrintManager 的常规公共交互是系统打印界面的“保存为 PDF”，不是应用指定 SAF URI 后直接获得 PDF。
如未来要求直接生成文件再 SAF 保存，需要独立验证支持该交互的 PDF 引擎；不把调用隐藏打印 API 当成默认 Plan B。

## 建议重做顺序

首先在产品基线运行最小探针，确定接受的内容范围和真实页面 DOM 覆盖。
若 DOM 足够，点击时一次性只读采集为结构化消息，再在后台转换 Markdown/净化 HTML；不要覆盖 fetch、依赖 CDN 内部函数名或 React Fiber。
在现有 MainActivity 的菜单、结果回调和释放逻辑中做最小接入；保留现有 WebView 配置和输入/分享组件。
若完整历史无法证明，停止扩展 DOM 导出，选择用户可提供的官方导出数据或另一个实际可验证的完整数据来源。
不默认旧内部读取器已经不可用，也不因合成测试通过就继续替真实账号权限问题打补丁。
