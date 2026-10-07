> 历史记录：旧滚动/Gecko试用方案已从正式入口退役。当前保存路线为普通HTML与Markdown同一次clone，PDF使用系统WebView打印同HTML；实际最新状态见docs/handoff/latest.md和docs/frozen-page-save.md。以下原证据保留，不代表当前产品行为。

# 验证结论与风险表

## 单向版本已验证并直接提供：0df561e / 1.4.0

用户最新要求为“到顶端或底部来一遍，直接一边过”，取代强制上下与第二轮方案。[CI37478242887](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37478242887)，最终源码 `0df561e822e4c3efe8c95f20140e71d60bae80ff`，构建/lint/原签名、Chromium17单快照+29滚动场景、Android35/36各15项全部通过，publish skipped。默认只单向一遍：在顶部向下、在底部向上、中间开始先定位到底部再向上，到另一端结束。完成恢复原始像素位置后不继续采集。不会出现自动第二轮或把secondPass标true。

本批提高步幅并保留相邻ID重叠、有界缩步重试、加载等待、正文/顺序冲突和取消限额。独立下载两份证据核对 `OK (15 tests)`，含顶部单向/无第二轮/恢复、3节点窄虚拟窗口保存40条、8秒加载等待后保存、原取消/SAF/打印/脱敏。两个固定正文系统PDF均4页且含首尾/代码/表格：API35 140385 bytes/SHA256 `08eb9f55012f7790b815da75f396e1159b69f0a74a1f96342a3c151418d1c796`，API36 141748 bytes/SHA256 `d63c38e197746ecab923515389c3fa286938066ade649261e47b783d6b569302`。不是用户WebView153的实测速度或真实长会话PDF证据。

本批原签名APK 1889683 bytes/SHA256 `6ffff37b2ce2d71057d4b0017cc619386e8e49c6669c9f3f6f5a8e44acaa3f75`；版本仍1.4.0-scroll-trial/code14。独立核对版本/原证书记录、两份脚本字节与源码一致、DEX完整buildRevision等于0df561e、测试夹具未含于主包。按用户既有授权在会话提供主APK及仅主APK/校验值ZIP，不创建公开Release。备用：[GitHub构建产物](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37478242887/artifacts/11420236145)，需登录/解压，只安装ChatGPT-Nova.apk，不安装ChatGPT-Nova-tests.apk；有保留期限。

复测看方向是否保持单向并在端点结束、之前已核对的27条主消息是否仍齐全、耗时是否改善。诊断应为scheme DOM-SCROLL-TRIAL-2/buildRevision0df561e、traversal single-direction/plannedLegs1/secondPassfalse，可反馈steps与elapsedMs/overlapRetries，不必提供正文。用户此前27条正面覆盖反馈保留，下方旧四趟/待验叙述为历史；当前单向APK的真机覆盖/耗时待复测，完整性仍未确认。独立基准导入和附件备份仅完成设计，未实现新的产品入口。

## 用户确认27条主对话覆盖；本批改为单向一遍（验证与交付见上方）

用户提供新HTML解析/逐轮对照结果：27条（14用户、13助手），此前包名修正、增量提示词、转移包、GitHub连接、d143a79/bd2a6bd审查及M1等遗漏轮次已出现，普通用户消息/助手最终回复截至该导出时刻未发现大段丢失，顺序正常。工具执行进度未包含，附件元数据部分保存、附件/图片原文件未包含。此为用户提供的真实单样本正面覆盖观察；本环境没有HTML原件/独立基准或本次buildRevision，不记成本环境独立解析，不推广成所有长会话完整或100%备份。

用户进一步明确否决“一次上行一次下行”和第二轮，要求单向扫一遍到另一端就结束。当前实现因此去除所有自动折返/第二轮：底部起点向上、顶部起点向下、中间起点先定位到底部再向上，完成/取消仍尝试恢复原始像素位置（完成时回原位置不代表继续扫描）。覆盖数据固定plannedLegs=1/secondPass=false，direction和traversal=single-direction明确方向，输出说明不再声称两轮核对；history/text仍not-proven。

每次推进从0.45提高至0.8窗口；相邻稳定窗口必须共享至少一个消息ID，否则按上次命令步幅减半回退重试，最多5次后拒绝H03，不把断开的窗口缓存成成功。仅为保留重叠的小幅重试，不是全程折返核对。缓存正文/角色冲突、唯一顺序、加载等待和限额/取消保留；正常轮询250→120ms，加载等待400ms，不缩短历史加载的30秒硬限或增加数据源hook。UI显示单向方向、加载等待、定位起点和缩小步幅重试。

本地Chromium17单快照+29滚动场景通过，包括顶部向下/底部向上/中间定位与恢复、窄3条虚拟窗口缩步后有序40条、慢加载、取消、冲突及初始布局30秒截止。新增负例：一条消息永不挂载，单向扫描可完成39条且观察两端，仍明确not-proven；这证明端点/缓存条数不是独立完整性校验。初次窄窗口测试直接删末4节点实际使末4永不挂载而得到36条，已修正为真正覆盖40条的3节点虚拟窗口；未以放宽完整性断言掩盖该输入限制。

只读同一40条夹具操作成本对照：已交付7216e92四趟56滚动/197采样/28上行+28下行；本批单向8滚动/32采样/8上行+0下行，均缓存40条。仅夹具操作次数，不是用户设备实测提速百分比或实际耗时。Android35/36各15项已在本批最终CI通过，新增顶部单向状态/恢复和窄窗口HTML保存；源码/最终包以CI提交为准。版本仍1.4.0/code14，无新增Release；已通过必要检查并直接提供本批测试包。[独立ID/附件边界设计](../../docs/export/independent-validation.md)不包含基准导入/附件打包的新产品实现。

## 本批已验证并直接提供测试包：7216e92 / 1.4.0（不创建 Release）

[CI37464896999](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37464896999)最终源码 `7216e924f10eb41a1839cdb657e8fd4a8068c585`，build/lint/原签名、Chromium17单快照+25滚动场景、Android35/36各13项全部通过；publish skipped。先前37464449161、37464495282、37464530438在本批测试计数/步数预算修正时被取消，不能记作完整通过。

已独立下载两份仪器证据核对 `OK (13 tests)`，包含新增8秒历史加载等待后HTML保存40条/首尾/位置恢复、持续正文变化仍H06且脱敏、布局抖动、滚动缓存、取消、SAF/打印。固定正文系统PDF均4页且有首尾/代码/表格标记：API35 140385 bytes/SHA256 `82496ea4acda2730aa897c700f065b9920d928e8b50af5becfa1dde975e9e90b`；API36 141748 bytes/SHA256 `d63e1badd9ba3e564c565efc27188d43c2b9db15dbb8ff75d620b521e3c5564b`。全部是受控夹具，不是真实账号历史或用户WebView153验证。

本批APK 1889055 bytes/SHA256 `cfdfdba75a798d4782cdfedb55b954bc1bb22e33ac5e59619fd7844663fdd382`。独立核对版本仍1.4.0-scroll-trial/code14、原证书记录、两份导出脚本与源码字节相同、DEX中的完整buildRevision=7216e924f10eb41a1839cdb657e8fd4a8068c585、测试夹具未包含。依照用户明确要求，在会话提供主APK及主APK/校验值ZIP，可覆盖安装；不升级版本、不创建公开Release。[GitHub构建产物备用下载](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37464896999/artifacts/11414128595)需登录/解压，仅安装ChatGPT-Nova.apk，不安装ChatGPT-Nova-tests.apk；产物到期2027-01-04。旧224dae2包不含本批修正。

手机复测：原H06会话等待回复结束→导出聊天历史（试用）→滚动收集历史，保持前台且不操作页面；加载时应显示等待状态。失败复制诊断，应为scheme DOM-SCROLL-TRIAL-2/buildRevision7216e92；loadingSignals/loadingPolls/loadingObserved与readyAgeMs可区分识别到加载和列表换入，单窗口30秒、整次10分钟/1200步有界。成功后逐项核对此前遗漏的中间消息。用户原会话修复与完整历史仍未确认，不能宣布完整功能验证完成或旧403修好。

## 224dae2 真机再次 H06：历史加载等待修正（已通过 Android CI，见上方交付）

用户安装224dae2/DOM-SCROLL-TRIAL-2在Android36、WebView153.0.8010.36仍H06：86.537秒、leg0、68步、缓存19条（10用户/9助手），未到顶部/底部、未二次核对。最后窗口5.382秒仅6次采样，列表变化4次、正文变化1次、高度变化4次、contentStable=1；用户观察到转圈未结束即中断。旧诊断无加载指示字段，不能断言真实转圈的DOM形态或唯一根因，也不能把前一APK说成已经修好。

源码确认固定5秒截止不识别历史加载。已独立对比旧224dae2与本修正：5.382秒才换入列表，旧报H06，新继续等待。新逻辑仅检查所选滚动区域内可见的公开aria-busy/progressbar/loading-spinner/animate-spin，正文装饰旋转、隐藏或区域外指示不算；加载时不缓存、不移动、不确认边界，消失后重新核对。无已识别指示时，消息ID/角色列表换入也给予新的5秒准备宽限；单纯同ID正文变化不延长宽限，缓存正文/角色冲突仍拒绝。

每个滚动窗口固定30秒总上限（列表反复换入/转圈不能无限延长），边界稳定核对也在该上限内；整次上限调整为10分钟，避免旧120秒在已经86秒却仍第一轮的真实反馈中提前截断两轮扫描。本次真机scrollMax34221/viewport747按0.45窗口估算四次遍历约408步，旧300步也不足，步数上限同步改为1200；1000消息、正文/字节限额、15秒单次回调、取消/清理不变。新增loadingSignals/loadingPolls/loadingObserved、readyAgeMs/windowLimitMs/scanLimitMs/stepLimit诊断及“网页正在加载历史”界面提示。指示选择器是有限兼容尝试，不保证识别真实网页所有转圈；完整历史始终not-proven。

本地Chromium17单快照+25滚动场景通过：实际6.5秒转圈后40条核对/位置恢复、无转圈的迟到列表宽限、永久转圈/不断换入在30秒拒绝、取消清理、装饰/隐藏/区域外指示忽略；旧正文持续变化和顺序/二次核对拒绝仍通过。新增Android8秒延迟加载→HTML保存40条测试，Android35/36各13项已在最终CI通过，证据见上方；不当作真实历史成功。版本保持1.4.0/code14，scheme2，通过buildRevision区分；只做CI验证，不自动Release，本修正现已按用户授权直接提供同版本测试包，下一步仍是用户原会话复测。

当前真实失败：1.4.0 H06，缓存10条/49步且未到顶。本轮已独立复现并修正布局像素变化误触发正文稳定门槛，增加脱敏变化分类和buildRevision。保持1.4.0不增版本，取消push自动发布；Chromium17单次/18滚动场景及Android35/36各12项、构建/lint/原签名检查在CI37461566300通过，独立核对APK仍1.4.0/code14及buildRevision、两份系统PDF。publish跳过，源码224dae2未发布；边界与几何容差已统一并做持续抖动回归。详见 [同版修正记录](../../docs/dom-export-trial.md)，用户原会话未复测，修正未发布。下方已交付信息为上一批。

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
