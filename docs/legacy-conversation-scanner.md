# Legacy Conversation Scanner（Experimental）

## Step 1：真实调用链审查（2026-10-07）

远端基线：`da4176c5731ee07ea3c8815d56169abf1b143b25`，分支 `feature/export-conversation`；不是 main。

- 正式 popup 菜单只调用 PageSnapshotExport.start/showDiagnostic，没有 scanner 入口。
- configureWebView 无条件 new ConversationExport，读取包/WebView版本并初始化诊断。
- onPageStarted → navigationStarted；onPageFinished → pageFinished（空）；onPause/onResume → activityPaused/activityResumed（空）；onActivityResult → legacy SAF 路由；renderer gone/清除数据/onDestroy → destroy。
- pageFinished 不注入脚本。start/capture/startScroll/locateText 才 evaluateJavascript；默认正式 UI 没有路径到这些操作。旧 capture.js/run.js 的内部接口方案未被当前 controller 安装。
- DomTrialExportTest 反射取 MainActivity 的实例，16项仍执行，2项 Gecko 专用明确退役。tools/dom-trial 四组浏览器测试可独立执行。正式 dom-trial.yml 的 build 和 Android job 都仍运行 legacy，故无关正式提交也被旧 scanner 阻塞。
- scroll-trial 当前是单向一遍：顶部向下，底部向上，中间先到底部再向上；不是旧文档曾描述的两轮。保留 stable ID、相邻窗口 overlap/减步重试、拓扑顺序、role/text 冲突、稳定等待、H02/H03/H06、取消恢复位置和次数/时间/大小上限。
- dom-trial 保留严格 data-turn=assistant 的 article 或 conversation-turn 标记容器 fallback；progress-discovery 和 locate-text 返回结构计数。扫描只读公开 DOM，无 backend/React/storage/网络拦截。

## 三个独立产品层级

| 层级 | 数据源与用途 | 完整性 |
|---|---|---|
| Stable | 保存当前网页：FrozenPageSnapshot → 普通 HTML / Markdown / System Print PDF | 仅当前已加载 DOM，不证明服务器历史 |
| Archive（未来，未实现） | 用户主动导入官方 OpenAI Data Export → 本地搜索/阅读/重新导出 | 独立模块；本轮不实现，也不自动导入 |
| Experimental | Legacy Conversation Scanner：手动扫描当前打开的会话，累计公开 DOM 窗口 | 永远 `historyCompleteness = not-proven` |

## 它是什么、为什么保留

旧自动滚动与跨虚拟化窗口累计实验保留用于开发、个人实验、虚拟化研究、和官方导出文件人工对照，以及必要时尝试恢复较长内容。ChatGPT 主页面与 Share 页面存在 DOM 虚拟化；单次 clone、先滚完整页再 clone、Frozen Snapshot 均不能证明完整历史。累计不同窗口比单次快照可能获得更多消息，但没有证明能恢复任意真实长会话。

旧 29/30、frozen 7/~40、漏 assistant progress、Share 虚拟化、H06、39/40 fixture 与失败 CI 不改写、不删除；保留在 docs/dom-export-trial.md、docs/export/、tools/feasibility/、tools/dom-trial/、tools/snapshot/evidence/ 和 Git 历史。成功停止或稳定计数不等于没有遗漏。

## 默认禁用与显式启用

普通 release **和 debug** 均 `BuildConfig.ENABLE_LEGACY_SCANNER=false`。默认不构造 ConversationExport，无实验菜单/诊断、无相关脚本注入，也没有扫描的 pageFinished/resume hook。默认构建命令不变。只有显式 Gradle property 启用：

```sh
# 本地个人实验（现有 debug 应用后缀与签名策略不变）
./gradlew -PnovaLegacyScanner=true :app:assembleDebug
# 同签名个人 experimental release：仍需既有 keystore.properties，不能新建替代证书
./gradlew -PnovaLegacyScanner=true :app:assembleRelease
```

仅接受 true/false；默认值 false，debug 不自动打开。不新增 product flavor/sourceSet，以免维护复杂度增加。旧类名保持 ConversationExport，注释明确 Legacy；四份 assets 仍打包但禁用。

显式启用的构建：设置 → 实验功能 → 实验：扫描当前会话。先显示不完整性警告与“开始扫描 / 取消”；用户点击开始才执行。页面打开不会自动采集。只扫描当前前台打开的 ChatGPT 会话，不遍历列表、不打开其他聊天、无批量/周期/Service/WorkManager。离开前台立即取消，不自动续扫。用户取消、成功、JS错误会释放观察器/缓存并恢复原滚动位置；页面已导航/容器已被网页删除时无法恢复旧文档，此时停止并失效旧任务。

## 保留算法和严格失败

顶部向下、底部向上，中间先定位底部再向上，当前实现单向一遍。保留稳定公开 ID、相邻窗口交集、缩小步幅重试、顺序边与唯一拓扑序、角色/正文冲突、公开 loading 指标、稳定等待、step/time/数量/字节上限。相同正文不同 ID 不去重，不按正文相似度强拼，不按 ID 排序，不伪造身份。重复边界稳定是算法停止条件，不是完整性证据。

| 原错误码（保留） | 用户解释别名 | 含义 |
|---|---|---|
| H02_MISSING_ID | H02_IDENTITY_INSUFFICIENT | 缺少稳定身份，停止跨窗口累计；不跳过消息 |
| H03_ORDER | H03_OVERLAP_INSUFFICIENT | 重叠不足或顺序歧义，停止避免错误拼接 |
| H06_UNSETTLED | H06_HISTORY_UNSTABLE | 正文/列表/边界不稳定，停止 |
| H04_* | 页面或内容改变 | 角色/正文冲突、导航或会话失效 |
| H05_LIMIT | 达到上限 | 10分钟、1200步、1000条、200万字符、64MiB缓存或8Mi JSON结果；明确失败不截断 |
| H07_CANCELLED | 取消 | 用户或离开前台取消，无部分文件 |

## assistant progress 与无作者节点

保留已有有限 fallback：可见 `article[data-turn=assistant]` 或 `data-turn=assistant` 且 conversation-turn testid 的容器，不含普通作者、嵌套作者/重复容器。不是只看 data-message-author-role。公开 commentary 标记可以识别进度，未知 channel 不猜类型。单窗口研究路径可保留无ID正文并警告，跨窗口必须 H02。隐藏、aria-hidden、按钮、navigation/toolbar 等 UI 排除；不新增网站结构 selector，不把任意文本当助手。明显页面结构变化宁可失败。本轮仅补通用 NAV/ARIA 控件过滤，无 selector 扩张。

## 数据边界

只用当前公开网页 DOM。无 undocumented backend/conversation endpoint、React Fiber/Redux/internal state、hidden reader、Service Worker/网络拦截、Cookie/Token提取或 localStorage 挖掘。旧内部读取器源码/文档是历史，当前controller不安装它。locate-text/progress-discovery仍作为人工只读诊断保留，不作自动补采或网络数据源。

## 实验结果与格式

“扫描完成，共采集 N 条记录，请人工核对”仅表示算法结束。摘要含 user/assistant、未知类型助手、fallback、windowCount、elapsedMs、overlapRetries、unstableWindowCount、上下边界和警告。coverage/results/diagnostic 永远 not-proven；滚动结果 `captureMode=legacy-scroll-experimental`。取消记录 cancelled=true。fallbackCount按累计的唯一记录计数；unknownCount是assistant-unknown，和fallback可能重叠，不可简单相加。overlapFailures目前计数缩小步幅重试，不是遗漏量；变化窗口也是观察到的不稳定，不是实际缺失条数。

保留实验 HTML/Markdown；文件前缀 `Nova-legacy-scan-`，内容及metadata标明 experimental captured message set，非完整历史备份。正式快照仍 `Nova-snapshot-`。旧Gecko实验PDF在本轮之前已退役，不恢复其依赖；可在浏览器打开实验HTML后手动打印，不能混称正式快照PDF。

Java diagnostic对白名单数字/布尔、受限枚举和结构数组做递归过滤；不记录正文、真实消息/会话ID、标题、私密URL、Cookie/凭据或非加密正文signature。复制诊断同样脱敏。实验导出文件本身含用户主动采集的正文，与diagnostic分开。普通Logcat中的网页/Android系统日志不在此工具保证范围内；本工具不新增正文日志。

## 测试与CI分类

- `tools/dom-trial/{browser,scroll,progress,locator}.test.cjs` 为当前legacy parser/scanner研究夹具，路径保留以便追溯；可独立执行。覆盖虚拟窗口、同文不同ID、顺序/编辑冲突、progress fallback、缺ID、缺重叠、双方向/中间起点、稳定/超时/step上限、取消和位置恢复。39/40正常结束反例继续验证not-proven。
- `DomTrialExportTest` 明确要求flag=true，旧16个Android场景保留，加3个实验UI确认/暂停取消/诊断攻击字符串过滤测试；2项Gecko PDF明确Ignore，不冒称全部执行。
- `LegacyScannerDisabledTest` 默认flag=false检查实例缺席、重建不注入、设置无实验入口、禁止构造。
- `.github/workflows/legacy-scanner.yml` 独立 `legacy-scanner-tests` 构建/lint/browser和Android35/36实验job，只在相关源码/fixture变化或workflow_dispatch时执行；不作为正式job依赖。无公开发布。
- 原 dom-trial.yml现在只运行正式snapshot/browser及default-build Android合同、HTML/MD/PDF实物、登录菜单/合成会话持久化、IME、upload/download/share/upgrade等既有回归。parser源码路径仍保留。
- 历史 `ConversationExportTest` / export-validation.yml 继续固定旧revision手动运行，用于历史接口方案追溯，不冒充当前scanner验收。

本地浏览器运行：先安装playwright-core并设置NOVA_PLAYWRIGHT_MODULE；示例 `NOVA_PLAYWRIGHT_MODULE=/path/node_modules/playwright-core node tools/dom-trial/scroll.test.cjs`，默认Chromium为/usr/bin/chromium，可用NOVA_CHROMIUM_EXECUTABLE覆盖。Android19项用实验签名release与test APK、scripts/dom_trial_android.py，正式检查使用默认APK和scripts/snapshot_android.py；两套APK应分开，不能把实验包当普通发布。

## 当前验证与后续

隔离实现/实验测试源码 `5d9efe8eee0c24d1ce2b1a48b6e14f74360ae583`；最新正式源码 `fbd7130a7656d4839c6a527d1e509dcc7b4193d6` 仅补原生打印测试目的地加载同步，生产/legacy源码及夹具不变。本地DOM18/scroll32/progress19/locator18/snapshot13通过。实验CI [37588638326](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37588638326) build/lint/browser及Android35/36全通过，各19个执行项、2个明确退役Gecko项。正式CI [37588638317](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37588638317) API35通过，API36两次真实System Print失败，原生测试已补目的地标题加载同步，新正式CI [37592089643](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37592089643) build通过，两API失败是新增原生测试等待未覆盖Select a printer初态；已修正初态与分页同步、待新CI；失败截图及报告保留，不写成成功。

两实际APK独立核原证书/签名content digest、DEX默认flag=false/实验true、源码revision及assets，未改变包名/签名/code14。报告 [isolation-validation.json](../tools/legacy-scanner/evidence/isolation-validation.json) 与 [package-verification.json](../tools/legacy-scanner/evidence/package-verification.json)。通过仅证明受控fixtures，未做真实ChatGPT账号或物理设备验收，永远不能由算法正常结束推断完整服务器历史。不合并main/forcepush/公开Release。

未来Archive可把用户主动导入的官方记录作为独立人工基准（例如官方40 vs scanner39），但scanner不自动访问官方导出，不与Archive共用完整性承诺。不描述为官方导出替代、完整备份或绝对安全/法律结论。

暂不建议sourceSet：少量禁用assets和无初始化controller不值得多variant系统。若以后研究依赖或攻击面明显扩大，再独立模块/sourceSet；不复活Gecko或旧backend来扩覆盖。
