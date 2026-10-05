# 真机 E2 403 的分页兼容读取（2026-10-05）

用户安装上一轮原签名测试包后返回：`模块=1，导入=1，读取器=1，状态=failed-403，接口=403`。这说明模块和完整读取器已经找到，但网页的完整读取器与独立完整接口均被拒绝。上一轮修复和 CI 记录见 [交接续接验证](export-handoff-continuation-2026-10-05.md)。

## 修改

完整读取失败后，先调用网页自身的普通网络读取流程。若它返回分页快照，则通过同一公开模块直接依赖中的分页读取助手继续读取更早消息；若普通读取也失败，允许使用这两个助手直接读取当前会话。没有另行构造鉴权、读取 Cookie/Token/Storage/现有 Header/PIN，或调用会话以外的接口。

本轮公开网页源证据：`conversation-small-ig1392ugx3eo129o.js` 的完整流程调用单数 `/conversation/{conversation_id}`，普通流程可以调用分页初始助手。其已加载直接依赖 `4813494d-okcxqld7kfttimw4.js` 的两个只读助手调用复数 `/conversations/{conversation_id}` 和 `/conversations/{conversation_id}/messages`。使用实际公开模块、Function.toString 和 Chromium ResourceTiming 独立确认了唯一函数和依赖。产品按行为标记及静态导入绑定发现这些助手，不硬编码本轮 CDN 哈希或缩短的导出名。

读取仅观察本次请求期间、同源、同会话精确路径的 GET 响应正文。起始页和每个更早页必须提供原始 `page_info.has_previous_page` 布尔值；为 true 时必须提供非空服务器游标。缺失标记不能被助手归一化后的 null 游标当成完整证明。

分页最多 400 页 / 累计 16 MiB，必须游标前进、消息 ID 与正文不冲突、重叠边界连续、服务器末端一致；明确不完整的响应立即拒绝。遍历到明确耗尽后，重新读取并比较完整起始响应（正文、分支、游标、更新时间、标题和元数据）。正常加载器的初始读取与复核使用相同窗口。页面地址和 DOM 消息/文字变化仍取消导出。响应无法唯一归属请求、超时或任何校验失败都拒绝不完整文件。

公开分页 API 本身没有原始树父节点；耗尽后构造的是当前已排序分支的导出链，证明明确标记为 `cursor-pagination`、页数与 `exhausted:true`，不将其当成服务器原始父子树。未经完整分页验证的网页 `__paginatedConversationPage` 快照明确拒绝。HTML、Markdown、PDF 保存/分享继续使用现有链路。

Native 加载新验证器，并将读取预算调整为 120 秒；完整读取 15 秒、分页阶段 75 秒、独立接口 10 秒分别有超时边界。新增 E2 页数和阶段以及固定公开错误类别，不显示会话地址、游标、任意服务器错误内容或账号信息。

## 验证与交付

本地核心 33/33、分页纯逻辑 52/52、已有捕获与渲染浏览器测试和新分页浏览器 10/10 场景通过。Debug、instrumentation APK 编译和 lintDebug 通过（0 errors / 20 warnings）。本提交另包含 Android WebView 生产资源读取的两页 400 消息测试和缺失耗尽标记拒绝测试。提交后由同分支 [导出验证工作流](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/workflows/export-validation.yml) 执行 Android 33/34/35 验证。

通过 [签名构建工作流](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/workflows/build-from-zip.yml) 的显式测试发布输入，原发布证书 APK 和校验文件发布为 `nova-export-test-<本提交前七位>` 的 GitHub Prerelease，提供无 Actions 登录要求的固定 APK 下载入口。生产 main 的发布流程不变。

`ComposerWebView.java` 和 `WebShareAdapter.java` 保持已验证 `200b7898` 基线。本环境无 KVM；不声称本地已运行 Android 模拟器。上一轮 API33 已有键盘性能门槛失败，保留原断言与输入实现。新一轮结果以对应工作流和后续验证记录为准。

真实账号的复数分页接口仍可能返回 403。本轮公开源验证与合成测试不能证明用户账号具备读取权限；该测试 APK 仍需在原先失败的会话验证。完整起始响应元数据变化、缺失加载依赖、并发同游标响应或更早页无明确完整性信息均会保守拒绝。

## 两页读取后的复核修复

用户连续报告 `分页=failed-error，页数=2，阶段=recheck`。单数完整接口的 403 与分页复核错误分别记录，不能将这条诊断解释为复数分页接口也返回 403。

复现确认：正常首屏请求省略 `include_message_id` 时，旧复核代码会补上 DOM 末条消息 ID，导致请求另一个窗口。现在从本次成功首屏请求 URL 仅记录 `num_turns` 和 `include_message_id` 的实际值以及是否存在；更早页沿用同一 turns 参数，复核沿用两项参数。删除 DOM 推断和归一化 marker 推断；参数缺席与空值保持区别，重复或不支持的参数拒绝读取。仍由网页的公开助手负责鉴权，不读取现有 Header、Cookie、Token 或账号 Store。

复核诊断细分为 `recheck-request`、`recheck-response` 和 `recheck-verify`。比对失败仅输出固定分类 `head-messages-changed`、`head-branch-changed` 或 `head-metadata-changed`，不会输出消息、URL、游标或账号信息。正文、更新时间及未知元数据继续完整比对；`page_info` 仅排除可轮换的 `start_cursor`，其余字段仍须一致。

本地核心 33/33、分页 54/54、分页捕获 17/17、既有完整捕获和浏览器渲染回归通过。新增场景覆盖消息参数缺席、空值、marker 与请求 turns 不同、两个参数均缺席、重复参数拒绝，以及正文/元数据变化的固定诊断。Android 的两页 400 条生产路径夹具会在复核擅自增加消息参数或更早页改变 turns 时返回错误。

针对上一轮 API34/35 所有导出测试在获得窗口焦点前超时，测试夹具改为每次启动前唤醒设备、等待可见窗口后解除模拟器锁屏并记录回调。最终 `hasWindowFocus && web.isShown` 断言保留，超时报告包含实际窗口归属、锁屏与电源状态。Android CI 同时保存窗口/电源诊断。此处只修改测试准备；实际根因是否被覆盖须由本轮 Android CI 证明。
