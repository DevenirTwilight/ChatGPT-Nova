# 当前工作交接（2026-10-07）

## 分支与目标

仓库DevenirTwilight/ChatGPT-Nova，工作分支feature/export-conversation。先git fetch并核对实际远端HEAD；本文不替代源码，也不把文档提交当应用实现。

最新用户已取消Share/MHTML/完整历史抓取，当前目标为普通HTML / Markdown / Android System WebView Print PDF：一次同步当前页面DOM clone→同clone派生HTML与MD→不可变FrozenPageSnapshot→PDF只打印该对象的frozenHtml。HTML为唯一标准阅读/打印表示，不用Gecko生成新PDF。旧扫描保留源/测试，冻结不扩展，不作为失败fallback。

源462ba8c已取得实际Nova PDF；最新应用/测试源码89b5176d4f6df909bc6de846c5ac56a711a64dfb修Firefox正常菜单匹配，CI37550505533正在执行。必须读取实际结果，不假定通过。Gecko仍生产保留，Commit B尚未开始。

## 已实现架构

- FrozenPageSnapshot.java：final字符串和长度；diagnostic metadata存为JSON字符串，getter复制，不暴露可变内部对象。
- assets/snapshot/freeze.js：一次evaluateJavascript内记录公开状态/URL/title/time，documentElement深clone一次，后续转换只用clone。公开role/明确无author助手turn保留前修；不要求ID、不按正文去重、不滚动/Storage/Cookie/React/private reader/backend/Observer/fetch包装。
- 普通UTF8静态HTML：移除脚本/事件/导航/按钮/输入/隐藏控件/SPA属性，Nova860px内联阅读及打印白底CSS；代码换行、表格/长消息允许分页，图片不超出版心。相对URL同步绝对化；dataPNG保留，blob占位，HTTPS图片可能失效/变化。
- Markdown：同clone语义投影，支持标题/强调/链接/列表/引用/代码/表格/公开数学与图片，明确只保存已加载内容。
- PageSnapshotExport：一次冻结后选HTML/MD/SystemPrint，不按格式重读live DOM。后台UTF8缓存→SAF64KiB流式复制/非空检查；PDF不要求最终路径，不在取消onFinish伪报成功。S01–S10，不混用旧H错误。
- SnapshotWebView：独立JS/storage/桥关闭WebView，仅frozenHtml/loadDataWithBaseURL→visual state→标准createPrintDocumentAdapter/PrintManager。导航拒绝；公开静态资源无Cookie/Auth，HTTPS/4redirect/5秒/8MiB单个/64个/32MiB总量，拒绝API路径。结束/取消/销毁释放，不污染主容器。
- DOM100000元素/JSON12Mi UTF16硬上限，callback24Mi外层/12Mi内层再核；超量明确失败，不静默截断。canvas/shadow/runtime/未挂载历史及外部资源字节不冻结。

详见docs/frozen-page-save.md及源码；不要为PDF另建HTML renderer。

## 实际证据

CI37548381063/source462ba8c已结束（整体失败仅Firefox A/B）。Android35/36各新快照10、旧18与Web9/Share5/ClipboardUi5/全部3项IME/native旋转返回重启/原签名v1升级均通过；build/lint/unit task/签名/浏览器也通过，单元任务NO-SOURCE（没有JVM用例）。

真实System Print UI保存并取回PDF：API35 278243字节，API36 294594字节；两份均31页Letter612×792pt、2链接、1个320×120图片对象。host核首尾/中法英/长代码/长表格/数学及AFTER排除；独立渲染抽查35页1/2/13/16/31、36页1无严重重叠；文本span没有越出纸张。证据tools/snapshot/evidence/android-system-pdf-A5.json。不是预览回调、桌面PDF或真实账号认证。

实际Android35 SAF保存的frozen-page-saf.html在Chromium143.0.7499.4 / Firefox144.0.2 / Edge154.0.4258.62直接file://打开，中法英/emoji/首尾/代码/表格/数学/PNG解码、无JS/HTTP请求、白底printCSS通过；tools/snapshot/evidence/android-saved-html-compatibility.json。

A5小证据artifact11452620683(API35)、11451684941(API36)，在CI37548381063；本地/tmp/nova-html-a5-35及-36为方便检查，仓库与CI才是继续依据。实际universal APK681117497字节；原nativeABIs arm64-v8a/armeabi-v7a/x86/x86_64；installedBaseApk同值。du无权限，安装总占用未知，不编造。

## 唯一剩余A失败及当前修正

Firefox157.0.1受控同frozenHtml A/B已打开正文，未取得PDF。失败UI树/截图明确More菜单无障碍contentDescription为“More Collapsed”，测试只exact匹配“More”，未进入Save as PDF。89b5176按实际正常UI标签修正，并处理可见Download确认；不是Firefox私有打印API。CI37550505533尚未完成，不得将A/B称成功。

FirefoxSnapshotTest仅测试APK：临时loopback服务同对象HTML→官方Firefox UI→Save as PDF→取文件核对；主页面已mutation，三个源不应包含AFTER。CI缺Firefox不能assumption跳过后假认证，host仍要求实物。仅模拟器，不代物理设备/真实账号人工验收。

## 下一步：A全部关键检查通过后再B

1. 查37550505533两个API结果，下载小证据；Firefox若失败先看独立firefox-ui-failure，按公开UI修，不回退扫描。两份真实Firefox PDF首尾/长代码/表格/数学/图片应通过，再比较页数/边距/分页字体。
2. A全通过才能B。实际生产Gecko唯一用户ConversationExport旧PDF；移除其PDF菜单/方法/字段、GeckoPdfExporter.java、Gecko dependency、仅Gecko Mozilla Maven/lint例外/许可资产和Gecko ABI/large APK交付workflow。ComposerWebView中的GeckoInputConnection注释是输入设计引用，不是引擎依赖，勿重构输入。
3. 正式UI只“保存当前网页”及保存诊断；隐藏旧history入口但保留legacy源。原18测试中2Gecko专用例显式Ignore退役、保留源与Git证据，其他16继续；runner不再要求旧Gecko PDF。不要偷偷删全部旧证据。
4. B运行diffcheck/build/lint/unit/全部旧可用Android与新snapshot/全部正常容器回归/API35/36/Firefox实物；检查releaseRuntime dependency和APK/Dex无Gecko/native库；测真实universal/ARM64/installedBaseAPK，不预报大小。原签名/包名/版号不变。可以拿A最终CI签名APK做A→B合成Cookie/storage覆盖升级，再继续v1升级。
5. 完成后交付稳定测试APK与GitHub CI artifact/文档；不公开Release。报告13项验收与限制，真实物理Android/账号未测明确说明。

## 用户偏好及固定边界

版号仍1.4.0-scroll-trial/code14；package com.example.chatgptnova，原cert SHA256 f93221ee0d2be2b806233a0b3427ec9c14766100c1bab3208841e6203e2b4289。修复不疯狂递增版本。用户已授权工作分支提交/push及测试APK，不授权main合并/forcepush/公开Release。

无可连接真机ADB条件；不要重复八项现状提问或以合成登录/附件夹具证明真实ChatGPT账号成功。三种格式严格对应同一次当前网页冻结快照，但无法证明ChatGPT服务器端完整会话历史；如果网页虚拟化未挂载较早内容，保存结果也不会包含那些内容。

仅实际压缩/迁移或明确要求时更新必要交接并push，普通回复不要给迁移提示词。历史过程完整保存在Git，旧滚动/403、Share虚拟化与MHTML失败不是当前待扩展任务。
