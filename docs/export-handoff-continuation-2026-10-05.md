# 交接续接与导出验证（2026-10-05）

实现提交：`4cd399afbb73366833a24808161b85f1bc6680d4`，分支 `feature/export-conversation`。续接起点为远端 `5ddae53`；产品基线仍是 `200b7898b51cdac9e342e49e7e7a7059067d944f`。

## 修改与验证范围

当前公开网页的完整读取器仍匹配既有模块正则和三个行为标记。网页自身从项目地址传入 `projectId`，并从 `owner_user_id` 传入共享会话拥有者；Nova 之前省略了这两个参数。本轮按网页自身的路由解析补齐，并让网页构建鉴权请求。原截图不足以证明缺失参数就是该设备失败的原因，真实已登录账号尚待实测。

读取器异常不再跳过后续完整接口读取；模块导入、未响应取消的读取器、接口及响应正文都有超时边界。每次导出清除旧快照，仅接受本次读取期间显式 full 请求的观察结果；晚到回调不会缓存过期树。末端须是没有正向/反向子节点的真实叶节点，映射必须有自己的节点条目。普通末条正文要求完整匹配，读取期间文字变化即取消。保留 HTML/Markdown/PDF 和 Android 保存/分享；未确认完整消息树时拒绝导出。

`ComposerWebView.java`、`WebShareAdapter.java` 与 `200b7898` 逐字节一致。没有引入 `NovaWebView`、新增 Android 权限或导出器 Cookie/Token/Storage/Header 读取。网页自己的 Web Share 保留。

## 本地

- 33/33 核心测试通过，包括 400 条消息、再生成分支、缺失祖先、非叶末端、反向父子不一致、继承映射拒绝。
- 捕获浏览器测试通过：项目/共享拥有者参数、读取器异常后的独立备用读取、旧缓存拒绝、忽略取消的读取器/模块超时、错误会话拒绝、安全 E2 诊断。
- 格式与浏览器回归通过：普通末条过期前缀拒绝、代码语言工具栏净化、同 ID 文字变化拒绝、LaTeX、320/390/844/1280px、深浅色、HTML/Markdown/15 页桌面 PDF。
- Debug 与 instrumentation APK 构建通过；lint 0 errors / 20 warnings。包内导出脚本与提交源码一致。
- 本环境无 KVM，没有运行本地 Android 模拟器。浏览器禁止 file://；单文件离线样例通过受控地址提供相同字节，并阻止其它网络请求。

## 本轮 Android CI

导出与基线：[37284837307](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37284837307)。原签名与兼容：[37284839087](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37284839087)。两轮构建 job 通过，但整体 CI 因 API33 的已有输入性能项失败而非全绿。

| API | 导出 | 输入/分享基线 | 导出工作流合计 | 原签名升级/WebView | 原签名界面/重启 |
| --- | --- | --- | --- | --- | --- |
| 33 | 7/7 | 21/22 | 28/29 | 24/25 | 2/2 |
| 34 | 7/7 | 22/22 | 29/29 | 25/25 | 2/2 |
| 35 | 7/7 | 22/22 | 29/29 | 25/25 | 2/2 |

API33 两轮失败均为 `keyboardLongCommitUsesPageTransactionAndUndo` 的既有性能门槛：debug transaction 3910.0 ms / editor baseline 2873.7 ms；正式签名 transaction 3334.8 ms / baseline 1175.9 ms。没有修改输入实现、放宽门槛或重跑以掩盖失败。

API34 正式签名的 WebShare 套件因观察到 GMS FontsProvider 死亡，按仓库既有恢复逻辑自动完整重试一次后通过；没有跳过断言或清空 Nova 数据。三档导出与基线测试均无跳过。原签名三档均通过 3/3 v1 数据播种/持久化/升级测试和 2/2 界面/重启测试。

## 实际 PDF 与安装包

本轮 Android33/34/35 系统保存出的 PDF 已取回，均为 5 页 A4，包含全部 80 段、长代码末尾、表格、公式源文、最终回复和 81 个真实 URL 注解。这是 2 条长合成消息的系统 PDF，不是真实账号会话。Android33 producer 为 Skia/PDF m109，Android34 为 m113，Android35 为 m124。

原签名 APK：`com.example.chatgptnova`，1.3.7 / versionCode 11，非 debuggable，1862243 bytes。

- [下载签名 APK 包](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37284839087/artifacts/11334285839)（解压后安装 ChatGPT-Nova.apk）
- [下载完整源码](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37284839087/artifacts/11333103375)
- [下载 debug APK / 阅读样例](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37284837307/artifacts/11333941984)

证书 SHA256：`f93221ee0d2be2b806233a0b3427ec9c14766100c1bab3208841e6203e2b4289`，与原发布证书一致。APK SHA256：`86a543760ace25c81f58af16177e69e74629c6ef78c9c4517538b5a08c5749ea`。导出脚本与实现提交逐字节一致；无需为安装此正式签名更新包清除原应用数据。源码 ZIP 的 83 个条目已与实现提交核对，不包含签名材料。

## 真实设备待验证

用户旧 APK 没有“复制诊断”按钮。本轮包包含该按钮。安装后应优先测试原先失败的项目会话，确认起始/最后消息与 HTML、Markdown、PDF 保存/分享；若仍失败，返回复制出的 E2 一行。

公开模块导入、合成浏览器、Android 模拟器通过不等于真实账号读取成功。真实登录、真实附件/分享目标和真机导出仍待验证。带引用末条继续按有序正文片段核对；React 原始 props 备用来源仍受其既有快照新鲜度限制，不据此声称服务器实时状态已证明。
