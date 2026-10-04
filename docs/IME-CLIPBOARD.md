# 输入法剪贴板修复（1.3.7）

## 问题与通路

1.3.6 的兼容脚本只捕获 DOM `paste`。Android 输入法的剪贴板历史可以直接调用 `InputConnection.commitText`，此时没有 DOM `paste`，多行文本仍走旧 Chromium 的逐行编辑路径。仓库此前已经记录这条路径的卡顿，并在 `ClipboardProbeTest.matrix` 中跳过所有多行 IME commitText；正式检查脚本也把此项失败列为可忽略诊断。这个缺口与用户报告的“输入法剪贴板粘贴无反应”相符，具体手机和输入法仍需要实机确认。

## Firefox 参考

- [Firefox GeckoInputConnection.java](https://searchfox.org/firefox-main/source/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/GeckoInputConnection.java)：`performContextMenuAction(android.R.id.paste)` 读取文本后调用 `commitText`；`commitText` 区分选区替换与组合输入，并以 batch edit 保持选区更新顺序。
- [Android InputConnectionWrapper](https://developer.android.com/reference/android/view/inputmethod/InputConnectionWrapper)：提供转发输入连接的正式接口，并支持带 TextAttribute 的现代重载。

本补丁参考其输入连接、选区与组合输入的设计，使用 Android WebView 的委托连接实现，没有迁移到 GeckoView，也没有复制 Firefox 的源码。

## 实现

`NovaWebView` 包装 WebView 创建的原始 InputConnection。没有活动组合输入、光标参数为常用的 `1` 时，长度至少 4096 UTF-16 单元，或包含换行且长度超过 1 的提交使用现有聊天框插入函数；单个 Enter、普通短文本、其他光标参数和组合输入继续委托 WebView。两种 commitText 重载都覆盖。

IME Paste / Paste as plain text 在适用的聊天框中使用同一插入函数。原生剪贴板仅在用户触发粘贴且已确认聊天框之后读取。直接 commitText 使用输入法已经给出的文本，不访问系统剪贴板。

插入仅适用于 `https://chatgpt.com` 上可见、可编辑且获得焦点的聊天框；不适用的输入委托原始连接。异步 JS 插入完成前，后续编辑命令按顺序排队，完成后回到原输入连接的 Handler。导航、重新创建连接或 closeConnection 取消旧连接的待执行命令。CR/CRLF 按 HTML 编辑器规则归一化为 LF，避免成功插入后错误回退造成重复。

## 回归验证

`ClipboardProbeTest` 包含 9 项检查：系统长按粘贴、IME 粘贴命令、原生菜单、两种 IME commitText 重载，以及选区与紧接着的输入顺序、中文组合输入、普通输入框/密码框原生回退和取消旧连接。

文本矩阵逐字比较 textarea 和 contenteditable 的短中文、短多行、CRLF、1/10/50 KB、Markdown、尾部换行及 Unicode/emoji。不再跳过多行 commitText。`.github/workflows/clipboard-regression.yml` 在 PR 中构建调试 APK 并运行 Android API 33/34/35；原正式签名流程也将所有剪贴板检查设为必需。

这些检查使用测试 APK 本地提供的受控网页；它们不等同于特定厂商输入法、真实账号下的 ChatGPT 编辑器或物理设备实测。CI 状态和原始文本比较结果以对应 Actions 运行及其 Artifact 为准。
