# 1.3.0 (versionCode 4)

- 保留 com.example.chatgptnova、原 release 签名与 WebView 数据，支持覆盖 v1。
- 上传明确处理多选、逗号分隔 MIME 与文件扩展名；成功拍摄的文件不会被下一次选择删除。
- HTTPS 与 blob 下载使用系统保存对话框；HTTPS 流式传输仅向最初 ChatGPT 同源地址发送 Cookie，跨源后全程不发送 Cookie，拒绝 HTTP 降级。
- 增加主机名显示、原生网络错误重试、权限顶层来源检查、清除数据期间导航保护与 256 KiB 状态限制。
- Microsoft / Apple 允许官网流程在 Nova 内继续；Google 提供浏览器帮助，明确浏览器会话不能迁移进 Nova。
- CI 直接构建标准 Gradle 工程，并新增 Android 13/14/15 的 v1 覆盖安装和受控 WebView 集成检查。

没有整体重写。真实账号及物理相机/麦克风仍未代替用户手机完成实测。
