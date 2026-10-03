# Nova 分享功能设计决策

## 背景

ChatGPT 网页的分享入口依赖 Web Share API（`navigator.share`）。Android WebView 与完整浏览器不是同一能力集合；Nova 当前设计不提供通用 JavaScript → Android 原生接口，因此不把网页脚本直接桥接到 `Intent.ACTION_SEND`。

## 决策

Nova 1.3.7 增加一个原生菜单入口：**分享当前页面**。

流程：

`chatgpt.com` 可信页面 → Nova 原生菜单 → `Intent.ACTION_SEND` (`text/plain`) → `Intent.createChooser()` → Android 系统 Sharesheet。

分享内容仅为当前 WebView 的 HTTPS 页面 URL。入口只在 `chatgpt.com` 精确主机名页面显示，并复用现有可信来源检查。

## 为什么使用受限 JavaScript Bridge

1.3.7 为了修复 ChatGPT 网页自身 Share，在可信 `chatgpt.com` 页面提供一个**单用途** `NovaWebShare.share(title,text,url)` 适配器，并由它调用 Android Sharesheet。它不是通用 JavaScript Bridge：没有 Cookie、Storage、文件、网络、账号或任意 Java 方法。

页面开始导航时立即移除接口；只有页面加载完成且仍通过现有可信来源检查时才重新安装。原生 `shareWebPayload()` 再次检查当前 WebView URL 和传入 URL，只允许 `chatgpt.com`。Android 官方同时明确警告 `addJavascriptInterface()` 的安全风险，因此这个接口必须继续保持最小权限和来源限制。

网页内 Share 与 Nova 原生菜单是两条不同入口，但现在都能进入 Android Sharesheet。网页 Share 仍由 ChatGPT 自己决定共享流程；Nova 不自行创建 `/share/` 链接。

## 为什么不增加 `<queries>`

Nova 不需要通过 `PackageManager` 预先枚举或判断分享目标。Android 官方文档说明，直接启动隐式 Intent 不要求额外的 package visibility；只有需要事先查询可用应用时才需要相应的 `<queries>` 声明。因此本版本不扩大应用对已安装应用的可见范围。

## WebView 能力表述与实测基线

Android System WebView 是通过 Google Play 持续更新的 Chromium 实现，Nova 无法控制用户设备上的 WebView 版本。因此本文件不把「WebView 不提供 Web Share API」写成永久成立的结论，只记录当前事实与实测：

> 截至Nova 1.3.7 的目标环境与实测，Android System WebView 未提供 Nova 所需的 Web Share API（`navigator.share`）能力；因此 Nova 不依赖 `navigator.share`，而使用原生菜单分享。

### 实测基线记录

每次验收时通过 `chrome://inspect` 连接真机 WebView（Android 官方支持 WebView 远程调试），在控制台执行 `typeof navigator.share` 并补充一行记录。若未来某次实测不再是 `"undefined"`，回到「后续」一节重新评估。

| 日期 | Android 版本 | System WebView 版本 | `typeof navigator.share` | 备注 |
| --- | --- | --- | --- | --- |
| 待补（1.3.7 验收时填写） |  |  |  |  |

## 后续

如果未来需要让网页内的分享按钮直接调用 Android Sharesheet，应先重新评估 WebView 安全边界、来源限制和桥接接口的最小权限，而不是直接加入通用 JavaScript Bridge。
