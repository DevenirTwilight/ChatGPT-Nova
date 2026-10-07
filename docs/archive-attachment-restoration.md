# Archive Attachment Restoration — 调查与设计

2026-10-07，工作分支 feature/export-conversation，调查源码 e0ecabddfe6bb52a0569d8fc5b0fafee16f71180（生产测试源码1990cb1）。远端默认HEAD main/e8ffa0c6，不作为开发基线。正式CI37634607647与Legacy37631515308均success。

用户最新任务报告真实会话兼容性Level2–4在其本地通过（首次/重复导入与计数）；这是用户报告，本开发环境未独立运行真实ZIP。附件约326个.dat及PNG/JPEG/PDF/DOCX/XLSX属于用户审计事实，不是Nova已恢复结果。真实附件schema与实际字节仍在用户设备，AR级别独立于会话兼容级别。

## 当前真实调用链

ArchiveActivity SAF选择→私有cache ZIP→ArchiveStore单事务→ArchiveImporter streaming JSON→conversation/messages。schema1没有asset/reference表；ArchiveModel.Node.text对asset显示metadata占位；Reader与HTML/MD调用同renderer，PDF打印静态HTML。SnapshotWebView offline入口拒绝全部资源，未开启file/content/JS/bridge。现有FileProvider只暴露cache camera/exports，与Archive无关。删除Archive当前仅DB/WAL与import cache。Stable Snapshot、原签名和Legacy代码均保持。

## 最小实施设计（尚未完成）

1. 共用有界ZIP central-directory验证，检查全部路径、重名、加密、方法、symlink/ZIP64；独立附件预算，不套用JSON64MiB。
2. 确认官方asset map与消息引用结构后，通过精确官方身份建立必要集合，不猜文件名/正文/hash/顺序。不识别的结构明确未恢复；需要用户提供完全虚构、等结构的映射与消息引用示例，无需上传私有ZIP。
3. SQLite schema2增assets与message_assets，原v1非破坏迁移；官方身份唯一、随机私有文件名、display metadata、检测/声明MIME、大小/校验、state、first/latest source、引用ordinal/kind。不存BLOB；保留原分支/current_node/原始metadata。
4. preflight→bounded map→streaming conversation与refs→候选大小/空间规划→按需copy到私有generated文件→CRC/size/magic/dimensions→finalize files→DB事务提交。失败删本次新文件，保留原complete；process-death留下无DB引用文件由锁内清理，不清旧complete。DB commit后诊断失败不得删已提交文件。
5. Shared DisplayContent保留text/Map.text、parts精确顺序、metadata附件在正文后、合法attachment-only；隐藏thoughts、标注recap。所有reader/export同语义。
6. Reader只通过精确app-local origin提供已验证图片，no JS/file/network/bridge；独立private FileProvider仅archive资产目录，content URI临时只读外部打开。HTML bounded data PNG/JPEG（超额明确descriptor），MD descriptor，PDF本地images与文档卡片，不合并附件页。
7. 删除DB/WAL/资产/pending/cache，保留外部SAF文件与在线Cookie。synthetic JVM与Android26/35/36→原Archive/Stable/Legacy→实际输出核对→用户本地真实AR2–5。

## 待确认的安全预算

容器512MiB/JSONentry64MiB/selectedJSON256MiB等现有边界不扩大。附件暂定单件32MiB/合计256MiB/每导入2048/每会话256；图片最大边16384/16百万pixels，OOXML目录2MiB/2048entries/声明展开64MiB。此为保守设计，未经真实附件尺寸分布验证，超限安全失败，不冒充真实兼容。copy前剩余空间需候选新附件+DB/WAL128MiB+reserve32MiB，输入已占用空间计入SAF阶段。

## 验收边界

AR1–AR5均尚未完成；本页为设计，不能称附件功能已实现。真实ZIP/正文/文件名/ID/path/hash/附件/截图不得进入GitHub、CI或fixtures。需要补充脱敏结构证据，之后接通mapping、DB、Reader/exports。当前可以独立完成ZIP/MIME/存储安全基础及synthetic tests。
