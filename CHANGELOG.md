# 1.3.4 (versionCode 8)

- 顶部只显示 ChatGPT Nova 和当前域名；非官方性质与独立登录说明移至“关于 ChatGPT Nova”。
- 右上角改为轻量弹出菜单：刷新、ChatGPT 首页、用浏览器打开、设置；页面明确显示未登录时才增加“登录”。
- 设置集中放置退出当前账号（页面明确显示已登录时）、清除登录与网站数据、登录帮助和关于。退出与清除均需确认，只作用于 Nova 自己的数据。
- 登录状态仅依据 ChatGPT 页面可见的公开登录/账号控件判断；不读取密码、Cookie、Storage 或私有认证接口。无法确定状态时不假装已登录，仍可从设置打开登录帮助或清除数据。
- 保留 Google 浏览器登录方案、现有图标、上传、下载、权限和 Cookie 持久化；保持 applicationId 与原 release 签名。新增登录/未登录/未知状态、SPA 状态变化及退出取消的真实 WebView 夹具检查。
- CI 固定 Emulator 37.1.11（build 15917651），使用当前支持的 SwiftShader 模式，关闭 Vulkan 与快照并明确内存配置；网页检查后重启模拟器再进行原生检查，不重建或清空其应用数据。修正界面抓取进程异常时未进入已有有限重试的问题，保留 Android 13 / 14 / 15 的完整断言与失败诊断。

# 1.3.3 (versionCode 7)

- Google 登录入口改为可选择的“浏览器登录”，并新增菜单“Google / 浏览器登录”，可在应用外完成官网登录并继续聊天。
- 浏览器从公开的 chatgpt.com/auth/login 开始新流程，优先使用 Chrome / Brave；不转发 WebView 的 OAuth 地址、临时 state 或 Cookie。
- 在打开浏览器前明确说明浏览器会话与 Nova 会话独立；返回 Nova 不会自动建立登录，不伪造认证成功。
- 取消浏览器登录仍留在 Nova；从 Google 提示打开浏览器时恢复 Nova 内的登录页，避免返回时停在空白 Google 页面。
- 保留所有其他功能、原图标、applicationId、release 签名及 Android 35 构建配置；新增受控外部登录 Intent 检查，不声称真实 Google 账号认证已实测。

# 1.3.2 (versionCode 6)

- 延续 1.3.1 的稳定 WebView 登录：直接打开 chatgpt.com，登录与聊天使用 Nova 的独立 Cookie / Storage。
- Google 入口明确显示“不支持该登录方式”；简化登录说明，保留返回 Nova 内官网登录页的按钮。
- 保留浏览器登录回调研究文档；没有加入外部浏览器认证回调、Cookie 导入或 OpenAI API。上一轮只有文档变更，因此无需撤销不存在的回调实现。
- 保留新图标、上传、下载、相机 / 麦克风权限、Cookie 持久化、返回键、外部链接、清除数据及 Android 35 构建配置。
- 保持 applicationId 和原 release 签名，提升版本号以便覆盖安装；继续执行签名、升级和 Android 13 / 14 / 15 检查。

# 1.3.1 (versionCode 5)

- 原创“两层聊天气泡 + 镂空 Nova 星核”图标，替换临时 Z/S 图形；统一桌面、安装页、启动页和关于页。
- 提供彩色 adaptive icon、Android 13+ 单色主题图标、圆形图标与矢量后备资源；附 SVG 源图和预览。
- 使用 AndroidX SplashScreen 标准启动页，不引入独立启动 Activity 或额外等待。
- 启动页退出后恢复浅色界面的深色状态栏图标；Android 8.0 使用可读的深色导航栏。
- 登录默认留在 Nova；新增内部官网登录入口，移除“外部浏览器登录”兼容引导。Google OAuth 明确标为当前不支持，Microsoft / Apple 仅继续官网允许的容器内流程，不声称已兼容。
- HTTPS intent 链接也在 Nova 内处理，避免其指定浏览器包名导致登录会话离开 Nova。
- 保留 `com.example.chatgptnova` 和原 release 签名，可覆盖之前的 Release 版本；CI 检查原 v1 升级、八项 WebView 集成测试及原生界面。

官网入口可见不代表账号认证已完成；真实账号登录和长期会话保留仍需要真实设备验证。WebView Cookie 不能从系统浏览器安全迁回。

# 1.3.0 (versionCode 4)

- 保留 com.example.chatgptnova、原 release 签名与 WebView 数据，支持覆盖 v1。
- 上传明确处理多选、逗号分隔 MIME 与文件扩展名；成功拍摄的文件不会被下一次选择删除。
- HTTPS 与 blob 下载使用系统保存对话框；HTTPS 流式传输仅向最初 ChatGPT 同源地址发送 Cookie，跨源后全程不发送 Cookie，拒绝 HTTP 降级。
- blob 下载尽量保留用户点击链接的文件名，无法取得时使用可读默认名，避免随机 ID。
- 增加主机名显示、原生网络错误重试、权限顶层来源检查、清除数据期间导航保护与 256 KiB 状态限制。
- Microsoft / Apple 允许官网流程在 Nova 内继续；Google 提供浏览器帮助，明确浏览器会话不能迁移进 Nova。
- CI 直接构建标准 Gradle 工程，并新增 Android 13/14/15 的 v1 覆盖安装和受控 WebView 集成检查。

没有整体重写。真实账号及物理相机/麦克风仍未代替用户手机完成实测。
