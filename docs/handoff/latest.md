# 当前交接：冻结网页三格式（2026-10-07）

工作分支 feature/export-conversation；已验收应用/测试源码 ac4776ff31feaed9794dbc434e423ac291fbee83。文档提交不是APK源码HEAD，开始续接先fetch核远端，读AGENTS/latest/REVIEW/docs/frozen-page-save.md、实际源码和CI，不回退旧修复。

## 已实现

用户最终要求普通HTML/Markdown/PDF，取消Share/MHTML/新完整历史算法。正式“保存当前网页”→一次evaluateJavascript同步深clone→同clone静态HTML和Markdown→不可变FrozenPageSnapshot。HTML为唯一标准表示；PDF仅同frozenHtml→独立无JS/static WebView→Android System Print。Nova860px阅读/白底print CSS，不复制ChatGPT SPA。无滚动/backend/private reader/React/token/storage读取/ID去重/跨窗口缓存，不重构输入上传下载分享。

阶段A保留Gecko完成实际受控Nova/Firefox×Android35/36 PDF保存/独立重验后，阶段B移除GeckoPdfExporter/唯一旧PDF引用/dependency/仅Mozilla仓库/许可资产/lint例外/专属ABI和650MB交付。保留旧dom/scroll/progress/locator源码及证据，正式菜单不调用；16旧Android例继续，2Gecko专项明确Ignore保留历史。Firefox/OCR只CI，不加入生产。

## 验收与证据

最终CI https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37556129018 全绿：build、Android35、Android36均success。两API小证据下载并独立复核，四份PDF均31页Letter；最终API36 SAF HTML三浏览器file打开通过。实际HTML/MD/PDF、同snapshot live mutation隔离、唯一打印源与Firefox源逐字节相同、标准PrintUI保存/取消/多次/生命周期、旧容器/权限/上传/下载/blob/Share/所有IME/native及A大包→B/v1→B覆盖升级均有受控测试。具体结果和13项限制见docs/frozen-page-save.md及tools/snapshot/evidence。不是物理设备/登录真实账号成功。

最终主APK1910773字节(1.822255MiB)，SHA fa96b8714047ae707bbb013be8473be98a51db92cd826c4652f7ebe0d008e5c5；原包com.example.chatgptnova、versionCode14/1.4.0-scroll-trial、cert f93221ee0d2be2b806233a0b3427ec9c14766100c1bab3208841e6203e2b4289。A7 universal681117497字节；B无native so、Gecko Dex/deps，同一包支持ARM64等原架构，不假造split。总安装占用未知，APK不包括共享System WebView/应用数据。独立主APK artifact11454731981，不混测试APK，无公开Release。

历史CI A7(37552313716)原host首标题文本误判仍红，四实物独立NFKC/必要Firefox首标题144DPI离线OCR+人工图像通过，android-pdf-firefox-A7.json保留原失败。B1 API36仅旧菜单预期失败、其余全部通过；B1 API35取消不记通过。最后只同步4个菜单预期，账号设置/清理断言保留。不得将历史红/取消改称绿。

## 剩余边界与继续工作

真实账号与物理设备、厂商WebView、相机/麦克风硬件手动验收尚未执行；用户ADB条件未知，不反复追问。用户可以直接同签名覆盖安装测试APK核当前加载内容/普通HTML/系统打印质量。不继续追求无法证明的完整历史或自动Share。

DOM100000元素/12MiJSON硬限，无静默截断；SAF后台UTF8/64KiB流式复制/wt截断旧目标、非空长度核对，取消/destroy释放。S01–S10脱敏diag不含正文/真实ID/URLquery/凭据。外链图片字节非冻结/可能需网络与变化；blob/附件原文件/canvas/shadow/runtime未备份，inlineSVG移除，数学可读/公开LaTeX非像素级。静态资源无Cookie/Auth、有数量/大小/时间上限。

保持原签名/包名/版号；不main/forcepush/公开Release。必要源码/测试/文档可提交push本分支。仅实际压缩/迁移或用户明确要求时维护入库交接及下一会话提示词，普通回复不生成。/tmp/nova-stage-b.py已应用勿重复执行；CI证据永久描述在Git，原artifact有期限。

三种格式严格对应同一次当前网页冻结快照，但无法证明ChatGPT服务器端完整会话历史；如果网页虚拟化未挂载较早内容，保存结果也不会包含那些内容。
