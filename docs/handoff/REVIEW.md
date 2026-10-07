# 最终审查（2026-10-07）

## 最新维护要求

用户2026-10-07明确要求每完成一个工作步骤更新交接，并让后续AI也遵守。AGENTS/latest/README及下一会话模板已同步：latest逐步记录实际结果与下一步，验证结论/边界变化时同步本文件；交接随步提交push，不等压缩/迁移，普通更新不生成迁移提示词。此步骤仅文档修改，用diff及一致性检查，不触发新APK或公开Release。下列已有验证结果未改变，下一步等待用户真机反馈。

## 本轮隔离边界（Step 1）

Stable=当前网页Frozen Snapshot三格式；Archive=未来主动导入官方Data Export（未实现）；Experimental=仅显式开启的Legacy Conversation Scanner，当前公开DOM滚屏累计，historyCompleteness永远not-proven。已完成静态调用链审查，Step2已实施默认关闭与实验确认/前台取消/diagnostic白名单；浏览器DOM17与snapshot13通过，Android构建/UI暂未验证。旧错误/29或39条/虚拟化/progress/Share和历史CI证据保留，详见../legacy-conversation-scanner.md。

## Step 3测试分离

正式保存CI不再依赖legacy执行；新legacy-scanner-tests workflow显式flag只在相关路径运行，19项（16旧+3新），Gecko2项明确退役。default-build新合同2项不许skip。当前源码尚待两套Android CI；旧ac4776f结果仅历史。已加通用NAV/ARIA控件过滤及其fixture，不新增网站selector。架构详情见../legacy-conversation-scanner.md。

## Step 4a兼容与隐私边界

对minSdk26避免Set.of新平台API；网页返回error字符串也通过固定错误码枚举，不只过滤nested diagnostic。两套755d727 CI尚未完成，不记录为通过，下一提交需重跑。无版号/签名/包名/正式快照更改。

## Step 4b本地与构建证据

已测源码4ca3c04：本地DOM18/scroll32/progress19/locator18/snapshot13通过，正式CI37587862891的build/lint/unit/原签名通过，Android35/36尚进行中；legacy37587862825尚进行中。独立JSON在tools/legacy-scanner/evidence/isolation-validation.json。未删除历史文件，五个正式核心源码无diff；不借用历史APK结论。755d727旧运行已cancelled，非success。

## Step 4c实际包检查

独立签名及实际DEX已核：default=false、experimental=true，原证书；实际APK分别1916121/1917341bytes，脚本匹配4ca3c04，包无native so。package-verification.json保留摘要。四Android job仍进行中，不把构建成功当默认设置/实验前台取消/正式PDF实物成功。

## Step 4d旧断言与计数

旧文件文案断言同步为实验记录语义，保留原40条/恢复检查并增加captureMode/not-proven；脱敏白名单补数值users/assistants并强断言20/20。新源码需重跑CI；前次APK签名/DEX核对仍仅属于4ca3c04。

## Step 4e实际前次失败

4ca3c04两档legacy各19项仅旧文案断言1失败，新增三项与其他旧项通过。两套run最终cancelled；实际failures如实入JSON，不改写。最新5d9efe8两套CI仍进行中，不能宣称安全隔离已全部验收。

## Step 4f最终候选包

5d9efe8实际包DEX source与false/true已核、原签名v2crypto verified；1916153/1917377bytes。最终package-verification.json与旧4ca3c04报告分开，Android仍待验证，不宣称完整安全隔离已验收。

## 现有验收结果

已测source ac4776ff31feaed9794dbc434e423ac291fbee83；CI37556129018 build、Android35/36全success。两API实际HTML/MD/PDF下载独立核Unicode/结构/首尾/后续mutation排除，两份打印源与Firefox逐字节相同，四PDF均31页，抽查长代码/表格/尾页可读。最终SAF HTML在Chromium/Firefox/Edge以普通file打开通过。完整13项报告及限制见../frozen-page-save.md，来源与原失败保留tools/snapshot/evidence。

Gecko生产路径/依赖/仅Mozilla仓库/引擎资产/ABI大包workflow均移除；实际APK1910773字节，原包名/签名/版号，Dex/deps/ZIP无Gecko或native so。同APK适用ARM64等原架构；两API installedBaseAPK相同字节，总安装占用du无权限未知。测试Firefox/OCR不进主包。A→B/v1覆盖升级、全部旧容器/分享/输入/IME/native回归通过，16旧Android继续、2Gecko专用明确退役，不冒称18全执行。

单clone生成HTML/MD、PDF同唯一HTML；无Share/MHTML/history算法/private API/认证读取。保留旧扫描源码和证据，不进入正式菜单。全部真实文件来自受控模拟器/fixture，不是物理设备/登录账号人工成功；硬件体验/厂商WebView仍需用户真机测试。无公开Release/版本递增/main合并。

历史A7原CI因Firefox粗体首标题text提取仍红，实际四PDF独立NFKC/仅必要首页离线OCR+视觉重核通过后才清理Gecko；B1旧菜单预期失败及API35取消留历史，不改称成功。最终B2断言保留并完整绿。

三种格式严格对应同一次当前网页冻结快照，但无法证明ChatGPT服务器端完整会话历史；如果网页虚拟化未挂载较早内容，保存结果也不会包含那些内容。
