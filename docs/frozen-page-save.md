# 当前网页冻结快照三格式原型

本轮基线为远端 feature/export-conversation 的 2b86b68，不回退历史修复。

## 数据流与范围

原型入口“保存当前网页（原型）”→确认冻结此刻→只调用一次 evaluateJavascript →同步读取公开显示状态并 clone document.documentElement →仅在 clone 上净化/转换→不可变 FrozenPageSnapshot →格式选择。

- Markdown：直接写 FrozenPageSnapshot.markdown，通过 SAF 保存。
- MHTML：独立无 JS SnapshotWebView 加载唯一 frozenHtml → saveWebArchive →非空缓存 .mhtml →SAF 流式复制。
- PDF：同一独立 WebView → createPrintDocumentAdapter → PrintManager.print →用户在 Android 打印界面选择保存为 PDF。

Markdown 与 MHTML 不相互解析，不再次读取 live DOM。一次冻结之后，live 页面可以继续生成/变化；格式生成使用原对象。导航在捕获回调完成之前会取消；之后静态快照不跟随官网。没有滚动、backend 调用、fetch/XHR 包装、长期 observer、存储/凭据读取、稳定 ID 或历史合并逻辑。

只保存此时已加载的页面，不声明服务器端完整 ChatGPT 会话历史。虚拟化未挂载的旧内容不会恢复。

## 静态化与安全

保留已有正文、样式、图片、表格和数学 DOM；移除脚本、事件属性、iframe/object/embed、自动 refresh、原 base 和非 stylesheet link。添加当前地址 base 与禁止 JS/连接/frame/form 的 CSP。输入框 value 不序列化；textarea 仅复制当前公开控件状态，不输出到诊断。

JS 同步事务内先读取公开可见性/滚动布局和控件状态，深 clone 后所有正文转换只读 clone。为了阅读/打印已加载的长内容，解除已识别滚动容器的高度/overflow 限制；不触发历史加载。

独立 WebView 禁 JS、窗口、file/content 访问和导航，未安装聊天桥。静态 CSS/图片/字体经限时、限量 HTTPS 加载器取得，不发送 Cookie 或 Authorization，不请求 backend-api/api；资源不可用时允许缺图/缺样式，不能称资源完整。系统全局 Cookie 设置不改变。取消、失败、清除账号、renderer recovery 和 Activity destroy 会关闭临时 WebView；打印由 adapter.onFinish 关闭，没有以该回调宣称保存成功。

## 不可变性的边界

冻结的是 DOM 与文本，不是所有远程资源字节。CSS/图片在静态加载期间可能变化或缺失；需要账号资源尤其可能不可用。canvas bitmap、shadow DOM、运行时 JS state 不序列化；srcset 移除以避免静态页面另选响应式资源，保留已有 src。不通过私有 API 恢复它们。

MHTML 是单文件网页归档格式，兼容性取决于浏览器；不是普通 .html 文件。保存内容可能包含正文里的私密信息，应由用户自行管理。诊断只含随机 snapshotId、格式、origin、字符/字节数、阶段、版本和耗时，不含真实会话 ID、正文或私密 URL/query。

## 验证入口

- tools/snapshot/browser.test.cjs：同一次冻结的 HTML/Markdown、多语言与结构、后续 mutation、标题/URL 变化、可信源/体积限制、live DOM 未改动。
- FrozenPageSnapshotTest：同一个对象的实际 SAF Markdown 与实际 MHTML；另一原生 MHTML 保存路径；实际系统 WebView print adapter 多页 PDF；取消/重复、生命周期/renderer recovery、Cookie 与诊断。
- scripts/snapshot_android.py：拉取真实文件，用 Python email 解析 MIME HTML part、对比首尾/前后 mutation markers；pdftotext 检查 PDF；Chromium 实际打开 Android .mhtml 并截图；继续跑已有上传/权限/下载/清理/导航/分享/输入/native 测试。
- 旧 DomTrialExportTest 与浏览器历史扫描测试保留并继续运行；ConversationExportTest 的旧私有 reader/pagination 路线历史已失效，不将其失败混作新快照失败，也不删除证据。

当前为 Commit A 原型，保留 Gecko 依赖和旧入口；原型 PDF 已不调用 Gecko。Commit B 的正式入口切换及移除生产依赖必须等待 A 的受控检查通过。人工真实 PDF、Firefox Android 对照、真实登录与用户 WebView153尚需明确记录，不能用模拟器替代。

## 人工验收

在 Android35/36：冻结可控长页→打印 / 保存为 PDF→选择 A4/Letter与方向并实际保存→打开 PDF 逐页检查首尾、中文/法语、代码、表格、长段落、链接与重叠。返回 Nova、取消和重复打印，检查聊天输入与菜单仍正常。相同 fixture 在 Firefox Android 保存一次 PDF 进行阅读/分页对照，不要求字节相同。

MHTML用至少一个支持它的 Chromium 浏览器打开；Markdown用 UTF-8 阅读器核对标题/段落/fence/表格/引用/列表/链接/多语言及无交互控件垃圾。三格式都不应含冻结后新增的 marker。

## 错误码

F01 可信源/地址不符；F02 捕获期间地址/标题变化；F03 快照超限；F04 转换/解码失败；F05 捕获超时；F06 静态渲染超时；F07 渲染失败；F08 文件写入失败；F09 归档失败/空文件；F10 无法启动打印；F11 无法启动系统保存选择器；F12 写入保存位置失败。实际诊断码带完整后缀，如 F09_ARCHIVE。取消为正常结束，不报告保存成功。
