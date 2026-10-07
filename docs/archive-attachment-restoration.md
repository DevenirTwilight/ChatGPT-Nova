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

## Step B：独立安全基础已实现，附件端到端仍待结构证据

新增ArchiveZip共用preflight，全部outer/nested路径在ZipFile前检查，拒绝symlink/特殊Unix文件、加密/未知method、ZIP64 extra、malformed extra、local/central名称或flags/size冲突、重名canonical path、重叠entry、控制字符路径；保留现有容量/ratio/entry边界。ArchiveImporter已复用此验证，conversation parser/显示/DB保持。

ArchiveAssetFiles提供32MiB单件/256MiB总量/2048候选/空间计划、随机私有文件名、CRC/长度/SHA256流式复制、PNG/JPEG尺寸头检查、PDF magic、bounded OOXML目录/manifest（无XML执行、宏/混合类型不标DOCX/XLSX）、unknown安全存储。Batch只接收调用方明确提供的候选，missing/damaged可记录；未commit close删本批新文件，保留旧文件；DB成功结束事务后才调用committed。recover需要调用方持锁及DB真实complete集合，清理无引用生成文件/死进程pending；不猜引用。当前这些附件文件接口**没有接入生产导入或Reader**，原有导入只变化ZIP校验。单文件预算与真实尺寸分布尚未实测；图片header/MIME检测不是完整codec验证，后续Android显示前仍需decode bounds/安全验证。

本地ECJ/Java21 JVM -Xmx128m：73 tests passed，原48+新增25。测试二进制由ImageIO/ZipOutputStream生成虚构图片与文件；PNG/JPEG/PDF/DOCX/XLSX仅结构/存储基础，不是Reader/PDF实际附件显示验收。新增rollback、重复logical ref只copy一次、ZIP移除后持久文件、cancel、stale recovery、unsafeDB引用集合不删除旧文件等。跨导入数据库去重、schema2迁移、provider、真实离线Reader、exports、AR1完整mapping尚未实现。

下一步仍需conversation_asset_file_names.json及对应消息的完全虚构等结构示例，确认key/value、identity与entry关系；不能根据通用文件名推测本样本映射。CI新源码验证等待中；Legacy path补上述Archive安全类，以用户要求在新APK跑独立实验回归，不改变scanner行为。

## Step C：用户提供映射文件的独立结构核对

已在本地只读检查用户提供conversation_asset_file_names.json，未复制进repo/cache/CI或输出名称/ID。根object，326个string key→string value；键固定file_ +32hex+.dat，值为非空原display filename。不含MIME/bytes/dimensions/message relationship。只按原名称扩展名统计：PNG90/JPG140/PDF5/DOCX82/XLSX5/other4；**不是magic/type或binary已恢复统计**。证据attachment-map-schema.json不包含真实名称、ID、路径或文件hash。

这完成map结构核对，不完成真实ZIP central/binary及message-reference核对；AR2只partial mapping audit，AR1端到端仍pending。仍需一条完全虚构但等结构的图片/attachment-only消息引用，并以同一假ID对应map键，以确认精确转换（不能按filename、大小、hash、正文或顺序猜关联）。

## Step D：Android Gradle测试编译失败记录

55830d1两CI的测试编译失败：桌面AWT/ImageIO在Android bootclasspath不存在。改用纯Java生成同样12×8的虚构PNG/JPEG；原73项本地再通过，独立Pillow实际decode两图通过。生产实现不变，Android运行尚未验证；失败证据保留attachment-foundation-build-failure.json，不把本地ECJ通过当Gradle通过。

## Step E：verified map shape的有界解析已实现

ArchiveAssetMap只支持已经核对的object<string,string>，保留原显示metadata但不把它用于路径/身份匹配；明确精确entry key。ZIP discovery忽略无关文件，map缺失返回空，多个同leaf映射拒绝猜选。预算4MiB字节/2048条/key1024/display4096字符及lexical32768防单token分配；严格UTF-8/JSON、CRC/length、duplicate、cancel，所有错误固定码，不附parser exception私密数据。已有conversation JSON Guard默认512K lexical/原字符预算未改，仅允许map使用更小token限额。

本地核心80=原48+file25+map7全部通过。**独立用新Nova解析器只读用户上传的真实映射文件，326条全部成功**；没有ZIP、消息关系或实际binary输入。只输出count，无private names/identity/path/hash落库。此parser尚未接生产Store，schema仍v1。

消息关联仍是必需待办：map不能提供message关系，不能凭32hex或file-prefix进行未证实转换。已请求完全虚构但对应同一假ID的图片与attachment-only消息字段。拿到结构后才能接schema2+refs+upsert+Reader/provider+三格式+删除/重启与真正AR1。正式新CI/Legacy80源码验证进行中，AR2–5未完成。

## Step F：单个用户binary本地只读识别

用户另提供一个.dat，本地仅结构核对：3459605bytes，DOCX容器40entries，声明展开3737696bytes，[Content_Types].xml和word/存在，无xl/或vbaProject.bin；全部entries CRC通过。新Nova ArchiveAssetFiles.inspect亦通过，MIME为DOCX。该exported entry精确存在于用户先前filename map，映射display扩展名与检测结果一致。未读取正文或写入repo/CI/fixtures，未输出真实filename/ID/path/hash。safe count/type结果见attachment-local-binary-audit.json。

这是单个真实binary类型/容器/filename-map验证，**不是Nova DB导入/Reader恢复/跨导入去重/AR3通过**。映射仍无消息引用字段，不能由这个binary推导conversation/message归属。下一步仍需完全虚构但同结构的图片asset_pointer与attachment-only metadata，再接端到端。

9ab3f72新CI：正式37645986505 build/API26 success，API35/36尚在运行；Legacy37645986535各job success（本步核API状态，未独立下载新runtime artifacts，不重复引用旧runtime数量充当新证据）。

## Step H：完整引用结构已独立核对

15份用户授权Drive分卷已下载至repo外并合并，普通non-ZIP64/未加密ZIP607条（含2directory），是手机重打包副本，不称原ZIP字节一致。Nova实际bounded parser128MiB解析122/7250/7130/map326成功。observed parts.asset_pointer=`sediment://file_<32hex>`、metadata.attachments[].id=`file_<32hex>`、map/exported=`file_<32hex>.dat`；265+376refs均精确匹配326physical binaries，265同消息metadata/parts重复。所有326 CRC通过，magic与尺寸统计见attachment-repacked-container-audit.json，真实数据未入repo/CI/fixture。

single32MiB暂定预算被真实63.62MiB PDF证明不足，应独立调整为64MiB，total256MiB/JSONentry64MiB等不扩大；此为已验证必要调整，不是统一取消预算。230images最大2.36Mpixel，16Mpixel预算保持。每会话82asset、全导入326低于256/2048限制。引用结构证据已足，无需用户再手工样例；现在可接schema2/reader/export端到端，再验收AR1/3–5。

## Step I：shared ordered Display基础

ArchiveDisplay已按实证指针形式解析身份/entry，保留Map.text与inline part顺序，抑制同消息metadata重复图片，metadata附件按collection顺序接正文后；attachment-only合法标签，thoughts visible隐藏/refs保留，recap readable。unknown reference不猜转换。single64MiB实证调整，total256MiB/JSON不变。JVM88通过，新增8项值均虚构。当前尚未接schema2/Reader/export，不构成AR1完成。
