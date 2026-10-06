# GPT Nova 导出可行性验证

本目录只包含探针、接入补丁、检查和报告，不是完整导出功能。

阅读 [REPORT.md](REPORT.md) 获取证据、风险排序和必须换路线的条件。

## 真机验证：推荐使用产品基线

目标基线：`200b7898b51cdac9e342e49e7e7a7059067d944f`，即 `fix/native-share-1.3.7`。
选择这个基线是为了隔离旧导出脚本的 fetch 包装、内部读取器与 React 读取对结果的影响。
保留已验证的输入、网页分享、原生分享、登录和下载实现。

在仓库创建独立工作树，再应用本目录的补丁：

```bash
git worktree add --detach ../nova-feasibility 200b7898b51cdac9e342e49e7e7a7059067d944f
git -C ../nova-feasibility apply --check /absolute/path/integration-baseline.patch
git -C ../nova-feasibility apply /absolute/path/integration-baseline.patch
cd ../nova-feasibility
./gradlew :app:assembleDebug
```

路径请替换为你实际下载的补丁路径。本轮已在干净基线执行 `git apply --check` 并通过；未运行完整 APK 构建。
Debug APK 的 applicationId 为 `com.example.chatgptnova.debug`，与正式应用登录数据独立，需要单独登录。
不要把 Debug 包的未登录状态解释为正式应用登录丢失。

补丁只添加一个探针类和一个 JS 资源，并在现有 MainActivity 增加调试菜单、导航代次和释放/结果转发。
菜单只在 `FLAG_DEBUGGABLE` 应用中显示，避免依赖 AGP 是否生成 BuildConfig。
不改已有 WebView 配置，不替换 Client，不改现有菜单项、输入类或分享类。

日志：

```bash
adb logcat -s NovaFeasibility
```

三个调试菜单：

1. **验证：DOM 快照**：UI 线程执行一次性只读脚本，返回角色、数量、消息 ID、结构计数和内容指纹；8 秒回调超时；导航或 WebView 更换后丢弃旧回调。不打印正文，不访问 Cookie、storage、fetch、React 属性。
2. **验证：系统 PDF**：独立 Activity WebView 打印固定 100 段测试 HTML。它保持附着和可渲染，禁用 JS/网络，在打印适配器 onFinish 或 Activity 销毁时释放；不因 onPause 释放。15 秒超时仅覆盖文档加载/视觉就绪，用户打印交互不设超时。
3. **验证：SAF 保存**：创建一个测试文本文件，后台写入固定 UTF-8 测试内容并回读比较；查看 `SAF roundTrip=true`。取消只消费本探针结果。独立 requestCode `0x6301` 不复用下载 `1005`、上传 `1001` 或分享 `0x7000..0x7fff`。

PDF 探针调用 onFinish 只表示适配器生命周期结束，不能据此宣称保存成功。必须实际打开所保存的 PDF。
SAF 验证会真实创建测试文件，请选择新文件位置。

## 不使用补丁时的最小手动接入

将 `ExportFeasibilityProbe.java` 复制到 `app/src/main/java/com/example/chatgptnova/`。
将 `dom-probe.js` 复制到 `app/src/main/assets/feasibility/`。

在现有菜单 UI 回调中调用：

```java
WebView page = webView;
ExportFeasibilityProbe.snapshot(this, page, () -> !clearing && webView == page);
```

手动接入也应像补丁一样将导航代次加入 current 判断，以拒绝导航回来后的迟到回调。
PDF 会话保存为 Activity 字段，避免重复打开打印；在现有 onDestroy 中调用 close，不加 onPause/onResume 的释放逻辑。
SAF 结果在现有 onActivityResult 中单独转发；具体代码以补丁为准。

## 真机执行顺序与通过标准

1. 短会话：首尾 ID、角色、数量与页面一致，`callbackMain=true`，`bounded=false`。
2. 长会话：底部运行、滚动顶部等待加载后运行、回到底部运行。记录数量和首尾 ID。数量增加证明原先缺历史；等量但 ID 替换提示虚拟化。数量相同不能证明完整。
3. 重新生成分支、编辑同 ID 正文、切换会话：内容签名对应变化，旧回调不交付。SPA 不一定触发 onPageStarted，因此仍依赖点击时 URL 和 DOM 内容检查；生产快照未来应在转换结束再次比对。
4. 流式回复：探针允许观察变化；未来导出应在回复结束后执行。stop-button 仅是提示，不能单独证明无流式更新。
5. 富文本：代码、表格、公式、引用、折叠文本、附件、图片分别检查；探针只确认结构存在，不证明完整格式还原。textContent 包含控件或隐藏文本，不能直接作为最终正文。
6. PDF：完整保存、取消再试、更改纸张、返回聊天；实际文件包含 PDF-FIRST、第 100 段、CODE-LAST、table-last、PDF-LAST，多页可读；链接另外检查。
7. SAF：创建、取消、重新保存、不同文档提供者；roundTrip=true；原有下载/上传回调不受影响。
8. 集成回归：正常输入、长文本粘贴、网页分享、原生分享、附件上传、下载、语音、登录持久化和退出清理。

必须提供真实登录会话和目标设备，才能完成步骤 1–8。此云环境没有账号、连接设备、adb 或 KVM；本轮没有执行这些步骤。

## 本地复验 DOM 探针

```bash
npm install --prefix /tmp/nova-feasibility-deps playwright-core@1.57.0 --no-audit --no-fund
NOVA_PLAYWRIGHT_MODULE=/tmp/nova-feasibility-deps/node_modules/playwright-core node tools/feasibility/dom-probe-check.cjs
```

检查使用本地 Chromium、合成 HTTPS 页面，所有网络请求均由夹具处理，不使用真实 ChatGPT 账号。
输出保存在 `dom-probe-results.json`。它模拟真实风险，不断言 ChatGPT 当前就采用同样的 DOM 虚拟化实现。

独立 Android 探针类可以用 JDK 编译核查 Android API 符号：

```bash
javac -source 17 -target 17 -cp "$ANDROID_HOME/platforms/android-35/android.jar" -d /tmp/nova-probe-classes tools/feasibility/ExportFeasibilityProbe.java
```

本轮对 API26/API35 android.jar 均编译通过。这不是 Gradle APK 构建、Android 运行或 minSdk 全量兼容测试。
