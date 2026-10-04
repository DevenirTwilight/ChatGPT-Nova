# 1.3.7 键盘剪贴板粘贴

用户在 2026-10-04 确认卡住的入口是输入法的剪贴板条目。它通常调用 `InputConnection.commitText(text, 1)`，与长按菜单的 `performContextMenuAction(android.R.id.paste)` 不同。网页 `paste` 监听器不能覆盖前者。此前 `ClipboardProbeTest` 还跳过了这条路径的换行用例，并将它列为可失败的诊断；旧 CI 通过不能证明用户使用的路径可用。

## Firefox 源码对照

查阅 Mozilla 官方 [GeckoInputConnection.java，固定 revision 66b70484481af2e01d4da8bb33f4a756aba77d74](https://searchfox.org/firefox-main/rev/66b70484481af2e01d4da8bb33f4a756aba77d74/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/GeckoInputConnection.java)。其 `commitText` 在没有活动组合文本时，先在 batch edit 中替换完整选区，然后更新光标并结束 batch；活动组合文本及特定键盘的单字符输入走原生实现。`performContextMenuAction` 的纯文本粘贴也会调用 `commitText`。

这个实现使用 Gecko 的 Editable、IPC 与文本模型，不能直接放入 Android Chromium WebView。Nova 实现等价的“整段处理后再继续后续编辑”顺序，没有复制 Mozilla 的源码文件，也没有引入 Gecko 引擎。Firefox 源文件保留其 MPL-2.0 许可；本次是基于公开实现的独立 WebView 适配。

## Nova 的输入法适配

`ComposerWebView` 包装原有 InputConnection。默认光标参数为 1，且文本至少 1024 个 UTF-16 单元，或包含换行并超过一个字符时，尝试整段提交：

1. 使用 IME 已提供的文本，不读取或改写系统剪贴板。
2. Java 和 JS 均确认当前页面为可信 HTTPS `chatgpt.com`；JS 只允许当前聚焦的、可编辑的聊天输入框。不会使用原生菜单记住的目标。
3. 将纯文本提供给网页的粘贴处理。网页接管时不重写 DOM；未处理时使用既有的一次性纯文本回退。
4. 在异步事务完成前排队保存后续输入、删除、选区和 batch 操作，随后按原顺序交回原有 InputConnection。不会把正文拆成逐行插入。

密码字段及其他 origin 在创建输入连接时直接排除，不进入适配器。非默认光标参数、活动组合文本及短单行输入使用原始连接；焦点不在聊天输入框时回退到原始连接。页面导航/关闭使旧连接队列失效，旧文本不会被重放到新页面。适配器不记录或持久化文本，不自动发送草稿，不增加 JavaScript 到 Android 的接口。它不改变 Web Share 或登录实现。

## 验证与限制

- 所有四种 Android 粘贴入口的 1/10/50 KB、多行、Markdown、中英文、emoji、结尾换行用例均为必测，不再跳过 IME 多行。
- 网页自有编辑器的约 248 KiB 夹具新增真正的 `commitText` 入口，覆盖两种重载，检查单次事务、完整文本、原选区替换、纯文本标记、光标及一次撤销。记录请求到验证耗时与事务/绘制阶段耗时。
- 键盘路径的性能参考是相同 WebView/相同编辑器的正常 OS 粘贴事务，明确不把它称为未适配 IME 的性能基线。原有时间阈值保持不变。
- 额外检查大文本之后立即打字、删除再打字的顺序，以及组合文本替换、非默认光标参数、非聊天输入框的原生语义；合成密码字段确认没有安装 Nova 输入法 wrapper。

受控夹具通过仍不等于真实 ChatGPT 编辑器及用户手机已经通过。版本保持 1.3.7 / versionCode 11；PR #1 在实际键盘剪贴板和网页 Share 验收之前保持未合并。
