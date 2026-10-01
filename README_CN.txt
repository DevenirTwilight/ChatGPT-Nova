ChatGPT Nova (Unofficial) - Android 第二登录容器

用途
- 与官方 ChatGPT Android App 同时安装。
- 本 APK/工程拥有独立的应用数据和 Cookie，可登录另一个 ChatGPT 账号。
- 页面直接加载 https://chatgpt.com/ 。本应用不保存 ChatGPT 用户名/密码，也不把密码写进源码。

当前工程
- compileSdk 35
- targetSdk 35
- minSdk 26
- Android Gradle Plugin 8.7.3
- Gradle 8.9
- Java 17
- Release versionCode 2 / versionName 1.1.0
- Application ID: com.example.chatgptnova

主要功能
- WebView 打开 chatgpt.com
- 独立 Cookie / WebStorage / 缓存
- Cookie 持久化
- 文件选择/上传
- 摄像头和麦克风网页权限
- Android 返回键网页返回
- 非 HTTP(S) 链接交给系统应用
- Android DownloadManager 下载文件到 Downloads
- 清除本应用登录数据
- 页面加载失败提示

GitHub Actions 构建（推荐）
1. 把整个工程上传到 GitHub 仓库根目录。
2. 在仓库 Settings → Secrets and variables → Actions 中建立 4 个 Repository secrets：
   - NOVA_KEYSTORE_B64
   - NOVA_STORE_PASSWORD
   - NOVA_KEY_ALIAS
   - NOVA_KEY_PASSWORD
3. 打开 Actions。
4. 运行 “Build ChatGPT Nova Release APK”。
5. 构建成功后下载 Artifact：ChatGPT-Nova-release。
6. Artifact 内包含：
   - ChatGPT-Nova.apk
   - ChatGPT-Nova.apk.sha256

工作流会：
- 安装 JDK 17
- 安装 Android SDK Platform 35 / Build Tools 35.0.0
- 使用 Gradle 8.9
- 从 GitHub Secrets 临时恢复 release 签名文件
- 执行 :app:assembleRelease
- 用 apksigner 验证签名
- 输出 ChatGPT-Nova.apk

Release 签名安全
- 源码包不包含 .jks 私钥，也不包含明文 keystore.properties。
- release 私钥只通过 GitHub Repository Secrets 注入，构建完成后不会进入源码仓库。
- 请单独离线保存签名备份；丢失原签名私钥后，新版本将无法覆盖安装已经发布的同包名 APK。
- 不要把签名备份、密码或 NOVA_KEYSTORE_B64 提交到 GitHub。

本地构建
- 安装 Android Studio / Android SDK 35 / JDK 17。
- 工程没有附带 Gradle Wrapper JAR；可让 Android Studio 配置 Gradle 8.9，或在系统安装 Gradle 8.9 后运行：
  gradle :app:assembleRelease
- Release APK 默认位置：
  app/build/outputs/apk/release/app-release.apk

登录兼容性
- 这是 WebView 容器，不是 OpenAI 官方 Android 客户端。
- 某些 Google / Microsoft / Apple OAuth 流程可能拒绝嵌入式 WebView。
- 菜单提供“用系统浏览器打开当前页”，作为登录或页面兼容性备用方案。
- 系统浏览器和 WebView 的 Cookie 不共享，因此外部 OAuth 是否能无缝回到 WebView 取决于网站自身流程；不要通过抓取或模拟 OpenAI 私有登录 API 解决。

注意
- 这是非官方客户端，不是 OpenAI 官方发布。
- 菜单“清除第二账号登录数据”只清除此 APK 的 Cookie、缓存和网站数据，不影响官方 ChatGPT App。
