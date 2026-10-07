# 当前交接：Legacy Scanner 隔离（2026-10-07）

工作分支 `feature/export-conversation`；本轮最新构建源码 `fbd7130a7656d4839c6a527d1e509dcc7b4193d6`；实验已测源码 `5d9efe8eee0c24d1ce2b1a48b6e14f74360ae583`（应用/legacy源码和夹具完全一致，仅正式原生测试同步变更）；当前文档基点 `fbd7130a7656d4839c6a527d1e509dcc7b4193d6`。最终文档提交可由git log核对，不等于APK源码HEAD。远端默认HEAD仍main/e8ffa0c6，本轮全程以工作分支最新远端为准；开始续接先fetch并读AGENTS/latest/REVIEW/legacy-conversation-scanner/frozen-page-save，不回退旧修复。

## 当前结论（优先于下方分步历史）

隔离实现已完成：Stable保存当前网页三格式；Archive官方导入仍未来规划；Experimental只显式flag启用，默认debug/release=false且不构造controller，不注入或自动扫描。实验设置警告确认→用户开始→前台当前会话→可取消/恢复；结果/文件/脱敏diagnostic永远not-proven。ConversationExport类名保留，历史源码/算法/失败证据未删除，不扩数据源/selector，不改包名/签名/code14，不main/forcepush/Release。

验证源码5d9efe8：本地DOM18/scroll32/progress19/locator18/snapshot13通过；实验CI37588638326 build和Android35/36全success，各19项通过（旧16+新3），Gecko2项明确退役。此前正式CI37588638317 API35全success、API36两次实际System Print失败；修复原生测试同步后的fbd7130正式CI37592089643 build/lint/签名success，Android35/36进行中，不放宽PDF断言。实际default/experimental APK DEX false/true、sourceRevision、原签名v2及content digest已独立核对，约1.83MiB。证据tools/legacy-scanner/evidence/isolation-validation.json、package-verification.json。

下一步核原生测试同步修复后的正式CI和实际PDF，再更新最终结论；之后仅物理设备/真实账号人工验证，不能证明完整历史；不重新投入selector扩张或批量采集。下方保留每步当时状态，ac4776f快照验收属于历史。

## 分步历史：最新要求与 Step 4b（2026-10-07）

当前分支feature/export-conversation，待Android验收源码4ca3c0428f46a646fc6c95a476349e35942888d8；审查29ea76d、隔离3d7f051、CI分类755d727、minSdk26与顶层错误码白名单修正4ca3c04。用户要求隔离旧实验scanner，默认false、显式property开启、警告/手动/前台/取消/恢复、脱敏not-proven，保留旧代码与失败证据；不增selector、不扩数据源、不main/Release/版号。

本步独立本地验证success：DOM18、scroll32、progress19、locator18、snapshot13；Python语法/YAML/diff通过。受跟踪文件无删除，FrozenPageSnapshot/PageSnapshotExport/freeze.js/ComposerWebView/WebShareAdapter与原基线无diff。报告：../../tools/legacy-scanner/evidence/isolation-validation.json。

正式CI37587862891构建/lint/unit与原签名检查success，Android35/36进行中；legacy37587862825浏览器success、构建进行中。755d727两次旧CI因新提交concurrency cancelled，不能写通过。当前Android新增default合同2、实验19及原正式回归not verified，待读取结果修复实际失败。真实账号/物理设备未验证。下一步核对两套CI、下载小报告独立验证、更新docs/REVIEW再push。

正式路径不变；默认debug/release不初始化ConversationExport；实验设置需开始扫描；单向stable ID/overlap/拓扑序/冲突/稳定上限/恢复保留，文件Nova-legacy-scan和not-proven。旧Gecko PDF已在此前退役，仅实验HTML/MD；正式System Print PDF保留。详见../legacy-conversation-scanner.md，未来Archive官方导入仅规划。

### Step 4c：实际APK独立核对 success

已通过GitHub connector取回正式artifact11466649749、实验artifact11467092575（CLI token/下载失败由connector解决，不重新登录/不绕代理）。独立核ZIP digest与GitHub元数据、APK SHA与交付校验、APK v2 RSA签名及签名content digest、原证书指纹、四legacy+freeze assets逐字节等于4ca3c04源码、无native so。DEX BuildConfig.ENABLE_LEGACY_SCANNER正式false/实验true。正式1916121bytes/SHA a37d8cd6371591f263ac00dc13eb49283abc0e3adda45d0e933351d941af5eaa；实验1917341bytes/SHA 6856865eb9439088910a02625d7005f2b4f9be68ec54426a4f2c1cdab7e02e3c。

入库报告 ../../tools/legacy-scanner/evidence/package-verification.json。两套build/lint/unit/签名均success，四个Android jobs仍进行中（not verified），不能把包验证当作UI/恢复/正式保存通过。下一步继续读取Android结果，不公开Release、不改版本。

### Step 4d：旧文件语义断言同步

复查Android旧fixture发现一项仍断言旧“滚动收集并缓存可见历史”文案；本轮已更名实验记录，改断言Experimental captured message set/captureMode/historyCompleteness，保留40条逐字/同文不同ID/恢复/不完整性检查，没有删除或放宽。补白名单users/assistants数值，使复制diagnostic保留分角色计数，并在旧方向测试要求20/20与not-proven。4ca3c04 Android仍进行中，本小修需新CI，不据静态发现宣称旧运行实际失败。下一步读取最新两套CI。

### Step 4e：前次实际Android失败已核

读取4ca3c04实际job logs：Android35 job112682669293、36 job112682669294均执行19项、1项失败，均为historyScrollCachesUnmountedMessagesThroughSaf第152行旧文件文案断言；其余18项（含新确认/前台取消/脱敏）通过。两套run最终被新提交取消，不能标overall success。失败摘要入isolation-validation.json previousRuns，不删除证据。最新源码5d9efe8eee0c24d1ce2b1a48b6e14f74360ae583正式37588638317/实验37588638326正在重跑，尚未最终验收。下一步等待新Android结果并独立核对最终文件与APK。

### Step 4f：最终候选包独立核对 success

源码5d9efe8，正式run37588638317/artifact11467268360，实验run37588638326/artifact11467472980。实际APK再次独立核SHA、v2RSA签名+签名content digest、原证书、5份assets、无native so；读取实际DEX核EXPORT_REVISION=5d9efe8、原applicationId、正式flag=false/实验=true。报告package-verification.json；前包报告另存package-verification-4ca3c04.json不覆盖失败历史。

正式1916153bytes/SHA4f9212d7dca36cb1563b88acce17051c89afe03ae9cb3d692a03cc19eb469ea5；实验1917377bytes/SHA423270eda1b8811372867a5fbc0ed7c466e1241a36d60b08b2af75d305e66204。两套build/lint/签名success，四Android jobs尚进行中；本步仅包核对、未完成最终Android验收。下一步等待19项legacy/default2/正式保存与回归，核实际文件，更新最终交接。

### Step 4g：Legacy两档最终验收 success

最新源码5d9efe8的实验CI37588638326全success：legacy-scanner-tests/build/lint/browser/原签名及Android35/36。已独立下载两档artifact11467947062/11467608903，核instrumentation各OK(19 tests)，19个成功方法与2个Gecko明确退役项；文本证据和摘要入tools/legacy-scanner/evidence。旧16个+新确认/前台取消/脱敏3个全部通过，恢复滚动/缺ID/overlap/不稳定/progress/SAF仍通过。仅受控夹具，真实账号未证明完整。

正式CI37588638317的build已success，两档Android仍进行中，尚不能最终声明正式保存回归完成。下一步核默认关闭合同、HTML/MD/PDF实物及IME/upload/download/share/升级报告，再最终交接。

### Step 4h：正式API35通过，API36打印失败保留并重试

源码5d9efe8正式API35 job112685070124全success，独立下载artifact11467699020核checks无失败、默认关闭2/快照10/全部原web/IME/share/native/升级回归通过；实际HTML/MD首尾/变更排除、同打印源、Nova/Firefox两PDF各31页与文本标记独立通过。API36 job112685070153仅realSystemPrintUiSavesFrozenMultipagePdf等待native PDF filename失败，另9项快照/默认关闭2/其余正式套件与Firefox均通过；截图显示printspooler错误/Retry页，不能称PDF成功。失败原instrumentation/UI文本/截图与JSON保留tools/legacy-scanner/evidence。已只重跑该失败job，源码/断言不改，不擅自归为环境问题。当前HEAD e26d9ad；本步实际结果success/failure混合，下一步读取同源API36重试、核PDF实物再最终交接。


### Step 4i：正式API36同源重试失败，修复原生打印测试同步

当前文档基点610fb8708fac93d7334ca9d44292805d67673165；已测源码5d9efe8。API36 job112689954042/artifact11468372879第二次仍native PDF filename超时，failure已保留原测试/UI和摘要，正式整体failure不能验收。日志在07:59:20首轮write(1 range)后测试打开destination并重选默认PDF，07:59:23第二次layout、write(0 ranges)后spooler错误。原测试只等窗口包名就判断Save as PDF缺席，窗口存在不代表目的地标题已加载。新增等待spinner标题非空后才判断是否切换目的地，避免加载时误操作；不改生产SnapshotWebView/PageSnapshotExport，不跳过实际保存/31页/首尾/变更排除/Firefox断言。本步修复待新CI验证，尚非成功结论。下一步核新正式CI与实际文件；legacy应用/fixture不变，其19项结论仍属于5d9efe8。

### Step 4j：修复候选包独立核对 success

当前HEAD/新构建源码fbd7130a7656d4839c6a527d1e509dcc7b4193d6，正式CI37592089643 build/lint/browser/unit/原签名success；两档Android进行中。已独立下载默认artifact11468968104，ZIP digest匹配、APK v2 RSA签名与content digest验证、原证书/5份assets/无native so/DEX flag=false及revision=fbd7130/applicationId通过。1916145bytes/SHA44a337745c9a5c5c3e26bda1dc95f6807f847451600f186c8bc9a8460095046a。旧5d9两包报告另存package-verification-5d9efe8.json，实验包仍5d9（生产/legacy源码无变化）。本步仅包核对success，正式Android仍not verified；下一步核实际PDF和全部回归。

## 此前 Frozen Snapshot 实现与验收（历史）

用户最终要求普通HTML/Markdown/PDF，取消Share/MHTML/新完整历史算法。正式“保存当前网页”→一次evaluateJavascript同步深clone→同clone静态HTML和Markdown→不可变FrozenPageSnapshot。HTML为唯一标准表示；PDF仅同frozenHtml→独立无JS/static WebView→Android System Print。Nova860px阅读/白底print CSS，不复制ChatGPT SPA。无滚动/backend/private reader/React/token/storage读取/ID去重/跨窗口缓存，不重构输入上传下载分享。

阶段A保留Gecko完成实际受控Nova/Firefox×Android35/36 PDF保存/独立重验后，阶段B移除GeckoPdfExporter/唯一旧PDF引用/dependency/仅Mozilla仓库/许可资产/lint例外/专属ABI和650MB交付。保留旧dom/scroll/progress/locator源码及证据，正式菜单不调用；16旧Android例继续，2Gecko专项明确Ignore保留历史。Firefox/OCR只CI，不加入生产。

## 验收与证据

最终CI https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37556129018 全绿：build、Android35、Android36均success。两API小证据下载并独立复核，四份PDF均31页Letter；最终API36 SAF HTML三浏览器file打开通过。实际HTML/MD/PDF、同snapshot live mutation隔离、唯一打印源与Firefox源逐字节相同、标准PrintUI保存/取消/多次/生命周期、旧容器/权限/上传/下载/blob/Share/所有IME/native及A大包→B/v1→B覆盖升级均有受控测试。具体结果和13项限制见docs/frozen-page-save.md及tools/snapshot/evidence。不是物理设备/登录真实账号成功。

最终主APK1910773字节(1.822255MiB)，SHA fa96b8714047ae707bbb013be8473be98a51db92cd826c4652f7ebe0d008e5c5；原包com.example.chatgptnova、versionCode14/1.4.0-scroll-trial、cert f93221ee0d2be2b806233a0b3427ec9c14766100c1bab3208841e6203e2b4289。A7 universal681117497字节；B无native so、Gecko Dex/deps，同一包支持ARM64等原架构，不假造split。总安装占用未知，APK不包括共享System WebView/应用数据。独立主APK artifact11454731981，不混测试APK，无公开Release。

历史CI A7(37552313716)原host首标题文本误判仍红，四实物独立NFKC/必要Firefox首标题144DPI离线OCR+人工图像通过，android-pdf-firefox-A7.json保留原失败。B1 API36仅旧菜单预期失败、其余全部通过；B1 API35取消不记通过。最后只同步4个菜单预期，账号设置/清理断言保留。不得将历史红/取消改称绿。

## 剩余边界与继续工作

真实账号与物理设备、厂商WebView、相机/麦克风硬件手动验收尚未执行；用户ADB条件未知，不反复追问。用户可以直接同签名覆盖安装测试APK核当前加载内容/普通HTML/系统打印质量。不继续追求无法证明的完整历史或自动Share。

DOM100000元素/12MiJSON硬限，无静默截断；SAF后台UTF8/64KiB流式复制/wt截断旧目标、非空长度核对，取消/destroy释放。S01–S10脱敏diag不含正文/真实ID/URLquery/凭据。外链图片字节非冻结/可能需网络与变化；blob/附件原文件/canvas/shadow/runtime未备份，inlineSVG移除，数学可读/公开LaTeX非像素级。静态资源无Cookie/Auth、有数量/大小/时间上限。

保持原签名/包名/版号；不main/forcepush/公开Release。必要源码/测试/文档可提交push本分支。每完成一个工作步骤都更新本交接，验证结论或边界变化时同步REVIEW，并随步骤提交push本分支；所有后续接手AI必须执行。仅实际压缩/迁移或用户明确要求时提供下一会话提示词，普通回复不生成。/tmp/nova-stage-b.py已应用勿重复执行；CI证据永久描述在Git，原artifact有期限。

三种格式严格对应同一次当前网页冻结快照，但无法证明ChatGPT服务器端完整会话历史；如果网页虚拟化未挂载较早内容，保存结果也不会包含那些内容。
