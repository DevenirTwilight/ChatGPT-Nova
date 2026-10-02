# 1.3.1 (versionCode 5)

- 原创“两层聊天气泡 + 镂空 Nova 星核”图标，替换临时 Z/S 图形；统一桌面、安装页、启动页和关于页。
- 提供彩色 adaptive icon、Android 13+ 单色主题图标、圆形图标与矢量后备资源；附 SVG 源图和预览。
- 使用 AndroidX SplashScreen 标准启动页，不引入独立启动 Activity 或额外等待。
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
