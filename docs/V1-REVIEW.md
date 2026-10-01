# v1 审查与增量改进

v1 基线：提交 fee90be011ac72b6bbb972f0e91265469d2776ab，versionCode 3 / 1.2.0。已交付 APK SHA-256：a48fc6e95bf3b8dd1abc32c86cbee168de7931aa71a80867134b58da61dd2921。包名 com.example.chatgptnova，证书指纹与 release-certificate.sha256 一致。

## 审查发现

- 原生模拟器检查通过，但 Android 15 的实际 portrait.xml / PNG 是 ERR_NAME_NOT_RESOLVED 错误页，不能据此声称网站加载或登录已通过。Android 13、14 的记录能看到 ChatGPT 公开页面。
- 文件选择依赖 FileChooserParams.createIntent 后改 action，没有明确传递所有 MIME 类型和多选语义。
- cancelFileChooser 会删除上一张已经交给网页的相机文件，下一次上传可能打断前次读取。
- Google、Microsoft、Apple 被统一拦截；浏览器登录与 Nova Cookie 隔离，无法自动登录回 Nova。Microsoft、Apple 的可用网页流程不应预先被应用阻止。
- DownloadManager 的静态 Cookie 头无法按应用策略在每一次重定向时重新选择，存在跨主机错误带 Cookie 的风险。
- 无原生网络错误重试页；无大状态 Bundle 限制；清除时新 WebView 已能导航，异步 Cookie 删除尚未完成。
- 未发现原生密码收集、JavaScript 原生密码桥、私有登录 API、OpenAI API Key 或登录代理。TLS 校验、独立 applicationId 和 Secrets 签名方式可保留。

## 为什么保留架构

核心要求需要原应用独立且持久的 WebView 网站数据。现有 Android / Gradle 工程可正常产出可安装、签名正确的 APK，数据隔离机制本身成立。整体替换为浏览器、Auth Tab 或临时 Custom Tabs 不能保证既有 Nova Cookie 保留，也不能凭空获得 ChatGPT 官方 OAuth 回调权限。上述问题集中在上传生命周期、下载传输与 UI 状态管理，局部替换这些实现更容易验证并保持升级兼容。

本版保留 MainActivity、WebView 数据目录、applicationId 和 release 证书；仅将上传与 HTTPS 下载的独立生命周期交给小型控制器，并完善状态与错误处理。没有整体重写。

## 验证边界

CI 使用原始签名 v1 APK 覆盖安装，验证合成 Cookie / localStorage 存续；使用新 Release APK 的真实 WebView 执行受控网页夹具。相机及文件选择由测试 ActivityMonitor 返回合成结果，HTTPS 传输由受控连接返回字节和重定向，不含用户凭据。模拟器证据会同时记录真实网站公开页面是否可见，但不将网络错误或仅有原生菜单当成网站通过。

真实账号长期登录、第三方 OAuth 全流程、硬件相机/麦克风、真实 ChatGPT 附件上传与带登录状态的网络下载仍需用户手机测试。网站与提供商对 WebView 的限制不能通过伪造 User-Agent 或私有认证接口绕过。
