# 项目长会话覆盖核对（验证工具，不是导出功能）

当前状态更新：1.3.9 DOM试用版已停用旧capture；下方基线/ADB步骤为前一阶段验证方法。最新任务为源码审查及 [滚动缓存完整性设计](../../../docs/export/history-integrity.md)，尚未接入自动滚动。新增 `export-snapshot.test.cjs` 使用当前生产脚本复现稳定7条缺33条、虚拟窗口并集和ID/正文区别；只合成验证，不解析用户HTML。运行：`node tools/feasibility/history-coverage/export-snapshot.test.cjs`。

旧 `dom-probe.js` 只输出首尾四条 samples，不能逐条核对中间历史。本目录独立探针返回全部已检查消息的 ID/角色，最多 2000 条；超限明确失败。不读取正文、Cookie、storage、fetch 或 React，不自动滚动。记录包含私有会话路径和消息 ID，仅本地保存，勿提交原始记录到公开仓库。

## 先建立真实测试条件

需要已登录目标项目会话的 Nova、可用电脑和设备连接。手机“最新版”不能替代 APK 提交号；记录安装来源、版本/code、Android 和 WebView 版本。若提交不明，写未知。

当前 feature 自动安装旧 capture，不能用于隔离新 DOM 路线的结论。推荐指定 `200b7898…` 隔离工作树，按上级 README 使用最小补丁；不要覆盖正式应用或搬运 Cookie。debug 包需重新登录。生产源码在本轮没有改动。

电脑本地装好 Android platform-tools 后运行 `adb devices -l`。需手机启用开发者选项/USB 调试、连接 USB 并确认该电脑授权；在本地操作，无需把 ADB 暴露到公网。设备序列号可打码。这里只是连接检查，不代表目标 WebView 可调试。

若隔离 debug WebView 不显示，可仅在独立验证工作树的 Activity 创建聊天 WebView 之前临时加入下面的公开调试 API。不要应用到生产分支。本轮未构建或运行这一接入。

```java
if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
    android.webkit.WebView.setWebContentsDebuggingEnabled(true);
}
```

用隔离 debug 构建连接电脑 Chrome 的 `chrome://inspect/#devices`，选择 **Nova debug 包的目标聊天 WebView**。若看不到页面，先检查 debug 构建是否启用了 WebView 调试；旧接入补丁未保证这一点。本轮未提供已构建探针 APK，也未验证设备连接，不能直接把这些步骤说成已可执行成功。不要在正式包启用调试。本地电脑浏览器采样可辅助分析，必须单列，不能冒充 Android WebView 证据。

## 真实采样与独立基准

1. 停止生成，选择目标项目长会话的明确分支。记录设备、APK、账号已登录（不记录凭据）、分支操作和已知首尾消息。
2. 在目标页面 DevTools Console 执行 `probe.js` 的完整表达式，保存返回的 JSON 字符串为本地记录。Chrome Console 的 `copy(<完整表达式>)` 可复制 JSON。勿只复制 Logcat：长 JSON 可能截断。
3. 底部、滚到最顶部等待历史加载、再回到底部分别采样，至少再重复一轮。记录操作时间和人工看到的加载/分支状态；探针不会替你滚动。把各 JSON 对象放入一个数组，保存 `snapshots.json`。
4. **独立**建立目标分支的有序消息 ID/角色基准，保存 `reference.json`。可用用户提供的官方导出（先确认含该项目会话、ID 能对应当前 DOM、选定分支与时间相符），或另一个能逐条核实的来源。不要从这些 DOM 快照的并集反推基准。无法取得独立基准时，只报告观察，完整历史仍未证明。
5. 新基准格式如下（示例是假数据，不是真实结果）：

```json
{
  "route": "/g/g-p-fixture/c/fixture",
  "source": "说明独立基准的来源及获取时间；工具无法验证此声明",
  "scope": "说明目标分支、编辑/重新生成选择、历史范围",
  "rows": [
    {"id": "fixture-0", "role": "user"},
    {"id": "fixture-1", "role": "assistant"}
  ]
}
```

```bash
node tools/feasibility/history-coverage/audit.cjs /local/reference.json /local/snapshots.json
```

退出码 0 仅指至少一个合格快照的 **ID/角色/顺序**匹配所提供基准；2 表示未展示单次匹配，1 是输入错误。所有结果仍标 `historyCompleteness=not-proven`、`bodyFidelity=not-tested`：工具无法验证独立来源是否完整，也没有核对正文或附件。

`union-only-match` 表示多次快照并集才覆盖基准，不能批准单次 DOM 导出。稳定但缺中间消息、错误路由、缺失/重复 ID、超限、嵌套 author、角色不符和生成中均不能匹配通过。发现真实已知历史缺失，应拒绝单次 DOM 路线；滚动并集能否可靠采全另行验证，不能据此修复旧403。

路径前后相同不能排除 SPA A→B→A，也不证明请求回调或正文未变。ID 覆盖匹配后仍需逐条正文、富文本与目标 Android 保存验证；不要提前扩展格式实现。

## 本地合成自检

```bash
node tools/feasibility/history-coverage/audit.test.cjs
node tools/feasibility/history-coverage/probe.test.cjs
```

浏览器检查依赖 `playwright-core` 与 `/usr/bin/chromium`，可用上级 README 的临时依赖安装和 `NOVA_PLAYWRIGHT_MODULE`。测试只验证工具对夹具的判断，不证明真实会话覆盖。
