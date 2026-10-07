# Nova Archive

## 设计基线（Step 1，2026-10-07）

远端HEAD为main/e8ffa0c6；工作分支feature/export-conversation最新d48aed0da152025902b83b1027b3a8e8bfc918ad。正式CI37593888527及实验37588638326已success；历史红/取消不当现状。仓库没有可使用的真实官方Data Export样本，本轮只建立脱敏synthetic/compatible fixtures，不声称real OpenAI export verified。此段记录调查基线。当前代码已实现 MVP 路径，Android 验收正在进行；未验证部分不能当作通过。

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

JSON支持会话数组、conversations数组wrapper和单会话mapping对象；未知会话/节点字段保留，未知wrapper字段仅有界解析。malformed JSON、重复JSON键、mapping类型错误明确失败。消息字段未知/非text不猜正文，原始metadata保留；附件文件本轮不解压/嵌入。

树按current_node parent链重建；可确定唯一叶时使用其链；缺失parent标注部分链；cycle/多叶无法确定时按时间+稳定node key的明确“非单一分支安全顺序”回退。保留所有节点，并可查看全部节点；不按正文相似度拼接，不伪造身份。

## 安全上限

输入256MiB；ZIP 中央目录先有界逐条检查（最多 16MiB，拒绝 ZIP64/多卷/自解压封装），之后 ZipFile；最多10000 entries、单entry64MiB、声明总解压512MiB、所选JSON实际累计256MiB、压缩比最多200；拒绝absolute/drive/backslash/traversal/重复canonical entry。任何entry都不写入用户路径，无Zip Slip解压面，忽略无关文件正文但校验目录大小/路径。CRC/实际长度检查用于所选entry。

UTF-8严格解码；JsonReader流式逐会话，预读取guard限制单字符串512Ki chars和嵌套64，避免nextString先分配巨大token；单会话总字符串1Mi chars、最多100000 JSON values/10000 nodes；最多 10000 conversations / 200000 nodes 每次导入。数据库512MiB、单存储会话 payload 2Mi chars（最多约 8MiB UTF-8），单 header/node 1MiB UTF-8，避免 CursorWindow 超限、HTML4Mi chars/Markdown2Mi chars，超限明确失败不截断。输入和解析过程检查取消/5分钟耗时；provider自身阻塞只能尽力关闭/中断，不保证任意SAF provider立即响应。

Reader/print静态HTML：JS/storage/file/content/混合内容/bridge关闭，所有请求阻止，无远程图片/字体/脚本；仅用户点击HTTP(S)链接时交系统浏览器，不转发Cookie/header。Markdown AST 节点数/深度有界检查，防 HTML renderer 递归过深。Raw HTML转义；不执行Markdown HTML或data/javascript链接。图片/附件保留文字metadata占位，LaTeX保留可读源，不新增MathJax/网络/OCR。

## UI 与生命周期

设置 → Nova Archive（无需ChatGPT登录）→ 导入ChatGPT数据、标题搜索、最近更新/最早时间排序、分页会话列表；行显示标题/日期/消息数；超长标题展示前 512 字符，原始标题保留，搜索输入最多 256 字符。独立Reader区分角色/channel，主链或明确全部节点安全顺序；导出HTML/Markdown/打印PDF。

导入使用application context worker，不持有已销毁Activity；旋转可保留任务/重绑定回调，取消/真正销毁回滚，重启只读取已提交SQLite。UI不自动重开URI/继续导入。Reader导出基于已载入Archive模型，SAF保存用wt；printing按现有静态WebView可见状态→PrintManager，finish/cancel/destroy释放renderer，不把onFinish视为成功。诊断仅随机import id、schema/version、阶段、数量、字节、耗时、固定错误码，可复制。

## 复用与不复用

复用：现有PrintManager/createPrintDocumentAdapter/visual state/cleanup，PageSnapshotExport文件名安全规则、SAF流式写入wt与generation保护的方式，freeze.js阅读/打印CSS的布局原则。SnapshotWebView最小新增独立HTML/offline入口，旧snapshot入口行为保持；必须跑正式回归。

不复用：FrozenPageSnapshot数据结构、DOM clone/role selector、legacy扫描/后台接口、ChatGPT session桥、JS marked（Reader默认无JS）。新增小型Gson streaming与Java 8 兼容的 CommonMark/GFM tables 0.21.0 与 Gson 2.11.0 依赖，许可证随包；不引入搜索引擎。MVP仅标题搜索，不做全文FTS/标签/摘要。

## 错误与验证

A01 unsupported、A02 invalid ZIP、A03 no conversations、A04 malformed JSON、A05 too large、A06 DB write、A07 cancelled、A08 unsupported schema、A09 storage；导出/打印可增加固定A10/A11。错误不包含输入片段/真实ID/路径，失败统计不把回滚记录说成已导入。

JVM：ZIP发现/分片/顺序/无关文件/重复entry/traversal/bomb/CRC、strict JSON/未知字段/深度/超长、树current/编辑/分支/孤儿/cycle/角色/非text、重复导入/新旧版本、安全Markdown HTML，以及1/100/1000会话/长代码/中法英/emoji合成夹具。Android：SAF/取消/持久化/重启/列表搜索排序/Reader/HTML/MD/实际System Print PDF/取消/旋转/销毁/导入生命周期；原有Nova回归与原签名升级必须保留。证据严格标synthetic / fixture validation passed。

## 数据删除与限制

Archive 首页“删除本地档案”：先结束导入、关闭DB、删除nova-archive.db及WAL/SHM和Archive临时文件，不清ChatGPT登录。卸载或Android清除应用数据也删除Archive。Nova“清除登录与网站数据”继续仅管理在线会话。用户自行导出的外部HTML/MD/PDF需自行删除。

schema、解析器、Activity 与导出已实现，Android 验收尚在进行。没有真实官方ZIP或物理设备验证；官方格式未来可能变化，不是对任何版本/大小/附件完整性的保证。
