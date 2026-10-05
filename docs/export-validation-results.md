# 导出功能验证结果（2026-10-04—05 UTC）

产品基线：`200b7898b51cdac9e342e49e7e7a7059067d944f`。验证分支：`feature/export-conversation`。本记录区分合成受控页面、模拟器与真实账号/真机。

## 本地结果

| 验证 | 实际结果 |
| --- | --- |
| Node 完整链/分支/格式测试 | 25/25 通过 |
| JS 语法检查 | core.js、capture.js、run.js 通过 |
| 浏览器提取/净化/响应作用域 | Chromium 通过 |
| 手机/横屏/桌面与深浅色 | 320、390、844、1280px 无整页横向溢出；深浅色通过；320px 下显示 1500 字符 URL 仍无整页溢出 |
| 离线文档 | 单文件 HTML/Markdown 生成；CI 真实 file:// 打开通过；本地云浏览器通过受控无网络地址读取相同文件 |
| 桌面 PDF | 15 页 A4；pdfinfo、pdftotext 可读；有真实 URL 链接对象 |
| Debug APK 与测试 APK | 构建通过 |
| Lint | 0 errors，20 warnings；export 专属代码无警告 |
| Release | 临时 init script 生成 unsigned APK；未修改发布签名配置；无原 keystore，正常签名发布未完成 |
| 基线文件核对 | ComposerWebView.java、WebShareAdapter.java SHA256 与 200b7898 一致 |

## Android CI

最终 checkpoint：`62979e7`，[CI 37249304995](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37249304995)。三档导出 6/6 全部通过。最终结果如下（不是全绿）：

| API | 导出 | 基线回归 | 合计 | 失败 |
| --- | --- | --- | --- | --- |
| 33 | 6/6 | 21/22 | 27/28 | keyboardLongCommitUsesPageTransactionAndUndo 性能阈值：2234.5 ms / baseline 1641.0 ms |
| 34 | 6/6 | 22/22 | 28/28 | 无 |
| 35 | 6/6 | 22/22 | 28/28 | 无 |

构建 job 通过。API33 的门槛没有放宽；不声称所有 CI 通过。API33/34 系统保存出的 PDF 已从这轮 artifacts 取回，API35 实物来自相同功能/测试源码的上一轮 142264c（62979e7 只增加 CI 锁屏设置）。套件共 28 项：ConversationExportTest 6、WebShareTest 5、NovaWebViewTest 9、ClipboardUiTest 5、ClipboardProbeTest 3。名称为 NovaWebViewTest 的测试在产品基线中已存在，不意味着采用 PR #3 的 NovaWebView。

已完成的上一轮（`3a97685`，[CI 37245479131](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37245479131)）：

| API | 实际结果 | 失败 |
| --- | --- | --- |
| 33 | 25/28 通过 | PDF 最终保存；keyboardLongCommitUsesPageTransactionAndUndo；systemLongPasteUsesPageTransactionAndUndo |
| 34 | 27/28 通过 | PDF 最终保存 |
| 35 | 27/28 通过 | PDF 最终保存 |

三档均通过 HTML/Markdown 保存、取消/重复导出、分享 URI、文件名及完整性拒绝测试；PDF 预览/取消通过，但最终保存显示系统生成错误，不能算 PDF 通过。API34/35 的 22 项基线回归通过。API33 两项性能失败分别为 transaction 3076.2 ms / baseline 2214.3 ms，3313.6 ms / baseline 2606.6 ms。

之后 `cf9e9f1` 的 API33/34 测试全部停在 `focused Nova window`，未执行到导出正文；新增多行条件逻辑暴露了 emulator action 每行独立 shell 的问题，诊断收集未执行。当前已改为完整 Bash 文件并取消测试前 adb root；将这轮记录为环境阻塞，不算导出功能通过。

`393b0bb` 的辅助打印诊断出现阻塞，已在 `142264c` 删除。AOSP Android 14 UiAutomationConnection 实际采用 Runtime.exec，不能假定它会处理 shell 引号；当前测试通过无空格的 sh -c 参数明确执行已编码的固定读取脚本，中文/空格文件名在本地用相同 argv 语义验证通过。该逻辑只在测试 APK 中读取当前合成会话保存的文件，不在正式 APK 中执行。

保留原有 IME 性能阈值，不以放宽阈值换取 CI 通过。过往失败包括 CI SDK tools 下载、Gradle HTTP 500、屏幕锁定、API33 不支持 String.formatted 的测试辅助代码、系统 PDF 预览等待。对应环境/辅助代码已逐项修复或明确记录，不能把这些中间失败当成通过。

`142264c` 的 Android33 导出 6/6 通过、基线回归 21/22 通过；唯一失败是 keyboardLongCommitUsesPageTransactionAndUndo，transaction 3591.9 ms / baseline 2488.7 ms。系统保存的实际 PDF 已取回并验证（Skia/PDF m109、5 页 A4、95649 bytes、81 个链接注解），与 API35 一样包含完整 80 段正文、长代码、表格、公式源文和最终回复。

## 已取回的 Android PDF 实物

`142264c` 的 Android35 CI 导出 6/6、基线回归 22/22 全部通过。artifact `export-android-35` 包含系统保存到 Download 的实际 PDF（不是打印预览复制件）：5 页 A4、103459 bytes、Skia/PDF m124。Android PdfRenderer 打开/多页断言通过。本地 pdfinfo/pdftotext 验证全部 80 段正文、LONG_LINE_END、表格 export-table-row、公式原文及最后 reply；pdfinfo -url 识别 81 个 Annotation 链接，包括 https://example.org/android-pdf-target。实际页图已检查，代码换行与表格可读、边距正常。

本样例有 2 条合成消息（长用户消息＋assistant 回复），与桌面 80 条消息样例不同。它证明生产系统 PDF 保存路径在该 Android35/WebView 环境工作，不证明真实账号提取或所有打印提供者兼容。

`62979e7` 的 Android34 实际 PDF 已取回：Skia/PDF m113，5 页 A4，全部 80 段及富文本末端标记存在，81 个链接注解。导出 6/6、回归 22/22 通过。

## 验证边界

真实已登录 ChatGPT 会话提取、真机 HTML/Markdown/PDF 保存与第三方应用交付、真机 IME 长文本、真实附件/登录、APK 覆盖安装均**未验证**。原用户真机已验证的 IME 基线保持不变，但源码不变不等同于本轮真机回归已通过。

样例是合成当前分支，包含中文/英文/富文本/长代码/表格/链接/图片/数学；不是来自账号的真实聊天。桌面 PDF 不证明 Android PDF 已保存成功。
