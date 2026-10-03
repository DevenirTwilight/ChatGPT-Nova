# Nova 分享功能设计决策

## 背景

ChatGPT 网页的分享入口依赖 Web Share API（`navigator.share`）。Android WebView 与完整浏览器不是同一能力集合；Nova 当前设计不提供通用 JavaScript → Android 原生接口，因此不把网页脚本直接桥接到 `Intent.ACTION_SEND`。

## 决策

Nova 1.3.7 增加一个原生菜单入口：**分享当前页面**。

流程：

`chatgpt.com` 可信页面 → Nova 原生菜单 → `Intent.ACTION_SEND` (`text/plain`) → `Intent.createChooser()` → Android 系统 Sharesheet。

分享内容仅为当前 WebView 的 HTTPS 页面 URL。入口只在 `chatgpt.com` 精确主机名页面显示，并复用现有可信来源检查。

## 为什么不使用 JavaScript Bridge

不引入 `addJavascriptInterface()` 或其他通用 JavaScript → Android API。这样可以保持 README 中既有的“无 JavaScript 原生接口”安全边界，不让远程网页 JavaScript 获得调用 Nova 原生能力的通道。

网页内分享按钮与原生菜单入口是两个不同能力：本版本保证原生菜单可以分享页面链接，但不声称修复网页内部的 `navigator.share` 调用。

## 为什么不增加 `<queries>`

Nova 不需要通过 `PackageManager` 预先枚举或判断分享目标。Android 官方文档说明，直接启动隐式 Intent 不要求额外的 package visibility；只有需要事先查询可用应用时才需要相应的 `<queries>` 声明。因此本版本不扩大应用对已安装应用的可见范围。

## 后续

如果未来需要让网页内的分享按钮直接调用 Android Sharesheet，应先重新评估 WebView 安全边界、来源限制和桥接接口的最小权限，而不是直接加入通用 JavaScript Bridge。
