# 当前工作交接（2026-10-07）

## 实际状态：阶段A实物通过，阶段B已实现待CI

工作分支feature/export-conversation；先fetch核实际远端，不把文档提交当实现。用户已取消Share/MHTML/新完整历史扫描，当前目标普通HTML/Markdown/System WebView Print：一次clone→同clone HTML与MD→PDF只打印同一frozenHtml。三种格式仅当前已加载状态，不证明服务器全量。

阶段A source544f6f0d902b8224a46028d6032208c886af5981/CI37552313716：build/lint/签名/浏览器13及全部旧浏览器场景通过，Android35/36旧18、新10、Firefox实际保存1、Web9/Share5/ClipboardUi5/全部3项IME/native旋转返回重启/v1升级均通过。CI整体仍failure仅host Firefox first-marker文字提取误判，不能说该CI绿：实际PDF首标记肉眼可见但粗体标题无可提取文本，CJK用兼容字形；pdftotext仅缺首标题。修正验收为标准NFKC正文对照，首标题仅Firefox必要时用实际第一页144DPI离线OCR，断言没有删。离线对独立取回两API的Nova和Firefox四份PDF重跑全部正文/首尾/代码/表格/数学/法语/320×120图片/AFTER排除通过；人工抽查首页/代码/表格/尾页可读。tools/snapshot/evidence/android-pdf-firefox-A7.json保存原CI失败与重核方法/文件hash，不伪造原CI成功。四份均31页Letter，Nova278KB/294KB左右，Firefox356099/355771字节；Firefox附页眉页脚，分页合理但不要求字节等同。

在上述全部实际受控检查完成后实施阶段B：正式“保存当前网页”，HTML/MD/系统PDF；隐藏旧扫描与诊断菜单。删除GeckoPdfExporter/ConversationExport唯一旧PDF引用、Gecko依赖/仅Mozilla仓库/lint例外/许可资产/Gecko ABI与650MB交付workflow，保留Composer等核心模块。保留旧DOM/scroll/progress/locator源码、浏览器测试和16旧Android例；2Gecko专用例明确Ignore、源与Git历史保留，不冒称旧18仍全执行。

B还修两项本地保存正确性：SAF用wt显式截断较长旧目标，现有HTML例预置旧长内容再比较完整字节；受控样例证实CSS零裁剪隐藏文字原会进入两格式，freeze公开computedStyle隐藏判断补clip/clip-path，browser及Android/文件断言覆盖。不是消息完整性算法，无新live正文读取。

B CI改host PDF文字校验（离线OCR仅fixture，不入主APK），仍build/lint/unit及全部可用native/浏览器测试，并新增已验证A原签名大包→B合成Cookie/storage覆盖升级与两代实际APK/installedBaseAPK测量，继续v1升级。依赖图和主APK/Dex断言无Gecko及任何native so。独立nova-frozen-page-apk artifact仅主APK/校验/签名/大小，无Release job、contents只读。实际B CI与APK大小待提交后结果，不预报小包数字，不提前交付。

## 下一步

提交/push当前B（已授权，无force/main/Release），取得新CI号，查Android35/36全部结果；失败依现场修，不恢复Gecko或扩展扫描。B全通过再取主APK与小证据，独立核hash/cert记录/Dex/依赖树、实际PDF/HTML/MD及Firefox，测universal/ARM64（无native库可共用同包）与installedBaseAPK；总安装占用du无权限则未知。当前A APK681117497字节，source544；历史独立ARM64 source190113f为178491277字节，不能冒充本次A新ARM64包。

更新docs/frozen-page-save.md详细13项结果、README/legacy标记及本交接，push文档；提供稳定测试APK、GitHub CI artifact/文档，不只沙箱。版号仍1.4.0-scroll-trial/code14，package com.example.chatgptnova，原cert f93221ee0d2be2b806233a0b3427ec9c14766100c1bab3208841e6203e2b4289。真实物理设备/账号验收未执行，不把模拟器合成Cookie/上传/权限当真实成功；用户ADB条件未知，不重复八项问题。

## 架构与边界

FrozenPageSnapshot final字符串/长度+metadata复制；freeze.js同步采样公开状态并clone一次。Nova860px内联阅读/白底16mm print CSS，不复制ChatGPT脚本/CSS/控件。Markdown同clone，角色仅公开可靠结构，无ID/正文去重/合并/滚动/backend/React/Storage读取。HTML普通UTF8，MD后台UTF8→SAF，PDF独立无JS/storage/桥WebView→visualState→标准PrintManager，不要求最终PDF路径/onFinish不伪报成功。取消/结束/destroy释放；body100000元素/12Mi payload硬上限，静态资源无Cookie/Auth、HTTPS/4redirect/5秒/8Mi单个/64资源/32Mi总量。S01–S10及脱敏版本/随机snapshotId/格式/字符数/阶段/耗时，不记正文/真实ID/私密query/token。

外部图片字节不冻结，可能需要网络/变化；blob/附件文件/canvas/shadow/runtime不备份，inline SVG去除，数学为可读文本/公开LaTeX。实际Android SAF HTML已在Chromium143/Firefox144/Edge154普通file打开，无脚本/HTTP请求、Unicode/PNG/代码表格/printCSS通过。100+屏browser fixture通过，不等于真实完整历史。详细源与证据见docs/frozen-page-save.md、tools/snapshot/evidence。

三种格式严格对应同一次当前网页冻结快照，但无法证明ChatGPT服务器端完整会话历史；如果网页虚拟化未挂载较早内容，保存结果也不会包含那些内容。

仅实际压缩/迁移时维护并push交接，普通回复不要迁移提示词。历史完整过程在Git，不按旧HEAD回退。暂无待找源码；/tmp准备脚本已经应用，勿重复执行。
