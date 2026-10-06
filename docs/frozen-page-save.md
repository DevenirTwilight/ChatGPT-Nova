# 当前网页：普通 HTML 冻结快照

本轮 Commit A 的唯一快照表示是普通 UTF-8 HTML，取消 MHTML 和 Share 后续开发。Gecko 暂留旧试用路径供 A/B，新保存 PDF 使用 Android System WebView Print。版号保持1.4.0/code14，不发布Release。Commit B必须在A的Android35/36和实际PDF验证通过后进行。

## 数据流

保存当前网页（原型）→确认冻结此刻→一次evaluateJavascript同步记录公开呈现状态与URL/title/time→深clone一次→同clone派生静态阅读HTML及Markdown→不可变FrozenPageSnapshot。

- HTML：frozenHtml原字符串→后台UTF-8缓存.html→SAF流式复制/非空校验。不是WebArchive，不含MIME封装。
- Markdown：同clone转换的markdown原字符串→后台UTF-8缓存.md→SAF。不是从HTML文件/PDF再解析。
- PDF：相同frozenHtml→独立SnapshotWebView（无JS、无storage、无桥）→静态加载完成及visual state→createPrintDocumentAdapter→PrintManager→系统Save as PDF。Nova不要求取得最终文件路径；onFinish包括取消，不报告已保存成功。

选格式后不再读取live正文补内容。冻结期间URL/title变化明确拒绝；完成后不跟随live mutation。没有滚动、账号存储/认证读取、fetch/XHR hook、observer、backend/private reader/React Fiber、消息ID要求、正文去重或跨窗口合并。

## HTML阅读和打印

只保留clone的main（或公开role=main/body兜底）阅读正文。移除脚本、事件、按钮/菜单/导航/输入/反馈、隐藏控制、应用样式和属性。公开data-message-author-role及无普通作者的显式assistant/user conversation-turn可提供角色标签；不猜角色。内联Nova stylesheet，无ChatGPT CSS依赖；860px正文宽度、代码/表格屏幕横向滚动、图片max-width。打印白底黑字、16mm页边距、标题避免孤行、代码换行、表格单行尽量避免切割；长消息/pre/table允许跨页，不强制整条不可分页。

相对链接/图片在同一事务中解析成绝对HTTPS URL，currentSrc在同步呈现状态采样中记录。data图片保留；blob图片无法离线复用，以可读占位说明。HTML外链图片可能需要网络、失效或改变；不下载原附件，不保证完全离线。静态打印资源请求限HTTPS、4次重定向、5秒、8MiB/资源，无Cookie/Authorization，并拒绝backend/API路径。保存HTML CSP拒绝脚本/连接/框架/表单；图片允许HTTPS/data，不允许应用再次启动。

canvas bitmap、shadow DOM、运行时JS状态不序列化。数学优先公开LaTeX annotation，保留当前可读文字，不保证原排版。Markdown支持标题/段落/强调/链接/列表/引用/代码fence/表格/数学/图片；保留公开pre-wrap换行。

## 限额、生命周期、诊断

同步DOM最多100000元素、最终JSON payload最多12Mi UTF-16 code units，超过明确失败，不静默截断。Android回调再核限额；文本UTF-8编码/写盘与64KiB流式复制在后台，取消/销毁检查代次。临时文件和WebView在结束/取消/Activity销毁释放；不修改主聊天输入/导航/上传/下载模块。

S01_INVALID_PAGE；S02_SNAPSHOT_FAILED（包括15秒捕获超时）；S03_SNAPSHOT_TOO_LARGE；S04_HTML_WRITE_FAILED；S05_MARKDOWN_WRITE_FAILED；S06_STATIC_WEBVIEW_FAILED（包括30秒渲染超时）；S07_PRINT_FAILED；S08_PAGE_CHANGED_BEFORE_SNAPSHOT；S09_SAVE_PICKER_FAILED；S10_SAVE_FAILED。新路径不使用H02/H03/H06。

诊断只含app/buildRevision/Android/WebView、随机snapshotId、origin、格式、字符数、阶段与耗时，不含正文/真实消息ID/Cookie/token/完整URL query。打印onLayout/onWrite仅记生命周期与页范围个数；不伪造回调成功。

## 自动及人工验收

`tools/snapshot/browser.test.cjs`：同clone、live不变、mutation隔离、脚本/SVG/控件剥离、UTF-8富文本、可信URL/title保护、过量拒绝、公开角色与pre-wrap、100+屏长fixture。

`FrozenPageSnapshotTest`：实际SAF .html/.md字节、同对象打印源、系统打印界面保存实际多页PDF、取消/重复、Activity重建/renderer恢复、Cookie/诊断。`scripts/snapshot_android.py`取回文件，核对UTF-8/首尾/code/table/math及late marker不存在，pdftotext/pdfinfo；实际打开HTML，继续原WebView/上传/权限/下载/清理/分享/全IME/native/原签名v1升级检查。

人工在Android35/36保存并打开PDF，检查中英法/emoji/长代码/表格/链接/图片/分页、首尾、返回聊天与取消/重复打印；同一保存HTML在Firefox Android打印做A/B，记录页数/边距/字体/分页差异，不能用桌面Firefox代替。桌面Chrome/Chromium、Firefox和Edge实际打开保存的HTML；禁止把Chromium结果冒充Edge测试。真实账号短会话/长会话也应检查当前挂载内容。

当前验证进度以handoff/latest及CI实际结果为准，未运行项明确待验证。此前MHTML实验和失败记录留在Git历史，不继续开发。

三种格式严格对应同一次当前网页冻结快照，但无法证明ChatGPT服务器端完整会话历史；如果网页虚拟化未挂载较早内容，保存结果也不会包含那些内容。

## 本轮已执行桌面验证（2026-10-06）

同一实际保存HTML fixture以file://在独立测试Chromium143.0.7499.4、Firefox144.0.2、Edge154.0.4258.62打开，UTF8中文/法语/emoji/代码/表格/数学/首尾和data图片解码、无脚本/HTTP请求、白底打印CSS通过。截图人工查看中文及emoji显示正常。初始基础fixture的Chromium桌面PDF18页；扩展长代码/表格后31页，pdftotext检出中文/café/首尾/code/table；不是Android PDF/Firefox Android A/B。浏览器版本及结果见tools/snapshot/evidence/desktop-compatibility.json。

最初沿用的1px图片存在IDAT CRC错误，已替换为有正确校验的实际PNG并保留图片解码断言（没有放宽测试）。系统托管Chromium的file策略限制不代表HTML错误；最终结果用独立测试版Chromium及真实Firefox/Edge，三者均file直接打开，无更改既有浏览器策略。`compatibility.cjs`明确区分file与可能的localhost降级，不冒充结果。

已新增FirefoxSnapshotTest受控Android A/B：官方Firefox157.0.1 x86_64仅在CI安装，不加入生产依赖；同一个frozenHtml通过临时loopback fixture服务交给Firefox，以正常菜单Save as PDF保存并拉回实物。此测试不使用私有浏览器打印API、不包含真实会话、不发送正文给第三方，结果待CI，不宣称通过。真机人工对照仍应执行。
