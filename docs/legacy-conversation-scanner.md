# Legacy Conversation Scanner（Experimental）

## Step 1：真实调用链审查（2026-10-07）

远端基线：`da4176c5731ee07ea3c8815d56169abf1b143b25`，分支 `feature/export-conversation`；不是 main。

- 正式 popup 菜单只调用 PageSnapshotExport.start/showDiagnostic，没有 scanner 入口。
- configureWebView 无条件 new ConversationExport，读取包/WebView版本并初始化诊断。
- onPageStarted → navigationStarted；onPageFinished → pageFinished（空）；onPause/onResume → activityPaused/activityResumed（空）；onActivityResult → legacy SAF 路由；renderer gone/清除数据/onDestroy → destroy。
- pageFinished 不注入脚本。start/capture/startScroll/locateText 才 evaluateJavascript；默认正式 UI 没有路径到这些操作。旧 capture.js/run.js 的内部接口方案未被当前 controller 安装。
- DomTrialExportTest 反射取 MainActivity 的实例，16项仍执行，2项 Gecko 专用明确退役。tools/dom-trial 四组浏览器测试可独立执行。正式 dom-trial.yml 的 build 和 Android job 都仍运行 legacy，故无关正式提交也被旧 scanner 阻塞。
- scroll-trial 当前是单向一遍：顶部向下，底部向上，中间先到底部再向上；不是旧文档曾描述的两轮。保留 stable ID、相邻窗口 overlap/减步重试、拓扑顺序、role/text 冲突、稳定等待、H02/H03/H06、取消恢复位置和次数/时间/大小上限。
- dom-trial 保留严格 data-turn=assistant 的 article 或 conversation-turn 标记容器 fallback；progress-discovery 和 locate-text 返回结构计数。扫描只读公开 DOM，无 backend/React/storage/网络拦截。

## Step 2：隔离实现（已实施，Android待验证）

BuildConfig.ENABLE_LEGACY_SCANNER 默认 false，仅 Gradle property `-PnovaLegacyScanner=true` 打开，debug 不自动开。默认不实例化 controller，移除空 hook；实验设置入口必须二次确认、用户点击开始，暂停立即取消，不自动重启。保留类名、assets、历史证据，避免多 variant/sourceSet 系统。实验文件命名 Nova-legacy-scan，metadata 永远 historyCompleteness=not-proven。正式 Frozen Snapshot 不变；未来 Archive 仅规划用户导入官方 Data Export，未实现。

## Step 1 验证

静态调用链和远端文档核对 success；当前修改尚未构建/运行，fixture与真机未验证。下一步实施隔离和文案/诊断，再分离CI并运行正式与legacy回归。

Step2本地DOM17/snapshot13浏览器夹具通过；实验入口、暂停与Android文件仍待独立测试。扫描器仅保留HTML/Markdown（旧Gecko PDF在本轮之前已移除），正式System Print PDF不变。类名保持以减少迁移风险，注释/flag/设置/文件/metadata明确Experimental。
