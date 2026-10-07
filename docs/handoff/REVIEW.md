## 阶段B最新源码与严格A/B修正

阶段B源9d74db8540ec1fae64eb468ab7eb74562d22bbca已推，CI37554349621尚在运行；Gecko生产清理已实现，不回退。新增对照修正：此前A7两个测试分别freeze同fixture，正文一致但timestamp不同；现Firefox直接读取System Print测试保留的唯一frozen-page-print-source.html，host逐字节比较firefox-source.html，强制完全同源。同clone的生产架构本来不分格式重读live；此次只加强A/B测试，不改聊天模块。后续核本修正新CI，不将被后续提交取消的旧B检查记为通过；大小待实际构建。A7文件重核通过与原验收误判均保留。

# 实际审查状态（2026-10-07）

最新任务与源码以远端及latest.md为准。阶段A source544/CI37552313716所有Android instrumentation与实际文件保存通过，但CI原host first-marker text提取错误仍红。独立四份实际PDF经NFKC+仅Firefox首标题必要的144DPI离线OCR、正文/首尾/中法英/长代码/表格/数学/图片/AFTER排除和人工抽查已通过。粗体标题视觉存在，不删首标记断言；tools/snapshot/evidence/android-pdf-firefox-A7.json明确原失败与重验证。不能把原CI说绿，不用预览代文件，不冒充物理/真实账号。

基于实际受控A验证完成实施B：正式HTML/MD/SystemPrint，Gecko生产用户唯一旧PDF已删除，依赖/仓库/资产/lint/ABI交付移除。旧扫描源码和16可继续Android测试保留，2Geo明确退役有历史证据。新CI全部检查及覆盖升级/大小待结果；未通过前不交付、不预报大小。

A实际universal681117497字节；installedBaseApk同值，du未授权总占用未知。旧历史ARM64178491277来自source190113f，不假装是544的arm64测量。B无JNI预期单包兼容所有ABI，但必须实际ZIP/Dex/依赖证明及测APK，不能先声称具体小包数。

单同步clone→HTML/MD→同HTML无JS打印。无私有接口/Storage/token/Cookie读取、自动滚动/Share/MHTML；诊断无正文/真实ID/query，资源无Auth/Cookie、有上限，Activity释放，核心输入/上传下载分享不重构。SAF显式wt及长旧目标回归，CSS裁剪hidden样例已证实并修，完整性不靠消息计数。所有测试夹具/OCR/Firefox只CI或测试APK，不入生产。

三种格式严格对应同一次当前网页冻结快照，但无法证明ChatGPT服务器端完整会话历史；如果网页虚拟化未挂载较早内容，保存结果也不会包含那些内容。
