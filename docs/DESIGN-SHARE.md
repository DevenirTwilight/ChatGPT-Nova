# Nova 分享功能设计决策

## 1. 当前能力

Nova 有两条独立分享路径：

1. **ChatGPT 网页 Share**：ChatGPT 前端调用 `navigator.share(...)` 时，Nova 仅在可信 `https://chatgpt.com` 页面且 WebView 尚未提供该 API 时安装最小兼容层，再由 Android `ACTION_SEND` 打开系统 Sharesheet。
2. **Nova 菜单“分享当前页面”**：直接把当前可信 ChatGPT 页面 URL 交给 Android Sharesheet。

OpenAI 当前文档说明，ChatGPT 的 Share 流程会先创建/更新共享链接，再可在移动端打开设备分享面板；真正的共享对话链接使用 `https://chatgpt.com/share/` 前缀。

## 2. Web Share 兼容层

Web Share 要求安全上下文，并且 `navigator.share()` 必须由用户激活触发。Nova 不自动发起分享。

当 WebView 没有原生 `navigator.share` 时，Nova 安装一个单用途 `NovaWebShare` 接口，并在页面 JavaScript 中提供兼容的 `navigator.share(data)`：

`navigator.share(data)` → `NovaWebShare.share(title,text,url)` → `Intent.ACTION_SEND` → Android Sharesheet

如果 WebView 将来原生提供 `navigator.share`，兼容层不覆盖它。

## 3. 安全边界

Android 官方文档指出，`addJavascriptInterface` 会暴露到 WebView 的所有 frame，调用 frame 的来源不能由应用可靠判断。因此这里严格限制桥接能力：

- 只在 `https://chatgpt.com` 精确主机页面安装；
- 页面开始导航时先移除接口；
- 页面加载完成并再次通过可信来源检查后才安装；
- 接口只有一个 `share` 方法；
- 原生侧再次检查当前 WebView 页面仍为可信 `chatgpt.com`；
- 不暴露 Cookie、Storage、账号状态、文件系统、网络请求或任意 Java 方法。

这不是通用 JavaScript Bridge，而是单用途分享适配器；仍应把它视为 WebView 安全边界的一部分。

## 4. 两种分享的语义

网页 Share 的目标是让 ChatGPT 自己完成其共享链接流程，然后把最终提供给 `navigator.share` 的数据交给 Android。Nova 不调用 ChatGPT 私有 API，也不自行伪造 `/share/` URL。

Nova 菜单“分享当前页面”则明确只分享当前 WebView URL。它可能是 `/c/<conversation-id>` 私有会话地址，不能被当作 ChatGPT 共享链接。

## 5. 验收

真机上至少验证：

1. 登录 ChatGPT；
2. 打开具体会话；
3. 点击 ChatGPT 网页 Share；
4. 如果出现分享预览/创建链接，完成 Create link 或 Copy link；
5. 如果进入 Android Sharesheet，确认分享内容是 ChatGPT 提供的最终 URL；
6. 最终共享链接应为 `https://chatgpt.com/share/...`，而不是普通 `/c/...`；
7. 取消系统 Sharesheet 不应导致崩溃；
8. 离开 `chatgpt.com` 后不应保留 Web Share 接口；
9. 在 `chrome://inspect` 中记录 Android 与 System WebView 版本以及 `typeof navigator.share`。

## 6. 后续

如果未来 System WebView 原生支持 Web Share API，兼容层应自动停用。若 ChatGPT 前端改变 Share 实现，使其不再调用 `navigator.share`，应根据实际行为重新定位，而不是假定旧兼容层仍然适用。
