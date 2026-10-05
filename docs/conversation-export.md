# 当前会话导出：实现与验证记录

## 基线与修改范围

唯一产品基线是 `fix/native-share-1.3.7` 的 `200b7898b51cdac9e342e49e7e7a7059067d944f`。旧 workspace 已不可恢复，经用户授权重新 clone，在该 commit 建立 `feature/export-conversation`。不存在可恢复的上一轮 export 文件；本轮后续中断留下的文件已保留并继续完成。本次继续时 `feature/export-conversation` 已存在，checkpoint `50ab7dd` 已有数据提取、HTML/Markdown/PDF、Android 文件流程及测试；保留这些文件，补齐 Markdown 围栏、反斜线公式及 DOM 公式核对，并修复 Android 打印生命周期/测试诊断，没有重新 clone。

`ComposerWebView.java` 和 `WebShareAdapter.java` 与基线逐字一致。没有引入 PR #3 的 `NovaWebView`；现有 `pasteFromInputMethod`、InputConnection、commitText、网页 paste/input 适配未改动。移除的是“分享当前页面”“从剪贴板粘贴”两个原生菜单入口及其 Java 专用方法，新增“导出当前会话”。共享的网页粘贴脚本保留，避免改变已验证的 IME 行为。网页 `navigator.share` 仍由原 WebShareAdapter 提供。

## 数据与完整性

2026-10-04 只读研究了未登录的公开 `chatgpt.com` 页面及其公开客户端资源：`/cdn/assets/conversation-small-ig1392ugx3eo129o.js`。该版本有历史消息分页加载逻辑（`paginated_conversation_initial`），完整请求使用 `include_full_conversation`；客户端也有从 `current_node` 沿 `mapping[node].parent` 遍历的逻辑。公开代码不足以确认所有账号所用的虚拟列表策略，未把 DOM 视为完整数据。

导出仅在顶层 `https://chatgpt.com` 已保存会话 `/c/<id>` 上运行：

1. 显式导出操作在当前页面请求 `/backend-api/conversation/<id>?include_full_conversation=true`，采用浏览器自身同源凭据行为，不读取 Cookie、Token、密码、session/storage 或请求头。
2. 文档启动脚本只观察网页已经发起的当前会话响应作为备用数据，不请求账号列表、其他会话或文件资源，不保存请求凭据。网页返回值仍原样返回。
3. 检查 conversation_id、current_node、mapping；验证选定末端到根的整条父链、节点 ID、无循环、父 children 反向关系、根节点，以及显式分页/不完整标记。
4. 页面消息 ID 只用于确定当前分支、核对顺序与末条内容。服务器末端一致时采用 current_node；若页面选择了另一条重新生成的回复，只有该节点在完整树中明确没有子节点时才可选作末端。历史中段的 DOM 不能选作会话末端。
5. 当前页面所有核对消息必须位于该链且顺序一致。读取过程中 URL 或 DOM 分支变化会拒绝导出。未完成回复、缺失根、断链、循环、未支持正文类型均拒绝。
6. 从验证后的完整链生成 User/Assistant 正文；系统、工具调用和隐藏 analysis 消息不导出。DOM 不是历史正文来源。

无法确认时显示持续的“未能导出会话”对话框，说明“无法确认完整会话”，不会降级为仅已加载消息，也不会生成残缺文件。可提示用户滚动到末尾、等待回复完成或刷新。

## 三种格式

同一次提取产生规范化消息数组，共享标题、角色、Markdown 原文、图片限制说明、引用 URL 与完整性证据。

- **HTML**：捆绑 MIT 许可的 Marked 15.0.12，通过标签及属性白名单生成单文件正文，移除脚本、控件、事件属性和危险 URL。内嵌手机优先样式、深浅色、代码横向滚动、表格滚动、长 URL 折行、图片自适应。CSP 禁止外部脚本、样式、网络图片和表单。真实链接保留为可点击地址。
- **Markdown**：保留原消息 Markdown，添加 User/Assistant 分隔；标题、列表、引用、围栏代码、行内代码、表格、链接保留；消息末尾未闭合的代码围栏会补齐，避免吞掉下一条消息的角色分隔。引用元数据存在明确 URL 时补充链接及 Sources；不猜测引用目标。
- **数学**：保护并保留 `$...$` / `$$...$$` / `\(...\)` / `\[...\]` LaTeX 原文，HTML 用可读等宽源文展示，不依赖外部数学脚本。不是完整 KaTeX 排版还原。
- **图片**：HTML 仅嵌入当前分支消息中已经加载、可通过 canvas 安全读取的图片或原文已有的数据图片，不下载模型写出的任意 URL。跨域、未加载、大小限制或仅有资源指针时给出占位及明确说明。Markdown 保留原始图片引用。离线图片不能保证完全保存。
- **PDF**：使用同一 HTML 在独立、禁止网络和 JavaScript 的 WebView 中排版（临时附着在现有不透明聊天界面下方，保持渲染但不可交互，等待视觉状态完成后启动打印），委托 WebView 的原生 PrintDocumentAdapter 调用 Android 公共 PrintManager，每次写入完整文档，由系统选择页范围；打印 WebView 保留至系统打印界面返回，再释放，默认 A4、标准边距。打印样式让长代码折行、表格固定布局并允许分页。用户在系统界面选择“保存为 PDF”，自行选择位置；之后从文件管理器打开/分享。没有调用隐藏打印 API，也没有另一套正文提取逻辑。PDF 链接是否保留由系统 WebView/打印提供者决定。

## Android 文件机制

HTML/Markdown 先生成临时缓存并显示“保存到本地 / 打开文件 / 分享文件”。保存用 ACTION_CREATE_DOCUMENT；打开用 ACTION_VIEW；分享用 ACTION_SEND + FileProvider content URI 和临时只读授权。FileProvider 仅增加 `exports/` 子目录，不开放整个缓存目录。不新增应用权限。

文件名为 `ChatGPT-<标题>-yyyyMMdd-HHmmss-SSS-<随机后缀>.<扩展名>`；处理中文、非法字符、空标题、长标题和重名。标题按 Unicode code point 截断，避免截断代理对。MIME 为 text/html、text/markdown、application/pdf。取消保存可再次导出。临时 HTML/Markdown 会在后续导出时清理超过七天的旧缓存。

PDF 使用系统保存流程，不具备 HTML/Markdown 的导出后原生“打开/分享”对话框；保存后的打开/分享通过文件管理器完成。

## 验证与样例

- `tools/export-tests/core.test.cjs`：25 项断言，包括短会话、400 条消息但 DOM 仅末条、中文/英文、结构和链接、当前/重新生成分支、错误会话、缺失根、断链、循环、分页标记、消息顺序、未完成回复、附件限制、图片指针和引用 URL。
- `tools/export-tests/browser.cjs`：真实 Chromium 渲染、HTML 净化、生产提取脚本和分块通信、分支拒绝、当前会话响应观察、320/390/844/1280px、深浅色、单文件阅读、数学源文、代码/表格/链接/图片。确保导出解析器不覆盖网页自己的 marked。
- `ConversationExportTest`：生产 Android 导出路径的保存、分享、取消、重复导出、文件名、不能确认时拒绝生成文件、系统 PDF 打印/取消/保存与 PdfRenderer 打开。
- CI `export-validation.yml`：构建、浏览器样例、Android 33/34/35；先运行 6 项导出测试，成功后运行 22 项基线回归，并分别保存报告；测试仅读取该合成会话保存到 Download 的 PDF，不使用 root 诊断。测试窗口显式唤醒/解除锁屏，打印目标及“Save to PDF”按钮状态经过核对；保留基线 WebShare、WebView、附件/下载、媒体权限、ClipboardUi 和 ClipboardProbe 的回归断言及性能阈值。移除的仅是不存在的原生粘贴菜单测试及对应报告项。

`samples/export/conversation.html`、`.md` 是合成的 80 条消息当前分支样例，另一分支不包含在正文。测试另生成 15 页 Chromium PDF 和手机浅/深色截图。它们不是已登录 ChatGPT 真实会话的导出证据。Android33/34/35 系统保存样例已从 CI 取回：5 页 A4，保留全部 80 段用户正文和最终回复、代码、表格、公式源文及 81 个真实 URL 链接注解；这份样例包含 2 条合成消息，仍不代表真实账号。

最终 CI 三档导出均 6/6；Android34/35 全部 28/28，Android33 27/28，原有长键盘粘贴性能门槛失败（2234.5 ms / baseline 1641.0 ms），未修改输入实现或放宽阈值。

本地 Debug APK、Android 测试 APK 构建成功；通过临时 Gradle init script 取消本机构建的签名配置，生成未签名 Release APK，项目发布签名配置未修改。普通 Release 打包缺少原发布 keystore 会失败；未签名 APK 不能直接安装。没有创建新的发布密钥，也未验证覆盖安装旧 APK。

## 已知限制及未验证

- 没有可用的已登录账号/真机；真实会话接口授权、当前网站账号变体、登录、聊天、原始 IME 真机粘贴、外部浏览器/笔记 App/文件管理器兼容性、真实附件上传与网页 Share 交付均未在真机验证。
- 仅同源 Cookie 的请求可能被接口拒绝；网页未提供完整会话响应、分页数据缺失根链或模式改变时会明确拒绝导出。不会为绕过授权读取 Token。
- 需要页面能核对当前分支末端；在虚拟化/懒加载历史中段可能需先滚动到底部。
- 含附件文件的消息当前拒绝导出，因为无法确认文件正文完整；对话中的长输入如果被网站转成附件也受此限制。不是 IME 输入回归。
- 某些图片只能保存占位；公式保留 LaTeX，不保证完整视觉排版。特殊交互卡片、音视频、Canvas 内容等无法可靠保存时拒绝，而不假装完整。
- 有 16×1024×1024 UTF-16 code unit 的数据传输上限及图片大小保护限制，超限明确失败，不截断正文。
- Android PDF 实际保存、PDF 可点击链接、复杂长代码/宽表格的所有打印提供者行为，以实际测试/设备结果为准。

最终 CI 结果另见 `docs/export-validation-results.md`；不能从测试已编译或已添加推断测试已通过。

## 修改文件

- `.github/workflows/export-validation.yml`
- `app/build.gradle`
- `app/src/androidTest/java/com/example/chatgptnova/ClipboardProbeTest.java`
- `app/src/androidTest/java/com/example/chatgptnova/ClipboardUiTest.java`
- `app/src/androidTest/java/com/example/chatgptnova/ConversationExportTest.java`
- `app/src/androidTest/java/com/example/chatgptnova/FixtureActivity.java`
- `app/src/androidTest/java/com/example/chatgptnova/NovaWebViewTest.java`
- `app/src/main/assets/export/capture.js`
- `app/src/main/assets/export/core.js`
- `app/src/main/assets/export/marked-LICENSE.md`
- `app/src/main/assets/export/marked.js`
- `app/src/main/assets/export/run.js`
- `app/src/main/java/com/example/chatgptnova/ConversationExport.java`
- `app/src/main/java/com/example/chatgptnova/MainActivity.java`
- `app/src/main/res/xml/file_paths.xml`
- `samples/export/.gitignore`
- `samples/export/README.md`
- `samples/export/conversation.html`
- `samples/export/conversation.md`
- `scripts/upgrade_and_web_tests.py`
- `tools/export-tests/android-ci.sh`
- `tools/export-tests/browser.cjs`
- `tools/export-tests/core.test.cjs`
- `tools/export-tests/fixture.cjs`
- `docs/conversation-export.md`
- `docs/export-validation-results.md`
