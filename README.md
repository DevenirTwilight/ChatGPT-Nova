# ChatGPT Nova

一个非官方 Android 网页容器。网页内容来自 https://chatgpt.com/ ，不由 OpenAI 发布、维护或背书。

安装 Release 构建的 `ChatGPT-Nova.apk` 后，它使用自己的应用包 `com.example.chatgptnova` 和 WebView 数据目录，可与官方 ChatGPT App 同时安装。Android 8.0（API 26）及以上可以安装。

## 使用

启动后进入官网，顶部显示 ChatGPT Nova 和当前域名。右上角菜单提供刷新、ChatGPT 首页、用浏览器打开和设置；页面明确显示未登录时增加“登录”。设置集中放置退出当前账号（明确已登录时）、清除登录与网站数据、登录帮助和关于 ChatGPT Nova。退出与数据清除都有确认，不影响官方 App 或浏览器。文件上传支持系统文件 / 图片选择器以及拍照；相机和麦克风只为 HTTPS 的 chatgpt.com 及其子域请求 Android 权限。Cookie 在页面完成和应用暂停时写入本地；清除时等待 Cookie 删除完成再加载首页。

HTTPS 下载与 blob 下载均通过系统保存对话框写入用户选择的位置，不需要存储权限。HTTPS 在后台线程流式写入，只向下载开始时的 ChatGPT 同源地址发送 Cookie；一旦跨源，后续全程不携带 Cookie，拒绝 HTTPS 降级为 HTTP；blob 分块保存，上限 256 MiB。下载进行时请保持 Nova 运行，清除数据或销毁 Activity 会取消未完成下载；失败时保存位置可能有不完整文件。不会将 Cookie 发给代理服务。HTTP 页面及 HTTP 下载通过浏览器打开。支持安全处理 intent 链接和其它外部 scheme，拒绝本地 file/content/javascript/data 页面跳转。TLS 验证和 Safe Browsing 保持启用。

工具栏显示当前网站主机名。网络错误提供原生重试、首页和浏览器入口；HTTP 错误保留网站自身的页面，避免遮挡身份提供商的正常提示。清除数据等待 Cookie 删除完成后再打开首页。保存的 WebView 状态限制为 256 KiB，过大或失效时以当前 HTTPS 地址恢复。相机成功返回的文件保留供网页读取，后续选择文件不会删除前次照片；七天前的临时照片会在启动时清理，主动清除 Nova 登录数据也会删除临时照片。

## 登录方式与限制

登录状态是界面提示，不是认证凭据：Nova 只检查 ChatGPT 页面可见的公开登录/账号控件，不读取输入框内容、Cookie、Storage 或私有认证接口。官网控件变化、加载失败或状态不明确时，菜单保持常用四项；设置中的登录帮助与数据清除始终可用。浏览器中的登录状态不会让 Nova 菜单显示为已登录。

默认的登录与聊天都使用 Nova 自己的 WebView 和 Cookie。“登录”或设置中的“登录帮助”可选择“应用内登录”，直接加载官方公开页面 https://chatgpt.com/auth/login ，不会打开系统浏览器。所有 HTTPS 跳转（包括 intent 链接里的 HTTPS 地址）优先在 Nova 内处理。

**Google 登录可选择在应用外完成。** 在 Nova 内点击 Google，或从“登录”/设置“登录帮助”进入登录方式选择后，可以选择“浏览器登录”。应用在打开前明确说明两个会话独立；取消会留在 Nova。优先使用已选择的 Chrome / Brave，否则尝试已安装的 Chrome / Brave，再回退到可用浏览器。官网认证与聊天由浏览器负责，没有可用浏览器时提供提示。

浏览器登录从公开页面 https://chatgpt.com/auth/login 开始，请在该页面选择 Google。它不会转发 WebView 中的 Google OAuth 地址、state、Cookie 或其他临时认证数据，避免复用绑定了另一套 Cookie 的登录流程。从 Google 提示进入浏览器时，Nova 自己也恢复到内部官网登录页，避免返回后停在空白 Google 页面。

**浏览器登录成功后，可继续在同一浏览器 / Custom Tab 中聊天。关闭浏览器页面可以回到 Nova，但不会自动建立 Nova 内的登录会话。** 浏览器现有的 ChatGPT / Google 登录会影响该外部流程；需要第二个账号时，应在官网确认所选账号。Nova 清除登录数据只作用于自己的 WebView，不清除浏览器的账号或 Cookie。

只有账号本身支持邮箱密码或验证码，并且官网允许 WebView 完成认证时，才能在 Nova 内通过这些方式登录。邮箱登录不是所有第三方账号的通用替代方案。网站验证、提供商政策、账号设置与手机 WebView 版本可能影响登录；公开登录页可见和合成 Cookie 测试通过均不代表真实账号认证成功。

菜单“用浏览器打开”用于外部查看当前页面。“浏览器登录”专门开启新的官网登录流程。二者都使用浏览器自己的数据；应用没有自行实现或逆向官方 OAuth 回调，也没有将浏览器 Cookie 搬回 Nova。

应用不读取或记录账号密码，不含 AI 后端、OpenAI API Key、通用 JavaScript 原生接口、第三方登录代理或密码存储逻辑。为兼容 ChatGPT 网页 Share，1.3.7 仅在可信 chatgpt.com 页面提供单用途 Web Share 适配器调用 Android Sharesheet。下载 blob 时执行的网页脚本只读取用户点击的那个 blob，不读取登录字段或 Cookie。Android 和官方网页自己的认证 Cookie 保存在应用独立数据目录中。

公开 OAuth 能力与 Nova 网页会话的区别、近期官方 Android 回调观察，以及尚未确认的后台限制，见 [浏览器登录回调研究](docs/LOGIN-CALLBACK-RESEARCH.md)（2026-10-02）。

## 第三方登录风险提醒

Nova 是非官方第三方客户端，不由 OpenAI 发布、维护或背书。Nova 的应用内登录会加载 `chatgpt.com` 官方登录页，但登录与会话仍运行在 Nova 的 WebView 中。第三方客户端的登录/访问方式可能与官方客户端不同，不能保证不会触发额外的安全验证、访问限制或其他账号问题。

这**不等于“使用非官方客户端一定会封号”**；本项目不作这种保证或判断。用户如果不愿承担第三方客户端带来的这项不确定性，应使用官方 ChatGPT Android App 或浏览器完成登录和使用。

## 构建与签名

- compileSdk / targetSdk: 35；Build Tools: 35.0.0
- AGP: 8.7.3；Gradle: 8.9；Java: 17
- 构建：`gradle :app:clean :app:assembleRelease`
- CI 还运行 lint、apksigner、Manifest / DEX 和 SHA-256 检查。
- 本地签名配置见 `keystore.properties.example`；真实私钥和 properties 不在源码内。
- GitHub Secrets: `NOVA_STORE_PASSWORD`、`NOVA_KEY_ALIAS`、`NOVA_KEY_PASSWORD`、`NOVA_KEYSTORE_B64`。

仓库中的标准 Gradle 工程是构建来源。`.github/workflows/build-from-zip.yml` 保留原工作流入口，但直接使用仓库的 Gradle wrapper 构建，不再依赖 ZIP 解压后的另一份源码。`ChatGPT-Nova-Secure-Source.zip` 是可导入 Android Studio 的快照，CI 每次自动生成完整安全源码 Artifact。签名仅由 Secrets 恢复，最后清理临时文件；公开的 `release-certificate.sha256` 是证书指纹，用于阻止误换签名，不含私钥。

Artifact `ChatGPT-Nova-release` 包含签名 APK、SHA-256 和静态验证报告，`ChatGPT-Nova-source` 包含完整工程。独立的 `ChatGPT-Nova-ci-inputs` 仅用于模拟器检查，里面的测试 APK 不是用户安装包。

Android 13 / 14 / 15 的模拟器均检查：原 v1 `install -r` 覆盖安装及合成 Cookie / localStorage 保留；受控网页的多文件输入、拍照结果生命周期、blob 字节完整性、HTTPS 下载重定向 Cookie 隔离与降级拒绝、权限范围、网络错误恢复和清除数据；原生菜单、旋转、返回与重启（原生自动化由测试 APK 提供受控页面，并使用持续的系统无障碍连接；公开网页检查脚本 scripts/smoke_apk.py 用于手动设备检查）。新增原始 WebView 剪贴板诊断：真实长按菜单粘贴、IME 粘贴命令与 IME commitText，逐字比较 1/10/50 KB、多段换行、Markdown、中英文与 emoji，分别覆盖 textarea 和 contenteditable，并记录 WebView 版本。网页与外部文件/相机结果是合成测试夹具，不代表真实账号上传或硬件拍摄已实测。测试夹具及其 ContentProvider 只存在于单独的测试 APK，未打包进 Release。原始 v1 Artifact 过期后，CI 从固定的 v1 提交和原签名重建兼容性基线。 如果模拟器日志明确显示 Google 系统字体提供者重启并连带终止 Nova，CI 只重跑一次完整原生检查并保留初次失败证据；Nova 自身崩溃及其他检查失败不会被此机制跳过。

## 品牌资源

1.3.1 的原创图标使用两层聊天气泡和镂空 Nova 星核，采用深蓝、薄荷绿与浅蓝。彩色 adaptive icon、Android 13+ 主题图标、安装页图标、标准启动页和关于页保持一致。完整矢量源图、512px 导出和图标预览位于 `docs/branding/`。启动页由 AndroidX SplashScreen 管理，应用准备好即可进入，不添加人为延迟。

详见 `docs/V1-REVIEW.md`、`docs/branding/README.md` 和 `CHANGELOG.md`。
