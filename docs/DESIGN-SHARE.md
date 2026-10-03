# Nova 1.3.7 分享设计与验收

版本保持 `1.3.7` / `versionCode 11`，工作仅在 PR #1 和 `fix/native-share-1.3.7`。

## 网页 Share

主要目标是让 ChatGPT 网页自己的 Share 流程在 Nova 中可用。网页负责生成和提供分享内容；Nova 在 WebView 缺少 `navigator.share` 时提供有限的 Web Share 兼容层。不覆盖已有的原生实现，不调用 ChatGPT 私有接口，不创建或推导公共分享链接。

`/c/<conversation-id>` 是私人会话地址，`/share/<id>` 是公共分享地址。兼容层原样使用网页提供的 URL；没有 URL 时只分享提供的文本或标题，不追加当前私人会话地址。非法 URL 会被拒绝，不替换成当前页面。

## 安装时序与安全边界

旧实现在 `onPageFinished()` 中调用 `addJavascriptInterface()`，而 Android 文档说明新增接口要到下一次页面加载才对 JavaScript 可见。Actions `37133352812` 的 Android 33/34/35 测试均在 `NovaWebShare.share` 为 `undefined` 时失败。这是安装时序的直接回归证据，不能把 APK 含有相应字符串当成运行成功。

现在使用 AndroidX WebKit 的 `WebViewCompat.addWebMessageListener()`，在任何 `loadUrl()` 或 `restoreState()` 之前注册，仅允许 `https://chatgpt.com` 默认 443 端口。支持时在 document start 安装兼容函数；较旧的 provider 有 message listener 但没有 document-start script 时，在页面加载完成后安装函数。没有 message listener 支持时不暴露 Java 对象，也不假装支持网页 Web Share。

`NovaWebShare.share(title, text, url)` 与 `navigator.share(data)` 使用同一条单用途消息通道。原生侧只接受 `share` 请求，检查实际 `sourceOrigin`、主 frame、当前 WebView、页面 URL 和传入 URL。子 frame 的请求被拒绝；外部 origin 没有通道。导航取消未完成请求，清除网站数据、进程恢复和 Activity 销毁时释放旧 adapter。

Share adapter 不读取 Cookie、密码、Token、账号、Storage、文件或已安装应用名单。它没有任意 Android 方法、通用命令、认证代理或登录系统。

## 有限 Web Share 行为

- 支持 title、text 和可信 ChatGPT URL；拒绝文件、空数据、userinfo、非 HTTPS、外部主机和非 443 端口。`canShare()` 使用同样的校验。
- `share()` 要求页面用户手势，同一文档只允许一个未完成请求。
- Promise 在系统报告用户选择分享目标后完成；关闭 Sharesheet、页面导航或启动失败会拒绝 Promise，不会无条件返回 `Promise.resolve()`。Android 返回的 Activity 结果不能单独证明是否选择了目标，因此使用 chooser 的 `IntentSender` 回调。
- 目标选择回调不代表接收应用完成了发送，也不代表公共会话创建成功。

## Nova 原生菜单

“分享当前页面”仍是独立辅助入口，分享当前 HTTPS 页面地址。它可能分享 `/c/...` 私人会话 URL；这不是 ChatGPT 公共分享，也不能作为网页 Share 已修好的证据。不枚举分享应用，不为 `ACTION_SEND` 添加额外 `<queries>`。

## 验证与发布条件

CI 检查签名、包名及 `1.3.7 / 11`；构建后另行核验 APK 中的 `NovaWebShare`、`navigator.share`。`WebShareTest` 从测试 APK 提供的受控网页点击按钮，经过真实 Release WebView 调用 `navigator.share()` 并打开系统 Sharesheet。测试覆盖提供的公共形式 URL、目标选择回调、取消与重试、用户手势、非法数据、SPA/重载、iframe 和外部来源限制。目标选择回调由测试夹具触发，不是向真实应用发送会话。

自动化夹具不能证明当前 ChatGPT 前端使用了同一调用链。合并前还必须在真机打开自己的 ChatGPT 会话，点击网页自己的 Share，验证公共分享链接及 Android Sharesheet，并实际检查接收目标的内容。

| 日期 | Android / WebView 版本 | 真正的 ChatGPT 网页 Share | 结果 |
| --- | --- | --- | --- |
| 待实机验收 | 待填写 | 自己的会话 → 网页 Share → 系统 Sharesheet → 接收目标 | 未验证 |

若实机仍失败，根据实际网页调用、Promise、SPA 和 frame 证据定位，保持现有来源边界；CI 全绿且实机通过后才考虑合并。

## 官方依据

- [WebView.addJavascriptInterface](https://developer.android.com/reference/android/webkit/WebView#addJavascriptInterface(java.lang.Object,java.lang.String))：接口可见时序与所有 frame 的暴露范围。
- [WebViewCompat](https://developer.android.com/reference/androidx/webkit/WebViewCompat)：origin rules、message listener 与 document-start script。
- [Web Share 标准](https://www.w3.org/TR/web-share/)：数据校验、用户手势、Promise 和取消行为。
- [Android Sharesheet](https://developer.android.com/training/sharing/send)：chooser 与目标选择回调。
