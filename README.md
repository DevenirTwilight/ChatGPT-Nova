# ChatGPT Nova

一个非官方 Android 网页容器。网页内容来自 https://chatgpt.com/ ，不由 OpenAI 发布、维护或背书。

安装 Release 构建的 `ChatGPT-Nova.apk` 后，它使用自己的应用包 `com.example.chatgptnova` 和 WebView 数据目录，可与官方 ChatGPT App 同时安装。Android 8.0（API 26）及以上可以安装。

## 使用

启动后进入官网。右上角菜单支持刷新、回到首页、清除 Nova 的登录数据、使用系统浏览器打开和关于页面。文件上传支持系统文件 / 图片选择器以及拍照；相机和麦克风只为 HTTPS 的 chatgpt.com 及其子域请求 Android 权限。Cookie 在页面完成和应用暂停时写入本地；清除时等待 Cookie 删除完成再加载首页。

普通 HTTP(S) 下载由系统 DownloadManager 保存到下载目录，并携带该下载地址自身的 Cookie。用户点击的 blob 下载通过系统保存对话框写入所选文件，分块处理，上限 256 MiB。不会将 Cookie 发给代理服务。HTTP 页面通过系统浏览器打开，HTTPS 页面保留在 WebView，拒绝本地 file/content 页面跳转。支持安全处理 intent 链接以及交给系统的其它外部链接。WebView 的 TLS 验证和 Safe Browsing 保持启用。

## 登录限制

Google / Microsoft / Apple 可能禁止 WebView 登录。检测到这些提供商时，会提供系统浏览器 / Custom Tabs 登录入口，从 ChatGPT 官网重新开始登录。**浏览器与 WebView 不共享 Cookie，浏览器登录不能自动迁移为 Nova 的登录状态，也没有自行实现或逆向官方 OAuth 回调。** 要在 Nova 的网页容器内保持第二账号，请使用官网当前提供且在 WebView 中可用的登录方式，例如邮箱登录。网站或身份提供商的限制仍可能影响登录，需要在实际手机和账号上确认。

应用不读取或记录账号密码，不含 AI 后端、OpenAI API Key、JavaScript 原生接口、第三方登录代理或密码存储逻辑。下载 blob 时执行的网页脚本只读取用户点击的那个 blob，不读取登录字段或 Cookie。Android 和官方网页自己的认证 Cookie 保存在应用独立数据目录中。

## 构建与签名

- compileSdk / targetSdk: 35；Build Tools: 35.0.0
- AGP: 8.7.3；Gradle: 8.9；Java: 17
- 构建：`gradle :app:clean :app:assembleRelease`
- CI 还运行 lint、apksigner、Manifest / DEX 和 SHA-256 检查。
- 本地签名配置见 `keystore.properties.example`；真实私钥和 properties 不在源码内。
- GitHub Secrets: `NOVA_STORE_PASSWORD`、`NOVA_KEY_ALIAS`、`NOVA_KEY_PASSWORD`、`NOVA_KEYSTORE_B64`。

仓库的 `.github/workflows/build-from-zip.yml` 解压安全源码 ZIP，校对其中的 app 与仓库源码一致，然后从 Secrets 恢复临时签名材料。Artifact `ChatGPT-Nova-release` 包含实际签名 APK、SHA-256 和静态验证结果；`ChatGPT-Nova-source` 包含 CI 生成的 Gradle 8.9 wrapper 和完整工程，自动排除签名私钥与配置。

Android 13 / 14 / 15 的模拟器使用同一个 Release APK 运行 `scripts/smoke_apk.py`，检查安装、启动、刷新、关于、清除数据、旋转、返回与重启，并保留截图和日志。这个测试不冒充已验证真实账号登录、长期登录、上传、语音或 OAuth；这些功能仍需要手机上的官网实际测试。
