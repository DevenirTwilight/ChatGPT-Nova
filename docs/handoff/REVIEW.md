# 最终审查（2026-10-07）

## 最新维护要求

用户2026-10-07明确要求每完成一个工作步骤更新交接，并让后续AI也遵守。AGENTS/latest/README及下一会话模板已同步：latest逐步记录实际结果与下一步，验证结论/边界变化时同步本文件；交接随步提交push，不等压缩/迁移，普通更新不生成迁移提示词。此步骤仅文档修改，用diff及一致性检查，不触发新APK或公开Release。下列已有验证结果未改变，下一步等待用户真机反馈。

## 现有验收结果

已测source ac4776ff31feaed9794dbc434e423ac291fbee83；CI37556129018 build、Android35/36全success。两API实际HTML/MD/PDF下载独立核Unicode/结构/首尾/后续mutation排除，两份打印源与Firefox逐字节相同，四PDF均31页，抽查长代码/表格/尾页可读。最终SAF HTML在Chromium/Firefox/Edge以普通file打开通过。完整13项报告及限制见../frozen-page-save.md，来源与原失败保留tools/snapshot/evidence。

Gecko生产路径/依赖/仅Mozilla仓库/引擎资产/ABI大包workflow均移除；实际APK1910773字节，原包名/签名/版号，Dex/deps/ZIP无Gecko或native so。同APK适用ARM64等原架构；两API installedBaseAPK相同字节，总安装占用du无权限未知。测试Firefox/OCR不进主包。A→B/v1覆盖升级、全部旧容器/分享/输入/IME/native回归通过，16旧Android继续、2Gecko专用明确退役，不冒称18全执行。

单clone生成HTML/MD、PDF同唯一HTML；无Share/MHTML/history算法/private API/认证读取。保留旧扫描源码和证据，不进入正式菜单。全部真实文件来自受控模拟器/fixture，不是物理设备/登录账号人工成功；硬件体验/厂商WebView仍需用户真机测试。无公开Release/版本递增/main合并。

历史A7原CI因Firefox粗体首标题text提取仍红，实际四PDF独立NFKC/仅必要首页离线OCR+视觉重核通过后才清理Gecko；B1旧菜单预期失败及API35取消留历史，不改称成功。最终B2断言保留并完整绿。

三种格式严格对应同一次当前网页冻结快照，但无法证明ChatGPT服务器端完整会话历史；如果网页虚拟化未挂载较早内容，保存结果也不会包含那些内容。
