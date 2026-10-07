# 当前网页冻结保存：HTML / Markdown / PDF

源码 `ac4776ff31feaed9794dbc434e423ac291fbee83`；[验收 CI](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37556129018)。**最终 CI 全绿：build 与 Android35/36 均 success，包含 Gradle build/lint/unit、现有与新增 instrumentation、实际文件及升级检查。** 本轮取消 MHTML、Share 快照与新的完整历史扫描。正式入口是“保存当前网页”。包名/原签名/版本保持原样，没有公开 Release。

## 1. 唯一快照与源码

`FrozenPageSnapshot` 的字符串和长度字段均为 final；诊断 metadata 保存为字符串，读取时返回副本。`PageSnapshotExport` 先核可信 HTTPS chatgpt.com 页面，然后仅一次 evaluateJavascript：记录 URL/title/time，同步采样公开呈现状态，深 clone 一次；后续 HTML 和 Markdown 转换只访问 clone，一次返回完整 payload。URL/title 变化报 S08，超限明确失败，不截断。

源码：`app/src/main/java/com/example/chatgptnova/{FrozenPageSnapshot,PageSnapshotExport,SnapshotWebView}.java`、`app/src/main/assets/snapshot/freeze.js`、`MainActivity.java`。配套 `FrozenPageSnapshotTest`、`FirefoxSnapshotTest`、`NovaWebViewTest`、`NativeUiTest`，`tools/snapshot/` fixtures/浏览器测试、`scripts/snapshot_android.py` 与 CI。未重构 ComposerWebView、上传、下载或分享。

## 2. HTML 是唯一标准表示

输出真正普通 UTF-8 `.html`，不是 MHTML/WebArchive。HTML 直接保存 snapshot.frozenHtml；PDF 直接加载同一字符串，没有另一个 PDF HTML renderer。采用 Nova 自有内联 reading stylesheet：860px 正文、长 URL 换行、屏幕代码/表格横向滚动、图片限宽。打印白底黑字、16mm 页边距、标题避免孤行、代码换行、表格行尽量不拆；长 article/pre/table 可以跨页，不强制整条消息不可分页。

移除 scripts、inline handlers、导航/菜单/按钮/输入/反馈、隐藏（含 CSS clip）控制、ChatGPT SPA CSS 与执行依赖。角色只取可靠公开 DOM 标记，无法确认按正文顺序，不猜角色、不要求 stable ID。保留代码/表格/引用/列表/链接/可读数学/图片表示。CSP 禁止脚本、网络连接、frame/form；相对 URL 在冻结事务中绝对化。正文可脱离登录阅读，外链图片可能仍需网络。

## 3. 普通浏览器实际打开

对实际 Android SAF 保存文件以 file:// 打开 Chromium143.0.7499.4、Firefox144.0.2、Edge154.0.4258.62，核 UTF-8 中法英/emoji、首尾、代码、表格、数学、320×120 PNG、无脚本/HTTP 请求依赖及白底打印 CSS。不是将 Chromium 冒充 Edge，也不是将桌面 PDF 冒充 Android PDF。证据 `tools/snapshot/evidence/android-saved-html-compatibility.json`。

## 4. Markdown

Markdown 在同一次同步事务中从相同 clone 生成，不解析保存的 HTML/PDF，不再读 live。支持 heading/paragraph/bold/italic/link/list/blockquote/inline code/fenced code/table/hr/公开 LaTeX 与 image alt/URL；带保存时间、当前冻结快照来源和已加载内容限制。实际 UTF-8 `.md` 验证中法英/emoji、fence/table/blockquote/lists/links、首尾、排除按钮/脚本/后续 mutation。

## 5. PDF 正式路径

frozenHtml → 独立 `SnapshotWebView` → loadDataWithBaseURL → visual state callback → createPrintDocumentAdapter → Android PrintManager → 系统 Save as PDF。JavaScript/storage/文件访问/桥/新窗口关闭，导航阻止，媒体上传能力未暴露。Nova 不要求最终 PDF 路径，打印取消不报告成功；完成、取消、Activity destroy 释放临时 WebView。

静态 HTTPS 图片请求不带 Cookie/Authorization，不读账号 Cookie 内容；拒绝 backend/API 路径及非必要协议，4 次重定向、5 秒、单资源8MiB、64资源/总32MiB。外部资源字节不属于 DOM 不可变保证，网络图片可能变化。

## 6. Firefox Android 实际 A/B

官方 Firefox157.0.1（仅 CI 测试 app）通过公开菜单 Save as PDF 保存实际文件。它读取 System Print 测试留下的原始 frozen-page-print-source.html；host 要求 firefox-source.html 与之逐字节相同，包括时间。Android35：Nova278245字节/Firefox355824字节；Android36：Nova294175字节/Firefox355727字节，四份均31页Letter(612×792pt)。Nova各2个链接，Firefox各4个（包含浏览器页眉链接）；检查首尾/中法英/长代码/表格/数学/链接/PNG 与后续内容隔离，抽样渲染检查阅读/分页。Firefox附日期/URL/页码，Nova没有该浏览器额外页眉页脚；字体、边距和分页存在合理差异，不保证逐字节相同。

Firefox粗体首标题在真实 PDF 可见，却缺少可提取文字；正文按 NFKC 验证兼容汉字，只有该首标题必要时用实际首页144DPI离线 OCR。其他14个内容标记仍要求 PDF 文本，图片对象仍断言320×120，没有删首尾检查。OCR不进入生产。

## 7. 同快照一致性

fixture snapshot 前含 Before snapshot marker，后续立即变更 live 标题/正文/表格及新增 After snapshot marker。实际 HTML、Markdown、唯一打印源和 PDF 保留 BEFORE、排除 AFTER。另测公开角色、隐藏控制、过量 payload、URL/title 变化、100+屏与超长消息。跨格式选项不会再读取 live；下次主动保存则创建新的快照。

## 8. Android 验证范围

Android35/36 x86_64 受控模拟器保存实际 HTML/MD/PDF、标准系统打印/取消/多次/返回/Activity 生命周期，继续 WebView 导航/Cookie/清理/上传多文件/下载与 blob/Web Share/媒体权限/长文本/Clipboard/三种 IME/native/renderer 恢复测试，并验证原签名 A Gecko大包→B 及 v1→B 覆盖升级的数据保留。

这是受控 fixture 与实际文件验收，不是登录真实账号/物理设备人工成功；拍照/麦克风硬件体验与各厂商 WebView 还需真机核对。旧16 Android例继续，2个 Gecko 专项明确 Ignore 保留历史证据，不声称旧18全执行。

## 9. 实测包体

移除前 A7 universal APK：681,117,497字节；最终 universal APK：1,910,773字节（1.8223MiB）。移除 native 引擎后，同一 APK 适用 ARM64 与其他原支持架构，没有虚构独立 split；历史 ARM64 178,491,277字节来自旧190113f，不能冒充本轮A7 ARM64实测。两API installedBaseApk实测均1,910,773字节；总安装占用无法从APK推断，设备 du 权限限制的未知项如实保留。

最终 APK SHA256：`fa96b8714047ae707bbb013be8473be98a51db92cd826c4652f7ebe0d008e5c5`。保留 com.example.chatgptnova / code14 / 1.4.0-scroll-trial，原证书 SHA256 `f93221ee0d2be2b806233a0b3427ec9c14766100c1bab3208841e6203e2b4289`。依赖/包体证据见 gecko-retirement-package.json、release-runtime-dependencies.txt。

## 10. Gecko 已退出生产

删除 GeckoPdfExporter.java、唯一旧 PDF 引用、GeckoView dependency、仅 Mozilla Maven、Gecko许可资产/lint例外与专属 ABI/650MiB 交付 workflow。实际依赖树无 Gecko，APK Dex无引擎与 exporter，无任何 native .so。Firefox仅对照测试安装，没有打入APK；保留Git历史。Composer输入设计注释中的 Gecko 名称不是生产依赖。

## 11. Legacy 保留（2026-10-07隔离更新）

默认debug/release的ENABLE_LEGACY_SCANNER=false，不构造旧controller或注入旧脚本。显式-PnovaLegacyScanner=true才有设置→实验功能入口，需警告确认并主动开始；不属于正式保存路径。旧测试转独立workflow，下面历史16项执行描述仍是原验收证据。本轮验收另外记录于[Legacy说明](legacy-conversation-scanner.md)。此段原属Legacy隔离轮次；当前Nova Archive已独立实现官方导出兼容ZIP/JSON本地导入MVP，synthetic / fixture验收见[Archive说明](nova-archive.md)，不与Frozen Snapshot数据模型混用。

dom-trial.js、scroll-trial.js、progress-discovery.js、locate-text.js 与旧扫描 controller/相关测试证据保留为 legacy。正式菜单不启动扫描，不扩展 H02/H03/H06、overlap/跨窗口缓存/完整历史推断。旧独立浏览器测试继续执行，不把合成通过当历史完整证明。

## 12. 上限、安全与仍存限制

DOM100000元素，最终JSON12Mi UTF-16 units；native外层callback24Mi检查。后台UTF8缓存→SAF64KiB流式复制，用wt截断较长旧目标并核非空/长度；取消/销毁任务代次失效与临时文件清理。错误 S01–S10，不混旧H代码；诊断只版本/随机snapshotId/origin/格式/字符数/耗时/阶段，不记正文、真实ID、私密query或凭据。全部本地，不上传。

不序列化 canvas、shadow DOM、运行时JS状态，移除 inline SVG；数学优先可读文字/公开LaTeX，非像素级原排版。data图片保留，HTTPS外链可变/可能失效，blob仅占位，附件原文件未备份。字符上限内仍有设备内存/字体差异，无法承诺任何体量/任何浏览器版本；不是完全离线附件备份。

## 13. 不声明完整历史

正式入口、快照文件头/页脚与诊断都描述当前已加载内容，不声称完整服务器历史。旧实验文档留在Git与legacy范围，不作为新功能承诺。

**三种格式严格对应同一次当前网页冻结快照，但无法证明 ChatGPT 服务器端完整会话历史；如果网页虚拟化未挂载较早内容，保存结果也不会包含那些内容。**

阶段历史：A7原CI仍failure（Firefox粗体首标题text提取误判），四份实际文件独立重验及人工抽查通过才实施B。B1 API36唯一旧菜单断言失败，保持断言语义同步后重跑B2。保留证据，不将被取消/失败的历史CI改称成功。

## 下载与证据

Legacy隔离轮次源码 `3149507ef3fb88d66428dde610894ccac82cf44b` 的默认关闭扫描器 APK 在 [正式构建 artifact](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37593888527/artifacts/11470220775)，沿用原签名/包名/code14；实验包是另一个显式开启构建，不能当默认正式包。当前35/36各43项及实际HTML/MD/四份31页PDF已通过并独立核对；原生打印测试补初态/分页同步，生产冻结/打印代码不改，失败历史留证。当前验证见 [Legacy 隔离报告](../tools/legacy-scanner/evidence/isolation-validation.json)，本节以下ac4776f链接保留为此前Frozen Snapshot验收历史。

[主 APK artifact（解压后安装 ChatGPT-Nova.apk）](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37556129018/artifacts/11454731981)仅含主包和校验/签名/大小记录；需要GitHub登录，非公开Release。可同签名覆盖安装，不要求清除账号。版本未频繁递增，诊断buildRevision为ac4776ff31feaed9794dbc434e423ac291fbee83。

- [最终两API实际文件与验证证据](../tools/snapshot/evidence/android-frozen-save-final.json)
- [普通浏览器打开实际HTML](../tools/snapshot/evidence/android-saved-html-compatibility.json)
- [包体/安装代码大小/依赖清理](../tools/snapshot/evidence/gecko-retirement-package.json)
- [最终release runtime依赖树](../tools/snapshot/evidence/release-runtime-dependencies.txt)
- [阶段A原失败与四PDF独立重验](../tools/snapshot/evidence/android-pdf-firefox-A7.json)

最后文档提交不会改变已测APK源码或触发新Release。CI artifact含实际HTML/MD/PDF fixture与测试日志；真实聊天正文未入库。

## Archive接入后的正式回归（2026-10-07）

实际源码 `7854f3ea06309fa20e5e26a7acc4b77750322968`，[CI37609817075](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37609817075) 全success。API35/36各17套43次Stable执行通过；独立下载核对正式HTML/Markdown及四份31页PDF（System Print与Firefox同静态HTML源），原签名升级、登录/IME/上传下载/share/生命周期保持。Archive新增独立offline静态HTML入口，不改变FrozenPageSnapshot语义；snapshot tests仍10项。证据见[API35](../tools/archive/evidence/accepted-runtime-35.json)、[API36](../tools/archive/evidence/accepted-runtime-36.json)。真实账号/物理设备仍未验证，历史失败证据不删。
