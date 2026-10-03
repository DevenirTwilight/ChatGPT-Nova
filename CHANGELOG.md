# 1.3.7 (versionCode 11)

- 新增“分享当前页面”原生菜单入口：仅在可信的 `chatgpt.com` 页面可用，直接使用 Android Sharesheet 的 `ACTION_SEND` 分享当前 HTTPS 页面链接。
- 为缺少 `navigator.share` 的 WebView 提供网页 Share 兼容层：使用加载前注册、限定 `https://chatgpt.com` 的 WebMessageListener；不暴露 Java 对象，不覆盖已有的原生实现。
- 修正旧 adapter 在页面加载完成后才添加 JavaScript 接口、导致当前页面接口为 undefined 的时序问题；支持时在 document start 安装兼容函数，保留 SPA 和重载后的能力。
- 分享 Promise 等待系统目标选择回调；取消、非法数据和来源限制会返回错误。未提供 URL 时不追加当前私人会话地址，非法 URL 不替换成当前页面。
- 原生侧再次校验当前页面与分享 URL；不读取 Cookie、账号、密码或 Storage，不调用 ChatGPT 私有 API，不伪造 `/share/` URL。
- 原样保留网页提供的 `/share/...`、`/s/t_...` 等可信分享地址，不把 URL 路径形式作为成功或失败的唯一判断。
- 粘贴先交给网页编辑器处理：系统粘贴在冒泡阶段才检查是否需要兼容回退，Nova 菜单提供纯文本粘贴事件；网页已处理时不再执行原生 HTML 插入，避免绕过编辑器模型、重建长文本和破坏编辑器撤销历史。
- 选区追踪不读取输入框布局尺寸；仅实际需要粘贴回退时检查可见性，减少长文本更新后的同步重排。
- 合并同一帧的选区变化，只读取一次 Range，避免旧 Chromium 在未缓存选区时重复计算可见选区与布局。
- 增加网页自有粘贴事务的长文本、选区替换、一次撤销和绘制耗时检查，并记录各原生剪贴板诊断路径的耗时；受控测试不代替真实 ChatGPT 编辑器的实机性能验收。
- Nova 菜单“分享当前页面”继续作为独立辅助入口；增加网页 Web Share 到真实系统 Sharesheet 的受控测试。真实 ChatGPT 会话的网页 Share 仍需实机验收，测试夹具通过不视为已修好。
- 修正原生粘贴脚本中的 Java 字符串换行转义，以及版本、菜单和关于说明的过时测试断言；版本仍为 1.3.7 / 11。
- 保留已有 SPA / 选区兼容，恢复 contenteditable 的一次性转义纯文本片段插入，避免多行 insertText 引入额外换行及长文本编辑卡顿。分享测试兼容 Android 35 的新版 chooser 回调字段。
- 不为系统 Sharesheet 增加不必要的 `<queries>` 包可见性声明：Nova 不需要预先枚举分享目标，直接调用系统 chooser 并处理无可用目标的异常。

# 1.3.6 (versionCode 10)

- 继续 1.3.5 的系统粘贴修复，解决旧 WebView 在大量换行的编辑命令中卡住：textarea 使用原生值 setter、保留选区并通知 input；contenteditable 使用一次性纯文本转义片段，保留浏览器编辑撤销路径。
- 系统长按/IME 粘贴和原生菜单使用同一插入逻辑，短单行文本继续使用网页默认粘贴。不读取密码、不保存剪贴板、不自动发送。
- 登录帮助默认解释邮箱与独立密码的应用内登录，浏览器仅作为 Google 登录的可选替代；已完成的主界面、菜单及正式图标保持。
- 修正原生重启夹具在返回页尚未就绪时写入 storage 的竞争；继续 Android 13/14/15 的完整测试与签名连续性检查。

# 1.3.5 (versionCode 9)

- 在 ChatGPT 聊天框捕获较长文本的真实系统粘贴事件，以纯文本编辑命令兼容网页编辑器；短文本保留原有粘贴行为。
- 保留系统长按与键盘粘贴；增加 ChatGPT 页面中的“从剪贴板粘贴”原生兜底。先点聊天输入框，再打开菜单粘贴。
- 仅用户主动操作时读取纯文本，以聊天输入框的纯文本编辑命令插入，JSON 安全转义，不持久保存、不解释为 HTML、不自动发送；不向登录字段或外部页面粘贴。
- 在签名 Release 的真实 WebView 中检查长按、IME 粘贴、IME 输入和原生菜单入口；覆盖 1/10/50 KB、中文英文、Markdown、换行与 emoji。受控页面结果不代表已验证真实账号登录后的 ChatGPT 网页。

# 1.3.4 (versionCode 8)

- 顶部只显示 ChatGPT Nova 和当前域名；非官方性质与独立登录说明移至“关于 ChatGPT Nova”。
- 右上角改为轻量弹出菜单：刷新、ChatGPT 首页、用浏览器打开、设置；页面明确显示未登录时才增加“登录”。
- 设置集中放置退出当前账号（页面明确显示已登录时）、清除登录与网站数据、登录帮助和关于。退出与清除均需确认，只作用于 Nova 自己的数据。
- 登录状态仅依据 ChatGPT 页面可见的公开登录/账号控件判断；不读取密码、Cookie、Storage 或私有认证接口。无法确定状态时不假装已登录，仍可从设置打开登录帮助或清除数据。
- 保留 Google 浏览器登录方案、现有图标、上传、下载、权限和 Cookie 持久化；保持 applicationId 与原 release 签名。新增登录/未登录/未知状态、SPA 状态变化及退出取消的真实 WebView 夹具检查。
- CI 固定 Emulator 37.1.11（build 15917651），使用当前支持的 SwiftShader 模式，关闭 Vulkan 与快照并明确内存配置；网页检查后重启模拟器再进行原生检查，不重建或清空其应用数据。修正界面抓取进程异常时未进入已有有限重试的问题，保留 Android 13 / 14 / 15 的完整断言与失败诊断。
- 自动化原生检查改为测试 APK 提供的受控页面，使用持续的系统无障碍连接操作实际 Release 界面；保留标题、菜单、关于、登录入口、刷新、清除、旋转、返回与强制停止后的数据保留断言。公开登录页与真实账号认证不算自动化检查通过项目；保留手动设备检查脚本。

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
