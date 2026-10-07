## Attachment Restoration：当前实现已接通，验收进行中

SQLite schema2非破坏升级新增assets/message_assets；用户导入ZIP中已实证的sediment://file_32hex与metadata file_32hex精确对应file_32hex.dat，保留parts顺序，metadata重复图片去重，attachment-only合法。private UUID文件、streaming copy/CRC/hash/MIME/尺寸、官方身份upsert/first-latest来源、事务取消/失败与启动recovery、删除本地档案含assets/pending。binary不存SQLite BLOB，无DOM/在线Cookie/backend/cloud同步。

图片Reader/PDF离线显示，文档card通过独立窄FileProvider临时content URI只读打开；no app有提示。单HTML PNG/JPEG bounded data URI，单图2MiB/合计6MiB；超限明确descriptor，document仅“Archived locally in Nova”描述不携binary/无private链接。Markdown只descriptor，不携图片/文档binary；PDF会话打印images+cards，不append附件文档页。未知binary不autoembed；缺失/损坏显式不可用，旧已校验资产不被坏输入覆盖。

预算：container512MiB/JSONentry64MiB/JSONtotal256MiB原边界保持；独立asset single64MiB（真实PDF63.62MiB必要）/total256MiB/2048per import/256per conversation、ratio200、图片最大边16384/16Mpixel、nested OOXML2048entries/2MiBcentral/64MiBdeclared展开。恢复前新增候选bytes+DB/WAL128MiB+reserve32MiB剩余空间检查，SAF阶段另计input cache。官方格式未来可能变化，不保证任意export/附件/server完整性。

本地JVM93与编译通过；新Android/实际三格式/签名/Stable/Legacy验收待CI，不称AR1完成。真实手机重打包数据已授权在repo外审计122/7250/7130、mapped326 CRC全部通过，230PNG/JPEG、82DOCX、5PDF、5XLSX、4unknown；此为AR2结构检查，真实Nova DB/Reader/重复/离线/人工AR3–5仍需用户本地验收。私有原文件/正文/ID/name/path/hash不进入repo/CI/fixtures/logs。详情[完整步骤与证据](archive-attachment-restoration.md)。下方历史schema1与metadata-only段落记录当时实现，以上为本轮当前状态。

> 最新用户任务报告：真实会话兼容Level2–4已在用户设备通过（首次/重复导入和统计）。这是user-reported local validation，开发环境未读取该ZIP；下方历史pending保留其当时状态。附件metadata占位属于当前Nova实现限制，用户官方ZIP已报告包含实际binary。本轮Attachment Restoration正在进行，尚未接入Reader/exports，详见[调查/安全基础与待办](archive-attachment-restoration.md)。AR1–5与会话Level独立，未验收。

# Nova Archive

## 设计基线（Step 1，2026-10-07）

远端HEAD为main/e8ffa0c6；工作分支feature/export-conversation最新d48aed0da152025902b83b1027b3a8e8bfc918ad。正式CI37593888527及实验37588638326已success；历史红/取消不当现状。仓库没有可使用的真实官方Data Export样本，本轮只建立脱敏synthetic/compatible fixtures，不声称real OpenAI export verified。此段记录调查基线；当前验收见下节。

## 前轮 MVP 验收结果（2026-10-07）

**Nova Archive MVP implemented — synthetic / fixture validation passed。** 验证实际源码 `7854f3ea06309fa20e5e26a7acc4b77750322968`，后续提交仅文档/证据；[正式 CI 37609817075](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37609817075) 的 build、API26、API35、API36 全部 success。没有真实官方 Data Export ZIP，也没有物理设备验证，**不是 real OpenAI export verified**。

| 验证 | 结果 |
| --- | --- |
| Gradle release / AndroidTest、lint、签名 | success；原包名/签名/code14/name保持 |
| JVM核心 / browser snapshot fixture | 39 / 13 通过 |
| Android26 Archive基础 | 2 通过 |
| Android35/36 Archive | 各17通过，另各seed/restart2通过，真正force-stop后持久化 |
| Android35/36 Stable | 各17套43次执行通过，含快照10、登录/菜单/上传/下载/分享/IME/生命周期/原签名升级 |
| 实际 Archive HTML/Markdown/PDF | 独立下载核对；PDF各16页，首尾/code/table/多语言/LaTeX源正确，非当前分支排除 |
| 实际 Frozen Snapshot / Firefox对照PDF | 各API两份31页；Unicode/首尾/长代码/表格/图片/晚变更排除通过，同打印HTML源 |
| 独立Legacy回归 | [37604491058](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37604491058)，各API19通过；2个历史Gecko-only专项仍明确Ignore |

Legacy验证源码 `66172a1d` 与上述最终源码之间的 scanner、MainActivity、共享SnapshotWebView、构建依赖/flag及旧assets没有变化；它仍默认关闭，不据fixture通过宣称真实完整历史。证据在 [accepted-runtime-35.json](../tools/archive/evidence/accepted-runtime-35.json)、[accepted-runtime-36.json](../tools/archive/evidence/accepted-runtime-36.json)、[accepted-package-verification.json](../tools/archive/evidence/accepted-package-verification.json)、[legacy-regression.json](../tools/archive/evidence/legacy-regression.json)。原失败报告也保留：数据库容量接口、显式null根节点往返、旧菜单预期及PDF夹具editable filename选择；后续修正后重新验证，没有将失败改写成成功。

## 产品与模块边界

Nova Archive是用户主动导入OpenAI官方Data Export兼容ZIP/JSON后，本地浏览、标题搜索、阅读与重新导出。真实性及完整性取决于用户所提供的导出文件，Nova不验证发行者，也不自动读取完整在线ChatGPT历史。Stable保存当前网页仍为currently-loaded-page；Legacy Scanner仍显式flag关闭/保留研究代码。Archive不使用DOM、Cookie、storage、internal reader、undocumented backend、React/Fiber或账号爬取，不连接云/analytics/embedding。

核心放在archive Java package：流式JSON/ZIP、数据模型、树选择、去重与Java Markdown→HTML renderer；Android适配器只负责私有SQLite、SAF、线程、独立Activity及静态打印。SQLiteOpenHelper而非Room，避免注解/编译系统和迁移复杂度；schema版本1，不把整个Archive常驻为JSON。

## 数据模型与schema v1

- import_sources：随机import_id、用户所选显示文件名（仅私有数据库/UI）、started/finished时间、status/error code、schema、输入字节、new/updated/skipped/failed计数。
- conversations：本地整数row_id、nullable官方conversation id（非空ID唯一）、title/搜索规范化title、create/update时间、current_node、保留未知header metadata的raw JSON、first/latest import时间与来源。缺ID保守生成新记录，不按正文或标题匹配。
- messages：本地conversation外键、node_key（mapping身份）、官方message id、parent、is_message、role/channel/content type/status/time、原 node JSON、first/latest来源。保留null根节点、空消息、tool/developer/unknown、非文本和附件metadata；mapping对象顺序不作为消息顺序。
- raw JSON保存受限单会话/节点metadata，不把所有会话合成一个巨型JSON。ID只存在私有DB/用户导出，不进入diagnostic/logcat。

官方ID用于upsert；mapping node key用于保留树身份，message id同时保存。相同内容不同ID不合并。重复相同输入按保留数字精度的 JSON 对比跳过；新导出合并节点并保留旧分支，较旧update_time不覆盖较新节点/标题/current_node。日期缺失或相同且不同内容时采用明确的最后用户导入策略。失败/取消全部会话变更回滚，单独保留失败导入统计，committed counts为0。

## 导入 pipeline 与支持的 schema

ACTION_OPEN_DOCUMENT → content URI → 后台限量复制私有 cache → 按所选文件扩展名识别 ZIP 或 UTF-8 JSON → ZipFile中央目录检查 → 遍历/发现conversations.json或conversations[-_]编号.json（可在文件夹，顺序无关）→ JsonReader逐会话 → validate/model → SQLite transaction/upsert → summary → 删除临时ZIP。

JSON支持会话数组、conversations数组wrapper和单会话mapping对象；未知会话/节点字段及显式 null 保留，未知wrapper字段仅有界解析。malformed JSON、重复JSON键、mapping类型错误明确失败。parts中的String和Map.text:String读取；明确asset_pointer/image_url/file_id用附件占位，未知Map用不支持占位，全部raw保留。thoughts默认不显示为答案，主链与全部节点视图/HTML/Markdown/PDF同样隐藏，但数据库/树完整保留。reasoning_recap支持content/text/recap:String（或已有parts），单独标“推理摘要”；没有可读文本时明确摘要占位，不猜任意字段。消息字段未知/非text不猜正文，原始metadata保留；附件文件本轮不解压/嵌入。

树按current_node parent链重建；可确定唯一叶时使用其链；缺失 parent 所指节点标注部分链；parent 字段本身缺失/类型错误时明确使用全部节点安全顺序；cycle/多叶无法确定时按时间+稳定node key的明确“非单一分支安全顺序”回退。保留所有节点，并可查看全部节点；不按正文相似度拼接，不伪造身份。

## 安全上限

容器输入512MiB（ARCHIVE_CONTAINER_LIMIT）；ZIP 中央目录先有界逐条检查（最多 16MiB，拒绝 ZIP64/多卷/自解压封装），之后 ZipFile；最多10000 entries、单entry64MiB、声明总解压512MiB、所选JSON实际累计256MiB、压缩比最多200；拒绝absolute/drive/backslash/traversal/重复canonical entry。任何entry都不写入用户路径，无Zip Slip解压面，忽略无关文件正文但校验目录大小/路径。CRC/实际长度检查用于所选entry。

UTF-8严格解码；JsonReader流式逐会话，预读取guard限制单字符串512Ki chars和嵌套64，避免nextString先分配巨大token；单会话总字符串及对象键4Mi UTF-16 chars、最多100000 JSON values/10000 nodes；最多 10000 conversations / 200000 nodes 每次导入。数据库512MiB、单序列化/合并存储/重载会话 payload 4Mi UTF-16 chars（最多约 16MiB UTF-8），单 header/node 1MiB UTF-8，identity/parent key 最多4096字符，避免 CursorWindow 超限、HTML4Mi chars/Markdown2Mi chars，超限明确失败不截断。输入和解析过程检查取消/5分钟耗时；provider自身阻塞只能尽力关闭/中断，不保证任意SAF provider立即响应。

Reader/print静态HTML：JS/storage/file/content/混合内容/bridge关闭，所有请求阻止，无远程图片/字体/脚本；仅用户点击HTTP(S)链接时交系统浏览器，不转发Cookie/header。Markdown AST 节点数/深度有界检查，防 HTML renderer 递归过深。Raw HTML转义；不执行Markdown HTML或data/javascript链接。图片/附件保留文字metadata占位，LaTeX保留可读源，不新增MathJax/网络/OCR。

## UI 与生命周期

设置 → Nova Archive（无需ChatGPT登录）→ 导入ChatGPT数据、标题搜索、最近更新/最早时间排序、每页最多200条的会话列表（上一页/下一页，不累积整个库在内存）；行显示标题/日期/消息数；超长标题展示前 512 字符，原始标题保留，搜索输入最多 256 字符。首页控件作为原生 ListView header 可滚动，横屏不会挤没列表；独立Reader区分角色/channel，主链或明确全部节点安全顺序；导出HTML/Markdown/打印PDF。

导入使用application context worker，不持有已销毁Activity；旋转可保留任务/重绑定回调，取消/真正销毁回滚，重启只读取已提交SQLite。UI不自动重开URI/继续导入。Reader导出基于已载入Archive模型，SAF保存用wt；printing按现有静态WebView可见状态→PrintManager，finish/cancel/destroy释放renderer，不把onFinish视为成功。诊断仅随机import id、schema/version、阶段、数量、字节、耗时、固定错误码，可复制。

## 复用与不复用

复用：现有PrintManager/createPrintDocumentAdapter/visual state/cleanup，PageSnapshotExport文件名安全规则、SAF流式写入wt与generation保护的方式，freeze.js阅读/打印CSS的布局原则。SnapshotWebView最小新增独立HTML/offline入口，旧snapshot入口行为保持；必须跑正式回归。

不复用：FrozenPageSnapshot数据结构、DOM clone/role selector、legacy扫描/后台接口、ChatGPT session桥、JS marked（Reader默认无JS）。新增小型Gson streaming与Java 8 兼容的 CommonMark/GFM tables 0.21.0 与 Gson 2.11.0 依赖，许可证随包；不引入搜索引擎。MVP仅标题搜索，不做全文FTS/标签/摘要。

## 错误与验证

A01 unsupported、A02 invalid ZIP、A03 no conversations、A04 malformed JSON、A05 too large、A06 DB write、A07 cancelled、A08 unsupported schema、A09 storage；Reader A10_READER_FAILED、导出 A11_EXPORT_FAILED、打印 A12_PRINT_FAILED。错误不包含输入片段/真实ID/路径，失败统计不把回滚记录说成已导入。

JVM：ZIP发现/分片/顺序/无关文件/重复entry/traversal/bomb/CRC、strict JSON/未知字段/深度/超长、树current/编辑/分支/孤儿/cycle/角色/非text、重复导入/新旧版本、安全Markdown HTML，以及1/100/1000会话/长代码/中法英/emoji合成夹具。Android：SAF/取消/持久化/重启/列表搜索排序/Reader/HTML/MD/实际System Print PDF/取消/旋转/销毁/导入生命周期；原有Nova回归与原签名升级必须保留。证据严格标synthetic / fixture validation passed。

## Real OpenAI Data Export compatibility — fixture修复通过，真实设备待验收

用户提供结构审计：370566688 bytes（约353.40MiB）、605 entries、普通non-ZIP64单卷Deflate；两个JSON约49.44MiB/6.00MiB，合计55.44MiB；122 conversations、7250 mapping nodes、7130 message-bearing nodes、约4267 current-branch messages。content_type约text3216/multimodal_text892/thoughts1874/reasoning_recap1148；这些是用户审计数据，**本开发环境未独立读取真实ZIP**。当前样本据用户审计未使用ZIP64；ZIP64仍不支持。

旧版容器256MiB必拒绝此样本，SAF有SIZE时复制前A05，否则复制超限A05；旧parser chars1Mi、序列化与Store聚合2Mi也不足。本轮只将容器增至512MiB、单会话字符/序列化及Store合并/重载统一4Mi chars，其余entry64MiB/selected total256MiB/声明解压512MiB/节点1MiB UTF-8/depth64/字符串512Ki chars/100000 values/10000 mapping/entries/ratio200/DB512MiB保持。不是所有预算一起放大；parent链深度不是JSON nesting。

私有空间：复制前要求已知ZIP大小 + 128MiB DB/WAL规划额度 + 32MiB固定余量（当前已知样本约需513.40MiB空闲）。SIZE未知时先保留160MiB，并每复制1MiB再检查；解析/事务逐会话保留32MiB，空间不足A09并回滚；128MiB仅规划额度，不保证任意输入足够。临时文件成功/失败/取消删除，process death后下一次无运行任务的Archive启动清理stale temp。保留单事务，未扩大数据库。

成功诊断新增buildRevision、输入mapping/message/current-branch/可见branch计数、白名单content_type统计、复制/解析/总耗时、临时bytes、前后空闲、关闭DB后的主库bytes、采样Java used heap与WAL峰值。采样值不是精确profiler峰值，整个进程heap包括WebView/应用；取消延迟/ANR需设备人工观察，诊断不含正文/title/ID/文件路径/原filename/私有文件hash。Reader数量为可见记录，列表message count仍含已保存thoughts，不将隐藏误称删除。

当前等级：**Level1 synthetic / fixture validation passed**，实际源码`1990cb1ffb7a0832e94c552cc349023e6e5989d9`，[正式CI37634607647](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37634607647) build/API26/35/36全success：JVM48/browser13/API26基础2；API35/36各Archive20+独立force-stop seed/restart2、原正式17套43执行。实际Archive HTML/MD/16页PDF独立核对object text/recap/中文标签/首尾/code/table/Unicode且thoughts/OTHER-BRANCH排除；正式四份31页System/Firefox PDF仍同打印源、内容/图片/晚变更排除通过。新[LegacyCI37631515308](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37631515308) d36246e各API19通过+2历史Gecko-only Ignore，scanner/共享生产依赖与最终源码完全一致。后续提交只文档/证据；Level2容器/Level3真实导入重启/Level4真实重复导入 **pending**；Level5 **manual content validation pending**。用户明确真实ZIP在其手机/电脑，仅使用修复签名APK本地验证，不上传开发服务器、GitHub/CI或输出文件。旧版真实运行失败也需用户本地实测，不能以源码推断冒充运行结果。[本地验收清单](archive-local-validation.md)。前轮MVP的已通过证据保留，不能代替本轮新源码验收。

## 数据删除与限制

Archive 首页“删除本地档案”：先结束导入、关闭DB、删除nova-archive.db及WAL/SHM和Archive临时文件，不清ChatGPT登录。卸载或Android清除应用数据也删除Archive。Nova“清除登录与网站数据”继续仅管理在线会话。用户自行导出的外部HTML/MD/PDF需自行删除。

schema、解析器、Activity、实际三格式导出、重启/生命周期和正式回归已通过上述受控验收。没有真实官方ZIP或物理设备验证；官方格式未来可能变化，不是对任何版本/大小/附件完整性的保证。ZIP64、多卷、自解压、大于限额的官方导出本轮不支持；图片/附件只是metadata占位；LaTeX保留源，不执行数学渲染；标题搜索，不含全文搜索。没有本地全文检索之外的云同步/自动摘要/embedding。

下一步先由用户选择真实兼容的官方导出文件进行本地人工验收，真实聊天内容不得提交代码仓库或上传到GitHub。本轮不要加入FTS/标签/附件下载/Cloud/Legacy增强；先完成真实样本分级验收。

## 文件导航

- 原生适配：`ArchiveActivity.java`、`ArchiveReaderActivity.java`、`ArchiveUi.java`；manifest与MainActivity只接入私有Activity/设置入口；SnapshotWebView增加独立offline静态HTML入口。
- `archive/`核心：ArchiveError、ArchiveModel、ArchiveImporter、ArchiveTree、ArchiveMerge、ArchiveStore、ArchiveRenderer；无ChatGPT DOM耦合。
- JVM：ArchiveCoreTest、`tools/archive/jvm-tests.sh`；Android：ArchiveTest、ArchiveProcessTest、ArchiveMinSdkTest、ArchiveFixtures、ArchiveFixtureDocuments及test manifest。
- CI：`scripts/archive_android.py`、`.github/workflows/dom-trial.yml`；Legacy workflow只补共享SnapshotWebView触发路径。NovaWebViewTest同步授权新增菜单项，FrozenPageSnapshotTest只修正文件名控件选择，实际内容断言保留。
- 依赖/许可证：app/build.gradle中Gson/CommonMark/GFM与JUnit；`assets/archive-licenses/`；说明README、本页、Legacy产品分层和handoff，验收/失败证据 `tools/archive/evidence/`。

## 本轮签名包与证据

[下载最终签名APK Artifact](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37634607647/artifacts/11487454505)，解压安装ChatGPT-Nova.apk，原包名/证书/code14/name保持。生产APK默认legacy=false、旧export/freeze assets一致、无新增native引擎，test provider/虚构聊天不入生产包。v2 signature/content digest、源码revision及SHA256已独立核对；无需GitHub Release。

[API35实际输出](../tools/archive/evidence/compatibility-runtime-35.json)、[API36实际输出](../tools/archive/evidence/compatibility-runtime-36.json)、[最终包](../tools/archive/evidence/compatibility-1990cb1-package-verification.json)、[Legacy回归](../tools/archive/evidence/compatibility-legacy-regression.json)、[用户审计与分级状态](../tools/archive/evidence/real-export-compatibility-summary.json)。本轮两次fixture失败也保留：不可变Map及旧short SAF输出缺schema标记；修正夹具与加强检查，没有改生产打印/扫描或删除断言。真实文件/正文/路径/输出/私有文件hash未进入仓库或CI。
