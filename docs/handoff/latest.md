# ChatGPT Nova 会话交接

用户实测已发布1.4.0在Android36/WebView153返回H06_UNSETTLED：61.23秒，向上第一轮leg0、49滚动步、缓存10条（5用户/5助手，12573字符），未观察到顶部/底部、未开始第二轮。不能从旧诊断确定根因。本地已复现正文/ID不变、仅滚动高度2px变化使旧版误报H06；修正采用三次相同正文快照即可缓存，边界另按2px容差连续核对，边界持续变化仍拒绝。新增settling分类/计数/窗口年龄/几何数据（不含正文ID），scheme为DOM-SCROLL-TRIAL-2，buildRevision记录构建提交。版本号保持1.4.0/code14，修正尚未发布，旧已安装包不包含本修正。Chromium17原采集+17滚动场景本地通过，Android35/36各12项待CI（新增真实布局抖动保存40条及真实正文变化拒绝/脱敏）。用户要求集中修复/功能后再交付，取消每次push自动发布，仅显式workflow_dispatch publish_trial=true才发布；本轮先验证提交，不新增Release。真实原会话仍待后续集中测试包复测，完整历史未证明。

当前用户授权可安装滚动历史测试。已接入1.4.0/code14滚动缓存、两轮上下核对、稳定ID合并/顺序冲突拒绝、取消/清理、覆盖结果与DOM-SCROLL-TRIAL-1诊断；完整历史仍未证明。Chromium17单快照+14滚动合成场景本地通过，Android35/36各10项、构建/lint/原签名与发布在工作流37457721588全部通过，已独立下载核对APK及两份4页PDF。源提交5cdb631，1.4.0已签名交付，永久下载和SHA256见试用说明。用户原会话仍待复测，完整历史未证明。当前采用页面临时内存缓存（完成/失败/取消清理），不是完整生产实现；APK与证据以试用说明为准。下方“尚未接入APK”是上一阶段记录。

最新历史覆盖反馈：用户转述GPT-5.6 sol对HTML的检查为7条消息（3用户/4助手）且缺多段已知历史；本环境没有HTML字节，未独立核文件或用户页面。源码确认只做单次DOM采集与同快照签名复核，无滚动缓存。新增 [历史完整性设计](../export/history-integrity.md) 与生产脚本限制合成复现：40条基准仅挂载7条时导出/复核成功但缺33条，虚拟窗口并集不能证明完整，ID匹配不核正文；3场景及既有audit测试通过。设计采用有界上下滚动缓存、稳定ID合并、顺序/正文冲突拒绝、独立基准核对及四维状态；到顶/无新增不允许标完整。审查设计已完成，滚动采集尚未接入APK，真实完整历史未验证。下一步可做有限滚动采集试验，不能承诺完整导出。此前D09修正不能补出未挂载历史。

首次生成：2026-10-05；入库更新：2026-10-06。当前交接优先于历史资料中的旧任务进度。

最新修正：用户实测1.3.8在Android36/WebView153.0.8010.36普通会话返回D09_EMPTY_BODY，count5、无缺失/重复ID、codeBlocks2；确认全是文字（可能含代码）。旧诊断无失败节点位置，因此未证明具体根因。已复现旧布局框过滤对display:contents误报D09，1.3.9/code13改为自身/祖先隐藏检查，保留隐藏与控件过滤；空markdown可在同一author结构化过滤后取备用正文，真空正文仍失败不跳过。新诊断DOM-TRIAL-2记录失败消息序号/角色及过滤布局计数，不读正文进诊断。本地及CI Chromium17合成场景通过，工作流37454082614构建/lint/原签名、Android35/36各7项及发布均成功，独立核对两份4页系统PDF与新版APK。源提交f2d0cce；新版永久下载与SHA见试用说明；用户原会话仍需复测。read-only-dom是采集方式，不是失败状态，完整历史仍未证明。详见试用说明顶部。

最新范围变更：用户明确要求“做一个用新方案的版本让我用用，记得加报错诊断”。已授权本轮实现并交付工作分支试用APK；旧“只评估、不改生产源码”的约束只对应上一阶段。试用版仍不承诺完整历史。当前实现与验证状态以 [试用说明](../dom-export-trial.md) 和本次DOM试用工作流为准；不是会话迁移，不附下一会话提示词。

前一试用版交付记录：实现提交 `895d32b12a90db872e2faa175fe6751b7437fc4a`，版本1.3.8-dom-trial/code12，原包名和原证书签名。永久APK：[nova-dom-trial-895d32b](https://github.com/DevenirTwilight/ChatGPT-Nova/releases/download/nova-dom-trial-895d32b/ChatGPT-Nova.apk)。已停用旧捕获器，菜单导出已加载消息与复制诊断；客户端后台转换、SAF与独立PrintManager WebView。

本轮实际验证：Chromium10合成场景；GitHub构建/lint/原证书核对；Android35五项导出夹具全部通过，包含SAF写入、Markdown取消重试/分享、诊断复制脱敏、PDF取消与实际系统保存。独立下载证据核实PDF4页A4及首尾/代码/表格标记，APK字节及SHA256见试用说明。首次PDF保存失败保留在run37450098092，改为完整页写出后run37451344432通过；中途不可构造打印结果回调的编译错误已纠正，未使用隐藏API。真实账号/用户设备和完整历史仍未验证，不把本次夹具结果当成旧403修复或全量输入/登录回归。下一步由用户安装并反馈诊断及历史覆盖现象，不重复要求其先搭建ADB。

用户澄清：不是每段对话都要生成下一会话提示词。仅在压缩上下文、迁移会话或用户明确要求时提供；普通进度回复应直接说明验证是否完成、是否可以开始实现。此澄清优先于下方历史交接措辞。

此前可行性状态：验证未完成，尚不具备按“完整项目长会话导出”目标开始完整功能实现的条件；用户随后授权有限范围试用版本（见上方）。静态集成与合成工具检查已完成；真实项目长会话完整覆盖、正文/富文本保真、新Android PDF实际保存、SAF及回归仍未验证，旧403未确认修复。可以继续最小验证接入；不能因此宣布方案已验证可行或自动开始生产实现。

## 2026-10-06 续接更新（本节优先于下方旧环境记录）

- 按用户要求先读取 AGENTS、latest、REVIEW、REPORT、根 README，另读取 handoff/README 与探针 README。拉取远端 `feature/export-conversation`，HEAD 为 `fc269792ecef68fe2e5930105011deaeafba3591`，没有更晚提交。下方 `5e969c3` 是上一阶段验证基线，不能当作本轮 HEAD；本轮提交号以 GitHub 分支日志为准。
- 当前只做 Markdown/HTML/PDF 可行性，不写完整功能、不改生产源码，允许重做。最高优先级仍是真实项目长会话历史覆盖。
- 用户表示可连接运行 Nova 的 Android 设备并登录目标会话；追问连接方式/安装提交后回答“未知，最新版”。这不是已经建立 ADB 连接，也不能认定 APK 对应哪个提交。当前云环境未发现 adb、连接设备或 KVM；没有访问真实账号。未执行真机验证，未修复/重测旧403。
- 静态发现：原 dom-probe 内部最多检查300条，但返回仅首尾四条 samples，不能逐条核对中间历史。feature 的 ConversationExport 使用 document-start 注入并在 pageFinished 再注入旧 capture；真实新路线测试必须区分隔离基线和带旧捕获器的 feature。
- 新增独立验证目录 `tools/feasibility/history-coverage/`：只读项目路由探针输出全量已检查 ID/角色（最多2000，超限标记），离线 audit 按独立有序基准检查逐条覆盖。合格单快照 ID 匹配、仅并集匹配、缺失与无效采样分开；所有结果仍是 `historyCompleteness=not-proven`，正文保真未测试。基准来源自述不能由工具证明，禁止从采样并集生成所谓独立基准。
- 本轮独立执行 `audit.test.cjs` 和 Chromium `probe.test.cjs` 均通过，`git diff --check` 通过。夹具覆盖中间缺失、稳定末尾、并集、乱序、错误路由、角色/ID异常、嵌套、未就绪、2001节点截断、只读及不返回正文。全部属于合成检查。本轮没有重跑旧 DOM9/9、Java编译、PDF或APK验证。
- 下一步：用户在本地电脑建立 USB ADB（无需公网端口），记录 Android/WebView/APK版本与安装来源；在指定隔离 debug 工作树启用仅debug的 WebView调试并自行构建登录，通过 DevTools 保存项目会话底部→顶部→底部及重复采样。操作步骤与最小调试代码在新目录 README。旧 Java Logcat 可能截断长 JSON，不能当作逐条记录传输。
- 取得独立完整目标分支基准后运行 audit；没有基准只能记录“观察到的覆盖”。ID匹配仍需逐条正文/富文本核验与Android真实保存；真实历史缺失且无法可靠加载全部时，更换数据来源。暂不扩大格式/PDF实现。
- 真实路径、消息ID与正文记录仅本地保留，不提交到公开仓库。可以提交脱敏结果摘要，但勿把假数据记成实测。

新工具及操作入口：[项目历史覆盖验证](../../tools/feasibility/history-coverage/README.md)。本轮阶段交接将提交并推送当前工作分支，不强推、不合并、不发布APK。

## 用户要求与沟通偏好

用户使用中文。希望拿到验证好的结果，曾明确说“给我验证好了再发”；不要用合成测试或编译成功代替真实会话成功。之前反复 debug 失败，正在考虑重做导出功能。

当前目标：验证通过已有 WebView 注入 JS，读取 `[data-message-author-role]`、返回 JSON，客户端转换 Markdown/HTML、SAF 保存，PDF 用独立可渲染 WebView + PrintManager 的方案是否可行。

用户规定：
1. 先一次性问不超过八个问题，包括语言/minSdk、WebView 配置、加载及 Client、菜单工具栏、保存分享。
2. 收到回答后检查登录/config、注入时机与 Client、evaluateJavascript 线程、PrintManager Activity/主题、SAF/权限/存储、页面 React/已有脚本的冲突。
3. 输出风险点/等级/最小验证/判断标准/Plan B 表，按验证成本从低到高；对最高风险的一到两项给可直接接入的最小验证代码；指出失败即必须换路线的条件。
4. 本阶段不写完整功能代码；优先对现有类最小改动，不重构架构。搜索 GitHub 找到项目，以代码作背景，但允许推翻旧导出路线。

八项问题已经问过，覆盖仓库版本、设备语言、配置登录、Client、菜单、保存分享、完整历史及富文本范围、保留/重做行为。用户统一答：“从仓库核实，但不要被它束缚，可以重做”。设备 Android/WebView 版本、当前安装 APK 的精确提交、实际账号可访问性、是否接受仅已加载消息，仍不能从仓库证明；暂以完整当前分支为最高风险场景，不擅自降低范围。

最新动作：用户手机点击沙箱下载链接无响应，因此明确要求直接将交接写入 GitHub 仓库，并要求后续会话也按此方式更新交接。当前变更只包含交接文档、验证材料和交接约定；不是恢复旧功能开发。

## 仓库和版本，必须区分

- GitHub：https://github.com/DevenirTwilight/ChatGPT-Nova ，公开仓库。
- 本轮 GitHub 在线核实默认 main：`e8ffa0c6bd9af9ab29aecad50efe2ab6b8099a00`，版本 1.3.6/code10。
- 本地工作目录 `/workspace/ChatGPT-Nova`：分支 `feature/export-conversation`，HEAD `5e969c36fd2751f3bcd39500ee9ed1520394fa1d`，版本 1.3.7/code11，工作树干净。
- 可行性补丁指定的隔离基线：`fix/native-share-1.3.7` 的 `200b7898b51cdac9e342e49e7e7a7059067d944f`。为避免旧导出 fetch/React 捕获器干扰而选择，不能假定适用于 main 或当前 feature。
- 包内 source/ 提供当前 feature 与指定基线的源文件快照，无 .git 历史；若要使用基线补丁应解压到基线快照目录，先做应用检查。快照不是新实现。

## 已核实软件背景

- Java17；minSdk26，compileSdk/targetSdk35。
- 包名 `com.example.chatgptnova`；debug 后缀 `.debug`，与正式应用登录数据隔离。
- JS、DOM storage、数据库、第一方/第三方 Cookie 已开启；没有 UA 覆盖，使用系统默认 UA。
- 聊天 WebView：禁用 file access，允许 content access，禁止混合内容，SafeBrowsing 开启。
- `MainActivity.configureWebView()` 已安装 WebViewClient/WebChromeClient，负责导航、进度、错误、权限、上传等。不能新建 Client 覆盖已有 Client。
- `onPageStarted` 做导航和请求状态失效；`onPageFinished` 刷 Cookie、注入已有粘贴兼容和分享脚本。SPA 切页未必触发这些回调，不能只靠 onPageFinished 认定数据准备完毕。
- `ComposerWebView` 有输入/IME 事务及队列；采集不能加入编辑事务队列。WebView evaluateJavascript 调用和回调走 UI 线程；转换和文件 I/O 应后台处理。
- 菜单是 PopupMenu，界面主要在 Activity 程序化构建，已有工具栏。
- 已有 SAF ACTION_CREATE_DOCUMENT 下载，结果码 SAVE_BLOB=1005；上传 PICK_FILE=1001；分享代码段 0x7000..0x7fff。探针 SAF 用独立 0x6301。
- Manifest 有 INTERNET/RECORD_AUDIO/CAMERA，没有存储权限；已配置 FileProvider。SAF 不需要新增传统存储权限。
- WebShareAdapter 实现 origin-scoped WebMessage 的 navigator.share；保留网页分享和原生分享行为。
- AppTheme Material Light NoActionBar；启动 LaunchTheme 转 AppTheme。没有必须为打印改主题的依据；PrintManager 要有效 Activity 上下文。
- feature 分支已有 ConversationExport 和旧 capture.js：包装 fetch、发现网页内部模块、读取器和分页助手，另有 React Fiber 来源。这些行为需要与新只读 DOM 探针隔离。

## 用户真实失败记录（历史背景，尚未被真实账号证明解决）

项目内地址结构：`https://chatgpt.com/g/g-p-<project-id>/c/<conversation-id>`。

诊断逐步出现：
```
诊断 E2：模块=1，导入=1，读取器=1，状态=failed-403，接口=403，分页=failed-error，页数=0，阶段=discovery
```
之后阶段有 discovery-source、discovery-loaded-assets；页数=2 时出现 recheck、recheck-verify，最终原因 `head-metadata-changed`。

这些是旧完整读取/分页/复核路径失败，不等同于 DOM evaluateJavascript 不可用。项目参数修复、分页 replay 等曾做过，但用户真机导出成功没有证据。

用户还遇到 GitHub 链接在手机 GitHub App 打开、临时 Azure 签名下载 URL 过期 AuthenticationFailed。新交接使用上传本 ZIP；不要依赖过期 Actions artifact 的临时签名 URL。

## 已完成的可行性工作

用户提供三份材料：REPORT.md、README.md、GPT-Nova-feasibility.zip。独立报告与说明逐字节等于 ZIP 中对应文件。

已阅读 DOM 脚本、浏览器测试、Java DOM/PDF/SAF 探针、基线接入补丁、报告和接入说明。包内有最小接入补丁，生产源码没有应用补丁，本轮没有 push、发布或构建完整 APK。

本轮独立复核：
- 新 DOM Chromium 合成探针 9/9。
- Java 探针 API35 编译通过。
- 基线补丁在指定提交的临时 Git index 上 `git apply --cached --check` 通过，不改工作树。
- PDF 元数据确认15页A4、191980字节、Skia/PDF m151；文本检查找到第79条消息。
- 当前生产仓库 git status 干净。

材料报告记录而本轮未重新执行：core33/33、pagination83/83、paged-browser41/41、capture与格式浏览器测试，Java API26 编译。原报告注明 file:// 被环境阻止，使用受控 HTTPS 提供相同文档通过；不能声称 file:// 验证通过。

仍未验证：新方案在真实登录会话/目标 Android WebView 上的 DOM 覆盖；新 PDF 探针的 Android 系统保存、取消和生命周期；新 SAF 探针/提供者；本轮完整 APK 构建和真实输入/分享/登录回归。

旧导出实现历史上有模拟器系统 PDF、签名 APK 和仪器测试，见 history/。这不代表新探针已通过，也不证明真实账号导出成功。

## 关键结论与不能夸大的边界

1. Android 集成条件具备，可以重做；首要问题是数据覆盖，不是先重构 Activity。
2. DOM 探针始终返回 `historyCompleteness=not-proven`。数量/首尾/签名稳定不是完整性证明；等量替换可提示虚拟化。滚动收集所有“见过”的消息也不能单独证明无遗漏。
3. 完整历史是硬要求，而 DOM 缺历史且不能可靠加载全量时，单次 DOM 路线必须更换数据来源。可考虑用户提供的官方导出数据或其他可实际验证完整的来源，不能默认内部 API 能用。
4. 渲染 DOM 不能唯一恢复原始 Markdown 写法；可以生成内容等价 Markdown。必需的隐藏正文/附件不在可读表示中时，同样要换来源或经用户明确调整范围。
5. PrintManager 交互是系统“保存为 PDF”；不等于应用指定 SAF URI 后静默生成 PDF。若要求直接生成再 SAF 保存，应另验证 PDF 引擎。
6. PrintDocumentAdapter.onFinish 只表示生命周期结束，不证明用户保存成功；需要实际文件。
7. 新探针只读 DOM，不读 Cookie/Token/storage，不包装 fetch，不读 React Fiber。结构计数不证明正文/公式/附件正确还原；textContent 可能包含控件和隐藏文字。
8. 探针节点300、单条指纹128000字符、总指纹500000字符，超限 bounded=true。textContent 分配整条字符串，限制不是硬内存上限；指纹非加密摘要。
9. 导航保护仍有空隙：Java 比较 URL/WebView/当前代次；补丁代次只在 onPageStarted 递增，不能覆盖所有 SPA A→B→A 迟到回调。生产采集需另外验证内容/路由前后的一致性，不可把探针说成已经完备。
10. SAF 写入与回读应分别判断；某提供者回读失败不必然意味着写入失败。

## 下一会话建议顺序

先阅读 REVIEW 和 REPORT，保留上述证据分级。若用户仍要求只评估，不自动应用补丁、开发功能或发布 APK。

优先验证目标账号的短会话选择器，再验证普通与项目内长会话：底部采样、顶部等待历史加载采样、回到底部采样，逐条核对已知消息/分支/首尾；覆盖重新生成、编辑、流式更新、SPA 切换。未证明覆盖前，不扩大格式/PDF实现。

若覆盖满足范围，再验证固定 HTML 的 Android PrintManager 真实输出、SAF创建/取消/不同提供者及旧上传下载回调，最后验证输入、粘贴、网页/原生分享、上传、语音、登录持久化和销毁。

没有真机/账号时可以推进静态审查、合成探针或独立测试构建（需用户当前任务允许），但明确缺少真实证据。此前八项已问，不再反复问仓库事实；只询问不能从代码核实且影响下一步的信息。

## 历史旧功能，供追踪而非本阶段目标

旧实现 APK 提交 `3fced4a6ce97915a20b3deb32892b0cc950f94d4`；最终测试提交当前 HEAD。Android CI run37336090843，API33/34/35 导出目标各11/11，通过合成完整400条 replay及隐藏祖先变化拒绝。整体工作流仍失败：API33旧输入性能门槛失败5613.8ms，对照3455.6ms，未修改输入实现或放宽标准。

旧 release：https://github.com/DevenirTwilight/ChatGPT-Nova/releases/download/nova-export-test-3fced4a/ChatGPT-Nova.apk 。APK SHA256 `7ba618b29cc6b508af011c2f4e94b76626cb37479692dfa649963c7252601986`；原证书 SHA256 `f93221ee0d2be2b806233a0b3427ec9c14766100c1bab3208841e6203e2b4289`。包名正式、1.3.7/code11。链接存在性本轮未重查；不是新方案交付，未附 APK。

旧方案尝试额外元数据漂移时独立再遍历和第三次 head guard，要求全文逐条一致，保留 title/update_time/moderation/safe_urls/blocked_urls 等严格检查。仍无真实账号成功证据。不要因为旧模拟器测试通过就继续无限修补内部网页接口。

## 仓库续接入口（优先使用）

当前交接文件：`docs/handoff/latest.md`；证据复核：`docs/handoff/REVIEW.md`；后续交接约定与提示词：`docs/handoff/README.md`；探针、报告和基线最小补丁：`tools/feasibility/`。这些文件位于 `feature/export-conversation` 分支。新会话应先读取对应分支，而不是默认 main。包内 source 快照、originals 等描述属于先前 ZIP；GitHub 续接直接使用提交历史中的对应源码，不需要下载 ZIP。

新增用户偏好：实际需要交接时，更新上述仓库文件并提交、推送至当前工作分支，返回 GitHub 链接和下一会话提示词，不能只提供 sandbox 下载链接。普通回复不生成迁移提示词。

## 可移植材料与环境

包内含两份源码快照、新探针与最小补丁、原始验证 ZIP/文档、HTML/Markdown/PDF样例、历史说明和 SHA256 清单。没有签名私钥、用户 Cookie/Token、node_modules、Android SDK、.git 或 APK。

前一环境有 /usr/bin/chromium、Node24、playwright-core；Java编译器 `/tmp/nova-build-env/jdk17/bin/javac`，API35 android.jar 在 `/tmp/nova-build-env/android-sdk/platforms/android-35/android.jar`。这些绝对路径不保证在新会话存在；代码材料在本包内，不依赖旧 workspace。

用户原上传 file IDs/旧 workspace 路径可能跨会话不可用，优先使用本 ZIP 自带的 originals/ 和 feasibility/。
