## Deep Research — Step AE：最终源码Legacy35/36回归通过（2026-10-07）

- 实际生产源码15651dc，Legacy37704403572所有job success。独立下载artifact35=11519530918、36=11519122828均匹配provider ZIP digest，各actual instrumentation 19执行通过+原Gecko2ignore，非将ignore算通过；证据research-1565-legacy-runtime.json。默认交付包DEX仍Legacy=false。
- 此独立阶段不替代正式35/36研究与三格式回归，正式37704403878 API35/36仍运行；API26原生19/JVM101/原签名包前步已通过。下一步核正式新research5与原回归、实际PDF报告首尾/代码/表格与资产像素，然后最终交付。无生产变更/版本/Release/main，真实手机未验证。

## Deep Research — Step AD：最终源码API26原生19项通过（2026-10-07）

- 生产15651dc，正式37704403878 API26 job113076222839 success；独立下载artifact11518279550匹配digest并核actual instrumentation，foundation2+asset7+Reader3+concurrency2+research5=19全部OK。新增第5项补充平面Unicode重导入超限回滚，旧31份报告仍可读，确已运行通过，不是只编译。
- 原4项研究SQLite去重/源ZIP删除重开、schema2迁移、坏重导入保留/数量限额/取消回滚、实际Reader入口/重建/离线亦通过，报告research-1565-api26.json。最终包签名/版本/defaultLegacyfalse与JVM101前步已独立通过。
- API35/36实际三格式与Stable完整回归、Legacy35/36仍运行，最终交付待这些结果；真实手机仍未验证。本步骤仅证据/交接skipci，生产源码保持1565。

## Deep Research — Step AC：最终源码1565包与JVM验收（2026-10-07）

- 生产源码15651dc5d0ea9289128c753900b6f65aef0de847，正式37704403878 build success；Legacy37704403572 build success。最终API26/35/36和Legacy35/36仍运行，新增Unicode原生用例尚待执行完。
- 独立下载正式artifact11518783109匹配ZIP digest，APK2110961bytes/SHA217bf895a5fa285ef15ac38e1df37f99a5091faf6f63f40ece353df928e70923，apksig完整v2/cert原签名、AXML原包/code14/name、DEX static revision1565/defaultLegacyfalse全部核对，无testfixture/.so/签名秘密；证据research-1565-package-verification.json。独立JVM artifact11518114474 XML101/0fail/error/skip。
- 当前生产核心重新ECJ编译后真实仓库外4份报告/97577字符/MD4逐字/HTML再次成功。分别生成4份私有报告供用户直接阅读，真实正文未入Git/CI/log。
- 历史8966正式API35/36被此次源码CI取消，不写成功或失败原因；8966 Legacy35/36全success仅历史状态。下一步只以1565实际native5/三格式和原回归作为最终交付依据，用户手机仍待验。

## Deep Research — Step AB：Unicode持久化预算一致性修正（2026-10-07）

- 当前基点f44d6f3，此前生产8966489的build/API26成功仍有效，但正式35/36与Legacy35/36尚运行；不是最终新源码验收。
- 独立复查发现SQLite LENGTH(message)按Unicode码点，而load按Java UTF-16单元计数，补充平面字符可导致写入通过但读取超限。现upsert逐行使用String.length与load一致，超限仍事务回滚；没有放宽4Mi字符/32报告限制。
- 增加第5个Android研究用例：先持久化31份可读报告，再重导入含大量补充平面字符的新正文，要求A05及旧31份可读正文保持。虚构STORED ZIP避免重复字符触发既有压缩比限制，UTF-8单消息保持1MiB以内；接入API26/35/36必需断言。
- 本地101 JVM、format与diff检查通过；新增native5与最终包完整CI待验证，不交旧8966包作为最终修正交付。下步核此源码签名/包版本、API26/35/36与Legacy回归、实际HTML/MD/PDF，保留真实手机待验边界。无版号/Release/main变更。

## Deep Research — Step AA：API26实物18项与默认DEX开关验收（2026-10-07）

- 源码仍8966489，正式37703447205 API26 job113072536668 success。独立下载artifact11517963703匹配ZIP digest并核原instrumentation：foundation2+asset persistence7+asset reader3+concurrency2+research4=18全部OK。research四个真实方法包括Reader切换/重建/离线、SQLite去重/来源ZIP删除重开、schema2→3、坏重导入保留/超限/取消回滚；不是仅编译。匿名证据research-8966-api26.json。
- success独立解析实际已下载APK的DEX static encoded values：APPLICATION_ID原包、code14/name保持、EXPORT_REVISION=8966、ENABLE_LEGACY_SCANNER=false；补入package-verification报告。不借CI报告猜默认开关。
- 正式API35/36与Legacy35/36仍运行，本步仅API26和包开关验收，不称完整Android/三格式验收成功。下一步核35/36新4项与原回归、实际研究正文PDF首尾/代码/表格及旧Stable实物。仅文档证据skipci，无新APK/生产变更；用户手机仍未验证。

## Deep Research — Step Z：正式包/JVM/真实本地正文独立核对通过，Android运行中（2026-10-07）

- 实际生产源码8966489b87f16d528a2b8a4b1845478594e6a0f9。正式CI37703447205 build/lint/unit/signing success；Legacy37703447262 build/lint/browser success；API26/35/36与Legacy35/36正在运行，不能称完整验收通过。
- success独立下载正式artifact11518951058，ZIP SHA匹配provider digest；APK2110929bytes/SHA d1752009481dff7de7d9dcdc8c8297c00f9f722b9d06c9649dca360904927bb7。apksig独立verify完整签名内容/v2，cert匹配原f93221ee…，AXML package/code14/name保持，DEX revision8966/ArchiveResearch及artifact标识、无testfixtures/native.so/签名秘密核对。报告research-8966-package-verification.json；目前只包验证，不等于Android恢复或默认flag测试通过。
- success独立下载CI JVM XML artifact11518064436：101 tests/fail0/error0/skip0。再次本地当前production parser/renderer执行真实仓库外输入：122 imported conversation、inventory19report中4条精确thread匹配，另外15条外层thread不在当前会话输入，不猜归属；4complete/1conversation/97577bodychars/MD4逐字/HTML成功，research-restoration-real-local.json。不称19条均恢复。
- 本检查点仅证据/交接及恢复jvm-tests.sh原有executable mode，不改production/版号、不公开Release，skip ci。8966误将原脚本100755写成100644，本步恢复；本地bash执行仍通过，原包代码不受影响。
- 下一步只核同8966 Android结果，读取actual native4、原schema1/2/资产并发/原回归、实际HTML/MD/PDF与默认Legacy关闭；实际失败留证并据具体原因修正，不跳过/放宽断言。真实手机仍待稳定包覆盖安装+手动重导入+报告人工核对。

## Deep Research — Step Y：恢复实现与本地验收，Android待验（2026-10-07）

- 基点55a34ce；本步骤将实际源码提交至feature/export-conversation，不改包名/签名/code14、不main/公开Release。实现ArchiveResearch标准inventory→exact thread/file→version1 completed report_message，只取明确最终正文，无activity/thoughts/在线访问；不猜两段聊天之间的插入位置。
- schema3非破坏reports表与1/2→3迁移，导入事务去重/保留旧已完成正文/限额/取消；Reader独立“研究报告”入口与HTML/MD/SystemPrint同离线正文模型。重复导入缺失/损坏/未完成不覆盖旧完成正文；升级需用户手动重导入，不自行重读旧ZIP。详docs/deep-research-restoration.md。
- success本地101 JVM（原93+新8）、脚本语法/diff检查；真实仓库外当前parser/renderer 4reports complete、Markdown4/4逐字、97577chars、HTML成功生成并仅在私有目录保存。新增native4/API26/35/36及原实际HTML/MD/PDF研究首尾/代码/表格断言已接入，尚未运行完，不称Android成功/手机恢复。
- 初次测试2failure（坏report JSON未转为明确不可用；空ZIP取消漏检）已修正，101全部通过，历史失败保留。初次工具ECJ classpath错误属此前V，未改写。
- 下一步核当前源码CI build/lint/JVM、API26/35/36原回归和新native4、实际三格式、原签名与DEX默认开关。只有完成实际验收才交同签名稳定测试APK；修复CI不改版号、不公开Release。真实标题/标识/正文/附件/路径不进Git/CI/log。

## Deep Research — Step X：独立报告源与精确会话归属已确认（2026-10-07）

- 用户提供研究范围更新前后的两段原文，明确“查看全部分支”仍无报告。已在仓库外唯一指定会话定位两段原文；它们是研究任务启动/更新确认，不是研究报告正文。前一步分支差异真实但不能解释此缺失。
- 生产源码仍787f18f，文档基点1328309。success独立发现标准library_files.json（1969888bytes，635条inventory），19条library_artifact_type=deep_research_report。指定会话有4条，origination_thread_id精确匹配official conversation id，file_id为canonical file_32hex且对应唯一.dat存在。报告是application/json、sediment backing，而非PNG/Office或聊天mapping节点。
- success四个JSON version1均widget_state.status=completed，widget_state.report_message为assistant/text、metadata.is_complete=true、parts单字符串，正文20797/24339/25569/26872字符。report_message.id及inventory.origination_message_id均不在该聊天mapping，backing_conversation_id是独立研究会话而非外层thread；不能猜具体聊天插入位置。activity_messages包含思考与搜索进度，只恢复明确report_message正文，不显示thoughts或任意widget metadata。
- 实施条件具备：标准inventory artifact类型+origination_thread_id→同会话独立研究报告，file_id→唯一.dat→严格version1完成报告结构；无名称/顺序/正文相似度归属。设计将研究报告单独列为“研究报告”区/入口并支持同模型HTML/Markdown/SystemPrint；不伪造它们在用户给定两段之间的精确位置。
- 下一步最小有界本地解析、非破坏持久化与Reader/三格式接入；missing/malformed/ambiguous/pending标记不支持或不可用，重复导入保持已恢复内容，取消/失败事务回滚。补全虚构fixtures与JVM/Android验证，保留签名/包名/code14，CI不公开Release。真实title/ID/正文/文件/路径只在repo外，入库仅schema/匿名计数。

## Deep Research — Step W：用户指定会话精确定位与分支核对（2026-10-07）

- 用户提供一份缺失正文所在的会话标题；在仓库外ZIP中发现唯一精确标题匹配。原始title/ID/内容不写Git，相关近似标题未合并。
- 分支feature/export-conversation；本步骤文档基点3f0e0bb，生产源码仍787f18f。本会话独立实际Java importer(false单会话JSON)/Tree/Display/Renderer核对：606mapping节点、605messages，current_node链56节点含55messages；“全部分支”605messages。19条>4000字符assistant text正文中1条在主链、18条在其他分支；5条>8000正文全在其他分支。19/19在实际全部分支Markdown逐字完整匹配；正文总169289chars（仅这19条），当前/全部分支HTML均成功生成。
- 证据tools/archive/evidence/research-body-target-branch-audit.json。仅匿名计数入Git；仓库外生成private target-current.html/target-all.html/target-all.md供本用户核对，不上传真实输出/脚本输入，不把target全部分支串接视为一份报告。未修改production或AndroidDB，也未声称具体报告识别、真实手机恢复或完整历史。
- 当前已证实默认主链会排除此会话18条长正文，而已有“查看全部分支”可选择这些节点；尚不能仅凭长度将某篇认定为Deep Research。下一步用户在现有Reader查看全部分支，或核对本地全部分支HTML，确认缺失报告是否出现；若仍缺失，请定位报告小标题/日期再查对应内容表示，不猜归属或显示thoughts。
- 无新APK/CI/版号/Release/main变更。真实内容隐私、Stable/Legacy与既有证据边界保持。

## Deep Research — Step V：实际Java正文保留检查完成，待具体报告定位（2026-10-07）

- 用户目标仍是完整研究报告正文。文档基点56d9c47；实际生产源码仍787f18f；本步骤只加显式本地工具/脱敏报告，无生产代码、APK、版号或CI变更。
- success 本会话独立ECJ编译当前ArchiveImporter/Model/Tree/Display/Renderer等，Java -Xmx128m读取用户15卷合并副本。122会话/7250节点/7130消息/1874隐藏thoughts。184条纯字符串parts、长度>4000的assistant text正文：184/184模型与Display逐字相同、184/184在全部分支Markdown中完整匹配；118在默认主链、66在其他分支。122个全部分支Reader HTML均成功生成。这不是特定Deep Research识别、Android SQLite/真机阅读或HTML视觉全文验收。
- 新工具tools/archive/ArchiveBodyAudit.java与body-audit.sh只显式本地输入、输出固定字段计数/错误码、不保存生成正文。报告tools/archive/evidence/research-body-local-audit.json。初次工具编译因ECJ不支持classpath通配符并缺StorageBudget源失败，已修正依赖和源清单后实际运行通过；生产代码未改。
- 根据本次证据不能把普通长正文统一归因于导入/Display/MD截断，也不能认定66条分支正文就是缺失报告；Reader已有“查看全部分支”。特定报告仍可能不在此导出、在其他分支或其他表示中，须定位后判断。
- 下一步依赖用户给出一份缺失报告的会话标题或主题+日期（已询问待答）。收到后只在仓库外做精确定位，核正文与current_node/显示/DB关系再实施最小修复；不猜归属、不展开thoughts、不拼多分支、不扩在线数据源。保留全部历史边界和失败。

## Deep Research — Step U：确认缺失报告正文并重新取得输入（2026-10-07）

- 用户在本会话明确：缺失的是 **完整研究报告正文**，不是生成 PDF/Word 下载文件。此前 sandbox 文件缺口不能代替本任务定位。
- 工作分支 feature/export-conversation；开始源码/文档基点6d035f391e8ea3062b26f6065a0a2468a289477f；已测生产源码仍787f18f。本步骤只读调查，无生产变更、新APK或发布。
- 用户提供一个Drive文件夹，连接器成功列出连续15分卷并原样下载；第15卷首次连接器内部错误，单独重取成功。合并371257079bytes，607 ZIP entries，12 JSON entries；只在仓库外私有目录保存输入。无需用户复制15链接，文件夹/文件链接与标识不写入公开仓库。
- 本会话独立Python只读结构审查：2个会话数组共122会话；7250 mapping节点，文本3216/multimodal892/thoughts1874/recap1148/无message120，和此前统计一致。184条assistant纯文本parts长于4000字符，其中118条在current_node主链、66条在其他分支；134条有content_references。这只是长文本统计，不能据此认定具体Deep Research报告或宣称完整恢复。未观察到已知research/async-task字段亦不能证明报告不存在。
- 当前代码独立复查：ArchiveModel仅按content_type=thoughts隐藏；ArchiveTree默认跟current_node的parent链；Reader已有“查看全部分支”。不得自动展示thoughts或把不同分支拼成一份完整报告。
- 下一步：已询问一份缺失报告的会话标题或主题+日期，答复待到达；并行核对现有有界Java parser/display/renderer对普通长正文的保留。只有定位到用户所指正文和具体丢失环节才实施最小修复。真实正文/title/ID/路径/下载链接/二进制不进Git/CI/log。

## Deep Research — Step T：澄清反馈并确认生成文件恢复缺口（2026-10-07）

- 用户澄清“重装后看不到”指的是 **Deep Research 生成的文档**，不能据此记录成所有普通上传附件恢复失败。当前待明确：缺失完整研究报告正文，还是报告中的 PDF／Word 下载文件；已提出单项澄清，尚未回答。新的工作范围是调查该缺失，不再把真实 AR3–5 清单当成唯一下一步。
- 分支 feature/export-conversation；开始前 fetch / ls-remote 核对默认远端 HEAD main/e8ffa0c6bd9af9ab29aecad50efe2ab6b8099a00，工作分支 HEAD 2a86746d36d87cc33cbbeed8628e29021b7bcae1 与远端一致、工作树干净。实际生产源码仍 787f18f5296e7bbf4148146c21c9c7656e8c7859。最新正式 37677076099 与 Legacy 37677076223 均 completed/success；本步仅调查和文档，无新 APK 或生产修复。
- success 源码确认：ArchiveDisplay 目前只解释 parts.asset_pointer / metadata.attachments[].id；ArchiveAssetStore 以这些引用规划恢复。ArchiveRenderer Markdown 链接只允许 HTTP(S)，sandbox 下载链接被呈现成普通标签，尚无生成文件恢复绑定。这是明确的功能覆盖缺口，但未证明就是用户所指 Deep Research 文档的具体原因。
- success 授权真实副本在仓库外按现有有界 parser 只读统计：现有普通附件引用全部属于 user；包含 sandbox: 的消息 15 条，当前主链可见 9 条。未找到已知 research 作者／模型标记不能证明导出没有研究报告。正文可能是普通 text，也可能位于其他分支；不自动展示隐藏 thoughts 或把任意 widget metadata 当正文。
- 仓库外 ZIP 审查发现 filename-map 未覆盖的 dat 有 266 个，前缀分类 PNG47/JPEG128/ZIP-container52/PDF7/other32；不能从前缀推断用途或把 ZIP-container 等同 Word。metadata.content_references 的 type=file 有 733 个 canonical 引用／80 个唯一文件条目，其中 730 次存在、3 次缺失；6 个唯一条目不在 filename-map。它们可能是引用来源文件，未证明与生成文件下载链接的绑定；不按名称、正文、ZIP 顺序或相似度猜归属，不宣称二进制不存在。
- privacy：真实聊天正文、名称、ID、路径、链接、二进制不进入 Git/CI/log；仅以上计数与固定 schema 字段记录。not verified：具体 Deep Research 文档、报告正文与生成文件的对应关系、真实手机阅读／重复导入等。没有 backend／网络抓取／Fiber／选择器扩张，Stable/Legacy 未改。
- 下一步：收到“报告正文／生成 PDF-Word”澄清后继续针对性调查；只有找到可靠官方数据归属才能实现本地恢复，否则明确标记不支持或源文件缺失。若改生产源码，补合成回归与 Android 实际 Reader/导出验证；保留此前验收和历史失败，不复用普通附件成功替代研究文档验证。

## Attachment Restoration — Step S：最终fixture/实际输出/原签名APK验收，真实手机AR3–5待验收（2026-10-07）

- 分支feature/export-conversation；完整已测源码**787f18f5296e7bbf4148146c21c9c7656e8c7859**，上一文档检查点62bc70c，本提交仅最终文档/证据，生产源码未再修改。正式[37677076099](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37677076099)与Legacy[37677076223](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37677076223)全部job success、head绑定787。success独立JVM93 XML0fail/error/skip；API26原生14（2+7+3+2），35/36各Archive34（7+3+20+concurrency2+true force-stop2）及Stable43；Legacy各19+历史Gecko2ignore。
- success独立35/36实际HTML/Markdown/PRINT source一致的scope/order/对象text/recap/hidden thoughts/另一分支排除、附件PNG portable解码；实际Archive PDF16页、12×8图的真实RGB24/108/150（不是只数image objects）、DOCX卡片与首尾/长代码表格Unicode。Stable/System及Firefox各31页PDF/320×120图、同static source/首snapshot marker必要OCR/晚变更排除/原签名升级核对。artifact35=11507622918、36=11508877009；installed package SHA同独立验证APK。evidence attachment-787f-runtime-35/36.json。35/36本次均无超时线程文件；两concurrency新用例实际结束且assertions通过。
- success正式[签名APK Artifact11507044105](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37677076099/artifacts/11507044105)，2106973bytes，SHA256726624300448bc653163ed095eab1f5364e51f60bcfcf032efdbdf23c1282a38；v2 RSA/content digest/原cert/source/默认legacyfalse、旧assets与freeze一致、无.so/无test-only diagnostics。versionCode14/versionName1.4.0-scroll-trial/package/签名保持。显式实验APKflagtrue独立通过，仅用于fixture，不作为默认包交付。没有公开Release/merge/forcepush。
- success授权real compatible repack的host追加完整codec检查230images（PNG82/JPEG148）全部decode0失败，无输出真实像素/内容/名称/ID/path/hash；attachment-real-image-codec-audit.json。此前326必要binary/154521964bytes/CRC/hash/magic/临时copy rollback0残留已通过。最新parser/tree122/7250/7130/4267/3065可见/fallback0与既有规模一致；都不是真实AndroidDB或人工内容验收，实际样本为手机重打包副本，不称原始ZIP字节完全相同。
- **AR1 synthetic / fixture validation passed；AR2 real compatible repack容器/map/host校验passed；AR3 real Android binary import pending；AR4 real duplicate pending；AR5≥5会话各类型/attachment-only/text+attachments/正确归属/airplane/restart/source ZIP失效/三格式人工pending。** 用户此前会话schema1 Level2–4成功报告独立保留；不替代新附件AR3–5，不宣称Attachment restoration manually validated。
- 37项完整[报告](../archive-attachment-validation.md)、[手机清单](../archive-local-validation.md)、nova-archive/attachment-restoration/README/Stable docs同步，历史schema1与失败证据不删。用户覆盖安装保留数据库，升级不自动读取旧ZIP，需要手动重新导入一次补回附件；保留唯一官方备份，原ZIP/repack初始空间规划约661/662MiB仍需余量，只反馈安全计数/固定码/pass-fail。
- 已知限制：OpenAI格式可变化，container512/所选JSON单64总256MiB、asset单64/new总256MiB、256/convo/2048/import、图片16Mpixel等保持；portableHTML单图2/总6MiB、MD描述无binary、Print不拼文档页、LaTeX源、分页200/title-only无FTS。总300s协作deadline不保证阻塞SAF I/O或monitor等候瞬时中断；采样heap/WAL非精确peak。外部PDF/Office真实app阅读待用户验证。3c两次API36stall根因未最终捕获，consistent lock-order修正独立风险且测试pass，不能改写历史。
- 下一步**用户手机真实AR3–5验收**，收到脱敏统计/固定码后按具体失败修复；当前已授权实现、fixture回归、签名包与文档交付完成。不主动加FTS/citation/Cloud/DOM/backend/selector，不删除或增强Legacy；目前不需要额外sourceSet复杂化。

## Attachment Restoration — Step R：新锁顺序API26、Legacy与签名独立通过（2026-10-07）

- 分支feature/export-conversation；实际源码787f18f5296e7bbf4148146c21c9c7656e8c7859。正式37677076099 build/lint/JVM93、API26success；35/36仍running，不能称整个最新验收完成。本步为已完成的独立证据检查点，正式Android输出继续待核。
- success独立实际XML93（fail/error/skip0）；API26 artifact11507865359：foundation2+persistence7+Reader3+new concurrency2=14，包含打开helper锁序及close等锁两原断言，非仅编译。attachment-787f-foundation.json。
- success同源码Legacy37677076223全部job，独立35/36 instrumentation各19pass+原Gecko-only2ignore，attachment-787f-legacy-runtime.json。普通签名APK artifact11507044105/2106973bytes/v2 signature+content digest+原cert/package/code14/revision787/defaultflagfalse，原5assets一致/无.so/无test diagnostics；独立实验APK11507605371同source/原签名/flagtrue与assets也确认，不把实验包交作正式默认包。attachment-787f-package-verification及legacy-package.json。
- success最新纯parser/tree源码重新编译并在repo外授权真实compatible repack解析-Xmx128m：122conv/7250mapping/7130message/4267current-branch、3065displayable（1202thoughts隐藏）、fallback0；all-node text3216/multimodal892/thoughts1874/recap1148。与既有真实兼容计数一致，不是Android SQLite恢复；只aggregate证据attachment-current-content-counts.json，无真实data/ID/name/path/hash。
- AR1最新API26/Legacypassed而完整35/36待核；AR2真实repack容器/map/326copy-rollback通过；AR3真实Android恢复、AR4真实duplicate、AR5人工含offline/restart/source loss/三格式pending。清单纠正AR4语义；空间原ZIP约661MiB/repack约662MiB初始规划，非充分保证。
- 旧3c两次API36stall原因仍unproven。新consistent LOCK→helper修正的是可见锁序风险；Android16公开实现还有条件性的同DB名shared opening lock，未检查hidden/runtime flag或将旧失败归因。下一步只完成现有正式35/36、actual export/图片像素/installed APK匹配与37项最终报告；不再增加scope/版号或发布Release，不删除Legacy。

## Attachment Restoration — Step Q：诊断基线实物通过，修正可见的数据库锁顺序风险（2026-10-07）

- 分支feature/export-conversation，已测基线ad30d7f3301ddab63120791d4519de5cbc22e25e（生产行为仍3c6c0f7）。正式37672756518全success；独立artifact核对JVM93、API26 2+7+3、35/36各Archive32+Stable43、实际16页Archive PDF/12×8图片与portableHTML/MD/描述、31页Stable/System及Firefox PDF/Unicode/code/table/升级；v2签名/content digest/原证书/defaultlegacyfalse/source/package/code14通过。证据attachment-ad30-foundation/runtime-35/runtime-36/package-verification.json。35/36没有生成超时线程文件，本次绿色不能证明此前两次阻塞的根因或修复。
- 源码静态确认独立风险：importFile先LOCK→SQLiteOpenHelper monitor；onOpen在helper monitor中取LOCK。统一getReadableDatabase/getWritableDatabase/close为LOCK→super helper monitor，保持reentrant recovery/transaction/文件一致性锁，不删除防错或加timeout。close也不得在导入持有LOCK期间关闭同helper。没有把此风险未经证据认定为此前API36根因。
- 新native ArchiveStoreConcurrencyTest两项：public ContextWrapper数据库路径/打开钩子协调同helper reader与LOCK owner，有限latch/join验证reader不能先占helper反向锁；close必须等一致性锁释放。无hidden fields、真实数据或改旧断言。API26与35/36均接入；新完整native CI仍pending。
- success本地JVM93、ECJ Android API编译Store/新Rule/concurrency类，脚本语法/diff；不是本地emulator实际测试。Stable生产/Legacy源码与fixtures保持，签名包名版号未改。AR1此前ad30 synthetic已通过，新锁顺序源码待完整CI；AR2真实repack326有界copy/rollback通过；真实AR3/4/5 pending。
- 下一步核对新源码CI/原断言/锁顺序两项/实际三格式/签名与安装产物；最终37项报告和phone清单。保留3c两次失败，不反复重跑掩盖、不承诺任意官方数据或外部Office真实成功。

## Attachment Restoration — Step P：API36第二次阻塞，增加合成线程证据采集（2026-10-07）

- 分支feature/export-conversation，生产源码仍3c6c0f78，文档基点df0ccf7。failed同源码重试API36 job112955254761/artifact11504958263，37665103092 attempt2 overall failure。新asset7/Reader3仍全部pass；原Archive20前10方法完成，事务取消测试started后无finished/断言，host900s timeout；与第一次malformed JSON方法阻塞位置不同，不能宣称环境偶发或已修复。证据attachment-3c6c-api36-attempt2-failure.json。
- 下一源码检查点只新增test-only ArchiveThreadEvidence Rule：每个原ArchiveTest超过90s时记录稳定phase/done+numeric thread identity/state/有界frame，无thread name或参数/正文/ID，128threads×48frames、2MiB文件预算；完成即interrupt watchdog，不改变test verdict/原断言/900s timeout，不进入生产APK。条件失败也采集。只synthetic instrumentation使用。
- host suite若TimeoutExpired，保存原partial stdout到对应instrumentation文件；finally拉取synthetic thread evidence，避免只有900s文字无法定位。不存在真实ZIP/用户数据进入CI。
- success本地ECJ编译新增Rule、脚本语法/diff；生产源码未变，不重复本地93；新CI在原fixture上捕获阻塞与位置后再修根因。当前new partial fullTest依赖FixtureActivity由Gradle编译，不能称已运行。旧3c API35实物/签名及ea26全success保留，但不替代两次36失败。
- AR1全套当前不能验收，AR2真实repack326copy/rollback passed；真实AR3–5 pending。暂停最终APK验收称谓，继续定位数据库/列表/导入锁与线程关系，不放宽安全边界或跳过malformed/cancel/print测试，不加FTS/DOM/backend，不递增版号/发布Release。

## Attachment Restoration — Step O：新provider API35通过，API36旧JSON测试阻塞失败留证（2026-10-07）

- 分支feature/export-conversation，测试源码3c6c0f78baac317f344f73e088bc48595c348221。success build/lint/JVM93/API26 2+7+3；最新API26独立actual Reader ACTION_VIEW无query/original name、READ-only/ClipData与provider name/size/MIME通过；API35各Archive32+Stable43、实际16/31页/图片/HTMLMD/Unicode/升级独立通过，attachment-3c6c-runtime-35.json。
- failed API36 job112943290944，artifact11503043598，正式37665103092整体failure。独立artifact/logcat：asset persistence7与Reader3（含新provider Intent）全部pass；原safJsonAndMalformedFilesHaveFixedErrors等待“JSON fixed error”60秒timeout，after等待import worker cleanup再timeout；后actual print test开始后无结束，host整个ArchiveTest900秒timeout，没有PDF/之后Stable实物，不能验收。
- **根因未知**，不能擅自归于emulator或JSON/parser/文件一致性锁。该旧方法/Task/importer在本次provider修正未改，API35及此前ea26 API36曾通过；下一步只重跑同源码失败API36一次检查重复性，原failure永久保留。若再次出现，抓worker/SQLite/Binder线程状态再修，不增timeout、不跳过malformed/cancel/print断言。
- latest APK artifact11503035582/2106829bytes签名/content digest/cert/flagfalse/source3c及包版本14核对通过，尚非最终完整CI验收；没有发布Release/递增版号。真实AR3/4/5仍pending。37项报告草稿/本地清单已备，待完整CI后入库与交付。

## Attachment Restoration — Step N：全套基线实物通过，修正外部URI隐私边界（2026-10-07）

- 分支feature/export-conversation，已完整验证源码ea26e180d6f466d9f283123b3ec7c5db93a38e33；文档基点422fecd。本提交为新provider修正源码，以实际SHA区分，不把旧实物冒充新源码。
- success正式CI37661640199 build/lint/unit/API26/35/36、Legacy37661640158各job。独立下载JVM93 XML零fail/error/skip；API26 foundation2+persistence7+Reader3；35/36各Archive32（7+3+20+force-stop2）和Stable43，实际会话PDF16页含12x8图片/文档描述，HTML实际PNG decode12x8与无private链接/MD描述/source PRINT image模型；Stable两PDF31页/320x120图、首尾/代码表格Unicode/后续变更排除、Firefox同source及独立OCR均通过。证据attachment-runtime-35/36.json；Legacy各19及原2ignore保留，attachment-ea26-legacy-runtime.json。
- success ea26 APK2106741bytes，artifact11500822418，v2签名/content digest/原cert/sha文件、package/version14、defaultlegacyfalse/revision、5旧asset源码一致、无native libs或test fixtures，attachment-ea26-package-verification.json。没有发布Release，尚未把旧包作为最终新provider修复APK交付。
- 最后隐私复查：生产getUriForFile第四参会把original name放入URI query，可能进入系统Intent日志。改生产URI只含随机内部文件名；provider query通过私有DB返回原display name、super query支持projection/size并保持路径检查，getType仍magic metadata。新native断言包括真实Reader openAsset发出的ACTION_VIEW URI无query/原name、detected MIME/READ grant/无WRITE/ClipData，监视器阻止实际外部应用启动；还核content query的name/size。这验证Intent管线，不冒称第三方Office成功。
- A06文案改“写入失败，请重新核对已有档案”，避免事务结束/诊断后写入异常结果不确定时宣称必已回滚；文件仍按实际DB恢复，逻辑不变。success本地JVM93、ECJ provider/Reader/native test编译/diff；新源码Android完整CI与原签名APK仍待重跑核对。
- 清单更新初始空间：本样本ZIP+147.36MiB附件+DB/WAL128MiB+32MiB余量约661MiB，实际仍需更多余量且运行时分阶段检查；不能把SAF预算513.4MiB当完整恢复保证。
- AR2 real compatible repack结构+host326copy/rollback passed；AR1完整synthetic属于ea26基线，新隐私修正待验证；真实AR3/4/5仍pending。下一步新CI/实际provider与三格式/签名核对，最终文档与APK交付；不加FTS/DOM/backend/Cloud，不改版号签名，不删除Legacy。

## Attachment Restoration — Step M：真实binary有界复制与回滚独立通过（2026-10-07）

- 分支feature/export-conversation，测试源码ea26e180d6f466d9f283123b3ec7c5db93a38e33；本步仅host审计/工具/脱敏证据，非Android SQLite验收。success JVM -Xmx128m 实際ArchiveDisplay引用集合→ArchiveAssetFiles.Batch preflight→326必要entry stream copy/CRC/SHA/magic→private随机文件→未commit close rollback；326/326complete，154521964bytes，2495ms，82PNG/148JPEG/5PDF/82DOCX/5XLSX/4unknown，回滚文件0/pending0。只计数/固定类型，所有真实binary输出仅repo外并已回滚。
- 提交通用ArchiveBinaryAudit.java（仅本地显式输入、自动临时目录、脱敏统计、无私人身份/路径/内容输出）与attachment-real-bounded-copy.json；未将真实ZIP/图片/文档加入GitHub/CI。host复制耗时不是手机导入duration；128MiB heap cap不是已测Android peak。
- fd3f4fc Legacy37660442048已workflow success；正式37660442058尚进行中，ea26新正式37661640199 pending/Legacy37661640158运行，未手动取消，不提前报全套成功。
- AR2增加真实compatible repack binary复制/回滚证据；AR1新Android end-to-end/三格式待CI，AR3/4/5真实手机仍pending。下一步完成新源码CI和产物独立核对、同签名APK交付、用户本地人工验收。

## Attachment Restoration — Step L：五类型与进程重启回归补齐（2026-10-07）

- 分支feature/export-conversation，实施基线fd3f4fc4bdace96e675b25dd6839b12beb51313e；本提交实际SHA由git log/新CI确认。success本地JVM93、ECJ Android API编译新的fixture/persistence/store、脚本语法与diff检查；本地完整ProcessTest依赖既有FixtureActivity由Gradle CI编译，不冒称本地执行。
- native persistence5→7：新增PNG/JPEG/DOCX/PDF/XLSX实际私有存储、原ZIP删除、数据库重开、FileProvider detected MIME/原bytes、portable PNG+JPEG；PDF/OOXML夹具只检测/存储/分享，非第三方阅读应用有效文档验收。另加IHDR CRC坏输入被native bounds拒绝时保留old verified file，修复第二验证阶段此前会把旧资产改damaged的边界；stream detector、outer CRC和native阶段均保持。
- shell force-stop重启原100会话测试保留，加独立第101会话/五种私有asset；删除两个input文件后新进程确认5binary available、JPEG portable与XLSX描述。不是仅Activity recreation。
- fd3f4fc build与API26 foundation2/persistence5/Reader3已独立下载核对success；APK v2签名/content digest/原证书、包名/code14/name、legacyfalse/revision核对通过（artifact11500920427）。API35/36与Legacy runtime仍进行中，不预记通过，也未手工取消。新提交仍需全套CI实际执行/输出。
- 本地验收清单更新：明确15分卷授权结构审计和原ZIP差别，历史1990 schema1 APK不用于附件验收；真实AR3–5检查≥5会话、各附件类型、duplicate/force-stop/撤销来源授权/移动测试ZIP副本/飞行模式/实际导出/删除。仅安全计数与固定码反馈；真实corpus/输出不入GitHub/CI。
- AR2 compatible repack结构passed；AR1端到端尚待新CI、AR3/4/5真实Android与人工pending。下一步等全套实物验证和稳定同签名APK，再由用户本地反馈，不改Stable/Legacy/签名/版号，不发布Release。

## Attachment Restoration — Step K：offline Reader/provider/三格式接通（2026-10-07）

- 分支feature/export-conversation，实施基线962df47；本提交为完整功能接通源码检查点。success本地JVM93（原48+files25/map7/display8/renderer5）、ECJ Android API编译core/reader/provider/新增native tests、host脚本/XML/diff；只有stub BuildConfig用于本地编译，不冒充Gradle或实际Android。
- Reader/PRINT共用ordered Display，真实PNG/JPEG经精确app-local origin的UUID白名单流读取，offline仍blockNetworkLoads/JS/file/content关闭，无Cookie/bridge/network。文档cards经独立exportedfalse FileProvider，仅filesDir/nova-archive-assets，detected MIME由私有DB给出，content URI临时READ grant/ClipData、无app明确提示。HTML portable单文件仅bounded PNG/JPEG data URI（single2MiB/total6MiB，超限descriptor），HTML整体16Mi chars/MD2Mi；documents descriptor不输出private路径/identity/hash/无效链接。MD明确不携binary；PRINT同message模型+本地真实images，不合并附件PDF页。
- 新3项native Reader/provider/recreate tests加入API26/35/36，原actual SAF HTML/MD与SystemPrint PDF test改用虚构PNG/DOCX ZIP，保留object text/recap/hidden thoughts/branch断言；保存真实PRINT-mode source，host必须pdfimages发现image，并核document descriptor。旧snapshot全回归/Legacy保留，共享Snapshot仅offline callback增加精确local resource接口，Stable路径行为不改。
- failed962df47 CI37657777698/API35：新asset persistence5全部passed，原Archive20中transaction cancel1 timeout，19passed；观察者新开Store被合法onOpen/import lock阻塞，直到transaction结束。改安全Control.phase在worker beginTransaction后database，原cancel/A07/rollback断言保持，不删文件一致性锁；证据attachment-schema2-cancellation-fixture-failure.json保留。API26 source962 foundation2+asset5已job success；Legacy37657777774 success；新reader/实际images尚未验收。
- 额外完整性：duplicate binary比较source SHA256+CRC/size且old private hash确认，不仅凭CRC跳过；endTransaction不确定异常保留新文件至DB-grounded recovery，避免误删可能已commit的文件。
- 尚未AR1/3/4/5验收：新完整CI/actualHTMLMDPDF/Stable/Legacy待运行，用户真实DB/离线打开/重启/duplicate/人工内容仍pending。AR2仅真实重打包container/map/refs/binary审计passed。下一步新CI实际产物与签名实核，提供稳定APK/本地清单，用户手机真实AR3–5；不上传私有corpus/outputs，不merge/Release/版号签名变更。

## Attachment Restoration — Step J：schema2与私有binary恢复接入（2026-10-07）

- 分支feature/export-conversation，实施基线657b436；本提交为源码检查点，CI revision以实际SHA为准。success本地ECJ Android API编译core+新增test、JVM88、python脚本语法/diff检查；**Android实际执行未验证**，不能称AR1/3完成。
- SQLite升级1→2非破坏新增assets/message_assets，旧conversation/message/source表保留；官方稳定身份唯一、generated private path/hash/size/magic+declared MIME/state/first-latest source、节点ordinal/kind/raw ref。SAF仍私有cache，先streaming plan+exact basename唯一解析（wrapper目录可支持，歧义拒绝），空间规划仅新候选，DB事务内copy/CRC/MIME/PNG-JPEG bounds，结束事务后committed，再清理无DB引用文件。旧complete经size/hash校验可复用，missing/damaged候选不覆盖已验证旧binary。
- 原conversation upsert不变；相同RAW再导入也重建asset refs，支持从schema1 metadata档案恢复；prepared plan不保留所有正文。diagnostic增安全asset计数/bytes/mime mismatch，schema2。Archive-only删除同时private assets/pending，onOpen持锁清理死进程orphan/pending，保留DB complete路径，不碰Cookie/外部SAF。
- 新5项Android synthetic persistence tests：files+refs/reimport/close-reopen+ZIP删除，malformed/cancel保留旧档案，Archive删除隔离，schema1迁移保留会话并恢复assets，死进程文件清理。夹具只有虚构PNG/DOCX/runtime ZIP；API26/35/36 CI明确执行新5项，旧Archive20+restart/Stable/Legacy保留。
- Reader/provider/HTMLMDPDF尚未接assets；真实ZIP只做过结构/CRC/parser审计，未实际Nova DB导入。AR2 repack结构passed，AR1端到端/AR3/4/5待验收。下一步核本源码CI并接reader/export安全边界；不删除Legacy，不改Stable/签名/版号，不发布Release。

## Attachment Restoration — Step I：精确引用与shared Display解析（2026-10-07）

- 分支feature/export-conversation，实施基线66a62c6；本提交为源码检查点。success本地JVM88=原48/file25/map7/display8（128MiB heap），仅新造synthetic values，无私人正文/ID/filename/bytes进fixtures。
- 新ArchiveDisplay按已实证sediment://file_<32hex>/metadata id解析稳定身份与.dat entry，保留parts text/image顺序及Map.text；同消息parts与metadata重复引用去重，不按文字或显示文件名合并；attachment-only为“附件消息”，metadata-only附件在正文后；unknown pointer不解码/截query/猜转换，thoughts可持久refs但visible隐藏，recap保留。
- single asset32→64MiB（实证63.62MiB PDF），total256MiB/JSON与其余pixel/count limits不改。源码Display尚未接Reader/Store，schema1现状保持，AR1/3–5仍pending。下一步schema2/filesystem事务/upsert/Reader/provider/exports；引用缺口已解决，不再询问用户。完整新CI等待触发/核对，不以本地88替代Android。

## Attachment Restoration — Step H：15分卷成功读取，真实结构缺口已解决（2026-10-07）

- 分支feature/export-conversation，当前测试源码9ab3f72585e78e064468917636b75797c0f50861；本步仅本地审计/脱敏证据。用户主动提供15个Drive分卷链接，均≤24MiB、001–015连续；下载并在repo外私有目录按字节合并，371257079bytes的未加密普通non-ZIP64 ZIP。此为手机重打包官方导出内容，607entries含2directories，不宣称与原370566688bytes ZIP逐字节一致。
- success Nova有界parser -Xmx128m 实际解析122conversations/7250mapping/7130messages/map326，selectedJSON58132014bytes，与先前用户统计一致。公开结构：parts.asset_pointer=sediment://file_<32hex>；metadata.attachments[].id=file_<32hex>；map key/exported basename=file_<32hex>.dat。265 pointers与376 metadata refs均精确命中；265 metadata与parts重复，需同消息去重；unique326、每会话最大82，全部326 mapped entries存在且无歧义。
- success mapped binary CRC326/failed0，154521964bytes；magic DOCX82/PNG82/JPEG148/XLSX5/PDF5/unknown4，PNG/JPEG230 header均通过、最大2359296pixels，16M预算足够。原文件名扩展名统计不等于检测类型，保持magic优先。单个PDF66707137bytes要求single asset32→64MiB，total256MiB/JSON预算不扩大；unreferenced dat不提取（全ZIP592dat）。
- AR2真实compatible repack的container/map/refs/binary结构检查通过；AR1仍未接端到端，AR3/4/5未验收。不提交或输出真实内容/文件名/ID/link/path/hash，不把real binary用于synthetic fixtures/CI。
- **不再需要用户手工提供引用结构**。下一步立即按证据接schema2 assets/refs、精确entry lookup、private恢复与重复导入、shared display/offlineReader/provider/HTMLMDPDF/删除；single64MiB的实证调整与fixtures要一起验证。新完整CI+实际输出核对，之后提供APK让用户本地完成AR3–5。原始frozen/legacy/签名/版号保持。证据tools/archive/evidence/attachment-repacked-container-audit.json。

## Attachment Restoration — Step G：Drive ZIP元数据可读，整包下载受限（2026-10-07）

- 分支feature/export-conversation，测试源码9ab3f72585e78e064468917636b75797c0f50861，本提交仅交接/脱敏证据。用户最新明确授权读取其提供的Drive真实ZIP链接；这更新此前“真实ZIP仅用户本地”的访问范围，不授权向GitHub/CI上传私有数据。Drive链接/ID/原文件名/私有hash未入库。
- success Drive metadata：application/zip、370566688bytes，与用户样本大小一致。failed raw fetch：connector HTTP413，最大268435456bytes（256MiB）；没有下载ZIP或解析其消息。只读direct访问检查返回登录HTML，不是ZIP；未改共享权限、未读取/提取账号凭据或Cookie，不要求公开分享。证据tools/archive/evidence/attachment-drive-readability.json。
- success新[正式CI37645986505](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37645986505)与[Legacy37645986535](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37645986535)均completed/success；本步核workflow状态，尚未独立下载新runtime产物/实际输出，不以状态替代实物核对。
- 实现边界不变：map326与单DOCX有本地检查，metadata+file primitives完备但schema2/message linkage/Reader/provider/exports仍未接通，AR1–5未完成。当前仍无conversation JSON字段结构可供关联。
- 下一步：用户可提供小于工具限制的必要输入（脱敏引用结构，或原ZIP按字节切成每份≤25MiB供本地原样合并，非新建多卷ZIP），不需公开文件；收到后继续精确mapping实现。新CI产物独立核对也待续，不动Stable/Legacy/包名/签名/版号，不merge/Release。

## Attachment Restoration — Step F：单个真实DOCX的安全本地核对（2026-10-07）

- 分支feature/export-conversation，当前测试源码9ab3f72585e78e064468917636b75797c0f50861；本步仅安全审计/文档。用户另提供单个binary，本地只读magic/ZIP结构/CRC及Nova有界检测：DOCX、3459605bytes、40entries、expanded3737696bytes，content-types/word存在，xl/macros无，CRC通过。精确命中先前用户filename map，display扩展名一致；不输出或提交实际file name/ID/path/hash/正文/字节。证据tools/archive/evidence/attachment-local-binary-audit.json。
- success新[LegacyCI37645986535](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37645986535)全部job，尚未独立下载该runtime产物核对数量；[正式37645986505](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37645986505) build/API26 success、API35/36进行中，完整新回归尚未验收。
- not verified message association、schema2 DB导入、Reader/FileProvider、真实去重/重启/exports；AR1端到端pending，AR2仅map和单binary部分，AR3–5未通过。不是已恢复真实附件，也不是.docx打开能力实测。
- 必需待办保持：完全虚构同结构的图片asset_pointer和attachment-only message metadata、同一假ID对应map key，不能按binary/filename/hash猜message归属。收到后继续实施及新CI，Frozen/Legacy/包名/签名/版号保持。

## Attachment Restoration — 当前等待消息引用结构（2026-10-07）

- 实际分支feature/export-conversation，当前源码9ab3f72585e78e064468917636b75797c0f50861；本提交仅交接，不改代码。独立本地JVM80 passed；实际上传map新parser326条成功（只读map，无真实ZIP/binary/消息输入）。
- 新[正式CI37645986505](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37645986505) queued，[Legacy37645986535](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37645986535) in_progress，尚未完成build/API26/35/36/实际输出回归。dfb7ec8的37645538332由后继源码workflow concurrency自动cancelled；非手工取消，不能报通过。55830d1 AWT test编译失败证据保留。
- **Attachment Restoration未完成**：已完成独立ZIP/MIME/private file安全基础和verified string-map parser；未接schema2/ref upsert/offlineReader/FileProvider/exports/delete/persistence端到端。AR1未完成，AR2仅实际map结构/解析部分，AR3–5未验证。Legacy保留/defaultfalse，Stable与原包签名/版号不变。
- 必需输入：完全虚构等结构图片asset_pointer和attachment-only metadata，假ID与一条map key对应。已请求；当前map只是entry→display name，没有message linkage。不能凭file_与file-service prefix猜变换，不要求上传ZIP、conversationsJSON或正文。收到结构后继续实施并核对新CI；真实binary/manual仅用户本地验收。

## Attachment Restoration — Step E：实际map解析成功，message linkage等待结构证据（2026-10-07）

- 分支feature/export-conversation，实施基线dfb7ec87ac5426886c8dae20ab36bd6030ac4d34；本提交为bounded map源码检查点，最终CI sourceRevision以实际SHA为准。success本地JVM80（原48+file25+map7，128MiB heap）。
- 新ArchiveAssetMap基于已审计string→string真实结构，4MiB/2048条/严格JSON UTF8/key-display-token limits/CRC/重复-歧义/cancel。独立运行新parser读取用户真实mapping326条成功，只输出安全count；原文件/名称/ID/path/hash未入repo/日志/CI。实际ZIP/binary/消息未接触。
- schema1/生产asset恢复/refs/Reader/provider/export尚未实施；map接口与file接口未接Store。AR1端到端未通过；AR2仅partial实际map，AR3/4/5未验证。会话Level2–4另为用户已报告通过。不能把基础测试或mapcount当附件恢复。
- 55830d1测试AWT编译失败保留，dfb7ec8 fixture修正后新正式CI37645538332仍在验证；本提交触发80项源码正式/Legacy回归，状态待核对。
- 必需缺口：图片asset_pointer与attachment-only metadata的完全虚构等结构例，使用同一假ID对应map键。已向用户请求，不要求ZIP/正文；不得猜prefix变换/按文件名hash顺序关联。下一步从结构证据接schema2/精确mapping/offlineReader/exports再验收，不动Frozen/Legacy/包名/签名/版号。

## Attachment Restoration — Step D：测试编译失败与可移植图片fixture修正（2026-10-07）

- 分支feature/export-conversation，失败源码55830d144dd0418267beba47667580c54b6388a4；正式37644928632/Legacy37644928646在unit test编译失败，Android suites未执行，不报回归成功。错误为新增test使用桌面AWT/ImageIO，Android Gradle bootclasspath不提供。
- 修正只test：普通Java生成12×8虚构PNG（deflate+CRC）与有效baseline grayscale JPEG（DCT/Huffman markers）；不加入私有图片或额外依赖，原测试断言不删。本地JVM73再次通过，独立Pillow实际decode两图12×8通过；生产ZIP/file基础代码不因fixture失败变更。证据tools/archive/evidence/attachment-foundation-build-failure.json。
- not verified：新源码完整CI仍需跑；附件端到端/DB/UI与AR1–5仍pending，真实映射结构已核对但消息ref待用户完全虚构示例。
- 下一步：新CI验证，继续bounded真实string→string map parser；不猜message linkage。

## Attachment Restoration — Step C：真实映射文件仅结构审计（2026-10-07）

- 分支feature/export-conversation，安全基础源码55830d144dd0418267beba47667580c54b6388a4；当前正式CI37644928632、Legacy37644928646进行中，尚未验收。
- success用户提供的mapping文件本地只读核对：326 string→string，file_32hex.dat→原display name。只有结构与扩展名分类计数入库，真实文件及名称/ID/路径/hash未入库或输出。无MIME/size/dimensions/消息引用关系，无真实binary检查。证据tools/archive/evidence/attachment-map-schema.json。
- 不把扩展名统计当magic/恢复结果；AR2仅partial mapping审计，AR1完整mapped storage与AR3–5未验证。尚需完全虚构图片/attachment-only消息引用，已请求，不上传正文/conversationsJSON。
- 下一步：核对CI、安全基础，继续独立数据模型与存储准备；拿到引用格式后精确关联，不能猜file-service与entry变换。Stable/Legacy不改。

## Attachment Restoration — Step B：有界安全基础与25项新增fixture（2026-10-07）

- 分支feature/export-conversation，实施基线306ffa6，本提交为安全基础源码检查点（CI sourceRevision以实际提交SHA为准）。success本地JVM73，原48保持+新增25；ECJ/Java21 -Xmx128m，纯虚构生成binary。
- ArchiveZip替换原preflight并强化outer/nested全部路径/local-central/加密/method/symlink/extra/ZIP64/重叠验证。ArchiveAssetFiles新增独立budget、流式CRC/hash/size、magic/尺寸头/OOXML、private generated文件Batch回滚与stale recovery接口。安全基础已实现，asset接口未接生产/DB/UI，不能说附件恢复已实现。
- schema1/Reader/HTML/MD/PDF/MainActivity/Snapshot/旧scanner/四JS/签名/包名/版号均未改变；正式导入仍metadata，占位现状明确保留。未上传真实文件、名称、ID、路径/hash/输出。
- not verified新CI/Android26/35/36/Stable/Legacy/实际输出；AR1完整mapping与AR2–5pending。真实会话Level2–4仅用户新任务报告通过。
- 下一步：新CI核对，等待完全虚构等结构官方asset map+消息引用示例，再实施schema2/精确refs/跨导入upsert/offlineReader/provider/exports/cleanup，不可猜关联。设计与完整安全预算见docs/archive-attachment-restoration.md。

## Attachment Restoration — Step A：远端调查与设计（2026-10-07）

- 分支feature/export-conversation，调查源码e0ecabddfe6bb52a0569d8fc5b0fafee16f71180；默认远端main/e8ffa0c6不是开发基线。当前正式37634607647/Legacy37631515308 success。已读当前AGENTS/handoff/产品docs、Archive实际源码/SAF/SQLite/renderer/print/manifest/provider与tests/CI。
- success调查：schema1无asset表，binary未恢复；现有占位是Nova限制，不是用户官方ZIP无binary。设计见[附件恢复](../archive-attachment-restoration.md)：schema2 normalized assets/refs、private generated files、精确官方mapping、bounded integrity/MIME、offline handler/FileProvider、shared display与三格式语义。全部仍planned。
- 用户新任务报告真实会话Level2–4本地通过；记录为user-reported，不冒充开发环境独立验证，旧pending记录保留为历史。本环境无真实ZIP；附件AR1–5未验证。已请求完全虚构等结构mapping/ref样例，不请求私人ZIP。
- 下一步：先完成不依赖私有schema的ZIP/MIME/有界文件基础及synthetic tests，取得字段结构后接通附件功能；不猜关联，不动Stable/Legacy/版号/签名，不发布Release。

## Real export compatibility — 最终开发/fixture验收，用户真实验证待完成（2026-10-07）

- 分支feature/export-conversation；实际完整测试源码 **1990cb1ffb7a0832e94c552cc349023e6e5989d9**，本提交仅最终文档/证据。正式[CI37634607647](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37634607647) build/API26/API35/API36全success，独立下载与核对，不把此前红CI当成功。
- success Level1：JVM48/browser13/API26基础2；API35/36各Archive20+真正force-stop seed/restart2，原Stable17套43执行（Frozen10/登录/菜单/IME/upload/download/share/生命周期/原签名升级）。实际HTML/MD、Archive各16页PDF object text/recap/摘要标签/首尾/code/table/Unicode正确，thoughts/非当前branch排除；正式System/Firefox四份31页PDF同静态源、Unicode/图片/长代码表格/晚变更排除核对通过。
- success Legacy新CI37631515308，源码d36246e各API19通过+2历史Gecko-only Ignore；该源码与最终1990全部生产、共享scanner依赖/legacy fixture一致，仅Archive test-only/文档/CI断言修正。旧scanner/JS/失败历史未删除，defaultfalse/not-proven保持。
- 最终正式[签名APK](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37634607647/artifacts/11487454505)，约1.99MiB。独立核v2 signature/content digest/原证书/sourceRevision1990/默认legacy=false/保留assets/test provider排除；包名/签名/code14/name保持。无main merge/forcepush/GitHub Release，无FTS/附件/Legacy增强/DOM历史扩展。
- 修复边界：container512MiB；parser单会话chars/serialized/Store写入与reload统一4Mi UTF-16 chars，entry64MiB/selectedJSON256MiB/声明解压512MiB/raw node1MiB UTF8/depth64/ratio200/10000entries/nodes/100000values/DB512MiB保持。SAF复制前ZIP+128MiB DB/WAL规划+32MiB余量，未知SIZE持续检查，事务低空间A09回滚。streaming/单事务/current_node/分支/未知metadata保持。
- 统一显示：String/Map.text可见；asset与未知Map分别占位；thoughts任何阅读/导出scope隐藏但raw/树/DB保留；reasoning_recap明确推理摘要。诊断有安全输入计数/类型、复制/导入耗时、DB/temp/free、采样heap/WAL，无正文/真实title/ID/filename/path/hash。采样值不冒充精确峰值。
- 证据 `tools/archive/evidence/compatibility-runtime-35.json`、`compatibility-runtime-36.json`、`compatibility-1990cb1-package-verification.json`、`compatibility-legacy-regression.json`、`real-export-compatibility-summary.json`。此前dcd不可变Map及3e输出fixture接线失败保留，不改成绿，生产代码无需为这两次fixture失败更改。
- **not verified real Levels2–4；Level5 manual content validation pending。** 用户确认真实ZIP在其手机/电脑；本环境未读取真实ZIP、未做旧版real runtime A05、真实首次导入/DB大小/耗时/heap/temp/重启/重复导入/真实三格式人工内容核对。370566688bytes/non-ZIP64/605entries/两JSON49.44+6.00MiB/122会话/7250节点/7130消息等仅用户提供审计，不当作新APK实测。未上传私人文件/输出/日志/截图至开发环境/GitHub/CI。
- 文档 `docs/nova-archive.md` 与[本地清单](../archive-local-validation.md)同步。**下一步由用户在本地安装最终APK，按清单反馈脱敏诊断、restart/duplicate状态、至少5会话与HTML/MD/PDF人工检查数量。** 不请求上传ZIP/正文；收到结果再分级记录真实Level2/3/4，只有用户明确确认才能Level5。不声称所有未来官方schema/ZIP64/附件恢复/服务器完整性。

## Real export compatibility — Step F：20项通过，实物检查夹具接线修正（2026-10-07）

- 分支feature/export-conversation；源码3e2d39f4be08ce333f6b77a128abf212fb62dd66，CI37633057743两API各Archive20全部success。
- failed后置实物检查：HTML/MD SAF test仍使用旧short fixture，不含新schema，但脚本要求object text/recap；print source/实际PDF已使用long/schema正确夹具。更改SAF test输入为同schema夹具并添加显式文字/摘要/hidden-thought断言，不删/放宽CI内容检查；失败消息固定化，避免空assert错误。
- success独立下载当前实际两PDF，pdftotext核object text/recap/中文标签/首尾/code/table/多语言，hidden thoughts/OTHER-BRANCH排除；仍不能说Stable通过，后置失败阻止Stable与restart suites。
- 证据 `tools/archive/evidence/compatibility-output-fixture-failure.json`，前一不可变Map失败保留。生产代码不变，新源码须重新跑完整CI。
- not verified：最终完整Archive/Stable/实际HTML/MD待下一CI；真实用户设备Level2–5pending。Legacy生产/依赖仍与已成功d36246e一致。
- 下一步：完整新CI实际产物验收后交付APK与本地清单，无新功能扩展。

## Real export compatibility — Step E：最终修正源码的基础验收（2026-10-07）

- 分支feature/export-conversation，实际源码3e2d39f4be08ce333f6b77a128abf212fb62dd66，CI37633057743。
- success：build/Gradle/lint/browser/JVM48与API26基础2；独立下载JVM XML/API26 instrumentation确认，不只引用job状态。独立下载APK v2签名/content digest/原证书/source revision/defaultlegacy=false实核，原包名/签名/code14/name保持，旧4export+freeze assets一致；test provider/虚构内容未入生产APK。
- 证据 `tools/archive/evidence/compatibility-final-package-verification.json`，最终签名APK artifact11487790533。前一份dcd package报告/失败不覆盖。本轮生产代码与成功Legacy测试源码d36246e一致（后续只有Archive test-only修正/文档）。
- not verified：最终API35/36 Archive20+restart2/Stable43/实际schemaPDF仍进行中；真实用户文件未在开发环境使用，Level2–4 pending、Level5 manual pending。
- 下一步：最终Android Artifact独立核对后交付APK。此提交只文档/证据，不改已构建源码或触发新公开发布。

## Real export compatibility — Step D：真实Android夹具失败与修正（2026-10-07）

- 分支feature/export-conversation；失败源码dcd9464bba57aa54868d7d6fc35074c0087b8cae，CI37631627786，两API均20项/8失败；其余12包括3Mi SQLite往返/重复导入通过。后续Stable suites未执行，不能宣称回归通过。
- 原因：Android synthetic fixture node.message原Map.of不可修改，新增longBody schema替换content抛UnsupportedOperationException（导入前）。改为LinkedHashMap的虚构message，生产代码不改、schema/真实PDF断言不删。
- success：本地编译并执行实际ArchiveFixtures短/长JSON、普通/slowZIP生成，四变体均通过；核心JVM48已有通过，新Android须以本提交重新验证。失败保留 `tools/archive/evidence/compatibility-fixture-failure.json`。
- success：独立LegacyCI37631515308源码d36246e，两API19执行通过+2历史Gecko Ignore，下载核对 `compatibility-legacy-regression.json`；API26基础2独立Artifact通过。legacy生产/共享依赖与当前源码相同。
- not verified：新源码正式Android/实际schemaPDF/Stable待重新运行；真实文件仍用户本地，Level2–5pending。本提交仅test-only fixture修正及证据，未改签名/包名/版号/legacy/snapshot。
- 下一步：新CI全绿与实际输出核对，提交最终文档/APK及用户本地清单。

## Real export compatibility — Step C：新源码构建与签名实核（2026-10-07）

- 实際分支feature/export-conversation；正式测试源码dcd9464bba57aa54868d7d6fc35074c0087b8cae，正式CI37631627786。
- success：build/Gradle/lint/browser、CI JVM XML48 tests/failures0/errors0独立下载核对；下载签名APK，v2 signature/content digest/原证书/SHA256/DEX源码revision与defaultlegacy=false均实核，包名/code14/name不变，旧export与snapshot assets一致，无native.so。
- 证据 `tools/archive/evidence/compatibility-package-verification.json`，正式APK artifact11487340777。`real-export-compatibility-summary.json`严格区分用户提供audit与本地未验证真实结果，不含正文/title/ID/路径或私有文件hash。
- not verified：API26/35/36/实际新schema PDF/Stable仍进行中；Legacy37631515308源码d36246e新构建success，Android仍进行中，dcd只修Archive test-provider未知SIZE分支，共享legacy/生产源码一致。
- 用户确认本地真实ZIP；Level2–4 pending、Level5manual pending、旧版real runtime not verified。已提供仓库内 `docs/archive-local-validation.md`，不得把数目匹配当人工内容验收。
- 下一步：完整CI及实际Artifact核对，然后交付已验证签名APK；本提交仅文档/证据，不改已构建源码。

## Real export compatibility — Step B2：未知SAF SIZE夹具检查（2026-10-07）

- 实际分支feature/export-conversation，实施源码基线d36246e044e52fff86dcc50a2458df37db7a7c02。
- 代码复查发现test-only provider的嵌套数字/null ternary可能对未知SIZE解箱null；改为显式分支，真实生产逻辑不改。保留未知SIZE实际复制场景，不吞provider错误。
- success：diff/Java格式检查；核心JVM48未受影响。not verified：新源码Android/完整CI待运行。用户真实ZIP仍在用户本地，Level2–5未标通过。
- 下一步：以下新源码为本轮CI验收基线，完成签名APK交付与本地清单。

## Real export compatibility — Step B：有界兼容修复与虚构回归（2026-10-07）

- 分支feature/export-conversation；实施基线9668e8b，本提交为修复源码；测试buildRevision将记录实际本提交SHA，后续交接引用CI真实源码。
- success本地JVM48（128MiB heap），300MiB sparse container+small selected JSON、512MiB metadata/64MiB entry/256MiB selected boundary、2–4Mi conversation/超限/escaped序列化、空间预算、multimodal/text/asset/未知Map、隐藏thoughts/recap标签、648主链/16分支无children等。既有39项保留（资产文案/字符预算断言同步）；先前1个旧占位断言失败已修为附件语义，不删除场景。
- 修复：container512MiB；parser字符及serialized/Store聚合写入与重载4Mi chars。其他限额未放大；数据原始metadata/分支/空根/未知roles保留。复制前ZIP+128MiB DB/WAL规划+32MiB余量，复制中/逐会话检查，A09低空间回滚。诊断新增安全计数/阶段时间/空间/采样heap/WAL，无正文/真实身份/路径。
- UI与导出共用Node.displayable/text；thoughts不作为回答显示，recap单独推理摘要；Map.text:String保留可见文字，资产与未知Map分别占位。Reader可见count与保存message count区分。
- Android新增3项至20：mocked300MiB/未知SIZE/超限SAF、3MiB SQLite往返/重复来源时间、schema raw与两scope/Reader一致。真实PDF fixture包含object text/recap/hidden thought，CI输出断言加强；真实数据未进入fixture/CI。Legacy workflow补ArchiveModel变更触发，以新APK跑用户本轮要求的Legacy回归，不改scanner。
- not verified：新源码Gradle/Android26/35/36/Stable/Legacy/实际PDF待CI。用户确认真实ZIP仅在手机/电脑，可用修复APK本地验收；Level2–4 pending，Level5 manual content validation pending，旧版real runtime也未执行。
- 下一步：完整CI验证新源码；提供签名APK与本地清单，等待用户脱敏验收结果。无FTS/附件/DOM扩展，不改签名/版号，不merge/Release。

## Real export compatibility — Step A：远端调查与样本边界（2026-10-07）

- 分支feature/export-conversation，远端/本地HEAD `0c154064bfa092bd2ec2025c9f0b01e81b76ae69`；远端默认HEAD main/e8ffa0c6，未作为开发基线。Archive核心与已测7854f3ea一致；当前正式CI37609817075、Legacy37604491058均success。
- 已读取AGENTS、当前handoff/产品docs/实际Archive源码/tests/workflows。用户新要求仅修已知真实容量/schema问题，不做FTS/附件/Legacy或DOM功能扩展。
- 用户结构审计提供：370566688 bytes、non-ZIP64、605entries、两拆分JSON约55.44MiB、122会话/7250节点/7130消息；仅用户提供事实，本环境未独立实测真实ZIP。
- confirmed source failure：旧SAF metadata及流式copy均FILE_LIMIT256MiB，旧parser chars1Mi/serialized2Mi，Store写入/重载同样2Mi。真实样本必超container limit；**real sample / old code runtime not verified**，不倒推成真实运行证据。
- 本环境目前仅收到任务Markdown，没有真实ZIP；不能完成Level2–4本地真实导入。Level5必须用户本人确认至少5个会话。所有新增fixtures纯虚构；真实文件/输出只能repo外，增加专用/local-private/ignore，不泛化忽略synthetic。
- 设计：container512MiB，conversation chars与serialized/store aggregate统一4Mi chars；其他entry/selected-total/node/depth/DB限额不变。复制前及复制中私有空间检查；统一display语义支持Map.text/附件占位、隐藏thoughts保留raw、recap独立标签。保持streaming/事务/树/current_node/离线导出。
- 下一步：实施最小兼容修复及synthetic regression，然后新源码完整CI；有真实文件才能运行本地真实验收，不上传GitHub/CI，不提交正文/ID/title/路径或私有文件hash。

## Nova Archive — 最终验收与交付（2026-10-07）

- success：**Nova Archive MVP implemented — synthetic / fixture validation passed**。分支 `feature/export-conversation`；实际测试源码 `7854f3ea06309fa20e5e26a7acc4b77750322968`，后续checkpoint/本提交仅文档与证据，不改已验证源码。
- [CI37609817075](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37609817075) 完成全success：build/Gradle/lint/browser13/JVM39，API26基础2，API35/36各Archive17+独立进程seed/restart2，以及原正式17套43次执行。没有仅依赖旧结果或算法停止推断完整性。
- 独立下载两份API35/36 Artifact核对实际HTML/Markdown/PDF、已安装APK指标与已验证签名包一致。Archive各16页PDF首尾/code/table/中法英/LaTeX源通过，OTHER-BRANCH不混入主链导出；原Frozen/System与Firefox四份31页PDF Unicode/首尾/长代码/表格/图片/晚变更排除通过，同打印HTML源，Firefox首页必要时独立render/OCR验证。
- 证据 `tools/archive/evidence/accepted-runtime-35.json`、`accepted-runtime-36.json`、`accepted-package-verification.json`。每份记录源码/CI/artifact ID与SHA256，实际文件统计；此前失败报告保留，不改写红CI历史。
- 架构：Settings→独立ArchiveActivity/Reader；SAF主动ZIP/JSON→有界streaming parser→私有SQLite schema1事务upsert；current_node主链/全部节点明确安全顺序，保留分支/未知metadata/显式null；标题搜索分页200；Archive模型独立渲染HTML/MD→offline静态WebView System Print。删除本地库/临时cache，不清在线登录。诊断无正文/真实ID，不用Cookie/DOM/backend/cloud。
- Stable保存当前已加载网页/FrozenPageSnapshot语义不变；Legacy scanner/四JS/测试/失败证据全部保留。独立LegacyCI37604491058源码66172a1d各API19通过+2旧Gecko专项Ignore；legacy调用链/构建依赖/flag/共享SnapshotWebView/MainActivity与最终源码一致，默认false，不称完整会话。
- 包名/原签名/code14/name不改；APK v2签名/content digest实核。未merge main、force push或发布GitHub Release。README、nova-archive、frozen-page-save、Legacy分层与REVIEW同步。
- not verified：无真实官方Data Export文件、真实账号/物理设备/OEM WebView/任意未来OpenAI schema验证。ZIP64/多卷/自解压/超安全限额不支持；附件metadata占位、LaTeX源、标题搜索；完整性仅依赖用户导入本身，Nova不证明服务器全部历史。详见 `docs/nova-archive.md`。
- 下一步：用户本地真实兼容官方导出人工验证（聊天正文不得提交GitHub）；然后按需要单独评估SQLite FTS全文检索。不要恢复DOM完整历史路线，不删除legacy。本轮工作已完成，无进行中的实现步骤。

## Nova Archive — Step 4m：最新修正源码的安装包核对（2026-10-07）

- 分支 `feature/export-conversation`；实际源码 `7854f3ea06309fa20e5e26a7acc4b77750322968`，CI `37609817075`。
- success：build/Gradle/lint/browser、JVM39、API26基础2项；独立下载APK/JVM/API26 Artifact，验证签名与内容digest、原证书、源码revision、包名、code14/name不变、默认legacy=false、旧assets一致、无native.so。证据 `tools/archive/evidence/accepted-package-verification.json`。
- not verified：该源码API35/36完整Archive/Stable与实际输出仍运行中；证据文件名不表示整个MVP已验收。没有真实官方ZIP或物理设备验证。
- 下一步：下载全部Android输出并独立核对，完整正式回归全绿后完成最终交接；本提交仅证据/文档，不修改测试源码。

## Nova Archive — Step 4l：Archive 实際输出通过；正式回归夹具修正（2026-10-07）

- 分支 `feature/export-conversation`；验证源码 `880c8fb`，CI `37607311483`。完整下载两份API35/36 Artifact，Archive summaries failures=[]，各 instrumentation17 + 独立 seed/restart2 通过。真实 System Print PDF 各16页，独立pdftotext核对首尾/code/table/中法英/LaTeX标记，非当前分支不混入HTML/MD/PDF。
- success证据 `tools/archive/evidence/archive-runtime-verification.json`，包含实际输出SHA256/页数/断言；仅synthetic / fixture validation，不是real OpenAI export verified。
- failed正式回归：NovaWebViewTest预期Settings仍只有旧3/4项，未包括用户授权新增Nova Archive；已更新准确预期，原有账号/清理行为断言保留。
- failed API35 Frozen PDF夹具：DocumentsUI已有Archive PDF后，android:id/title首先匹配文件卡片TextView。旧循环立即return false，不继续找可编辑文件名框。已依据保存UI截图/树，只接受可见启用EditText，SET_TEXT失败继续；没有增加网页selector，也不删/放宽实际PDF/内容检查。API36原Snapshot10项与31页PDF已success，生产打印管线不改。
- 生命周期小修：删除Archive成功后清掉已完成Task，旋转不会重播旧导入成功状态；已有删除隔离test现覆盖SAF导入→删除→重建，Cookie保留。
- 下一步：新源码重新跑Archive/Stable全部CI，直到正式回归全绿再宣称MVP验收完成。

## Nova Archive — Step 4k：最终源码签名包与 JVM Artifact 独立核对（2026-10-07）

- 分支 `feature/export-conversation`；当前实际源码 `880c8fbab3393c7a10084a3bc08266e29a093482`；后续提交只有文档/证据；CI `37607311483`。
- success：独立下载同源码APK与JVM/API26 Artifact。APK v2签名与内容digest、原证书、SHA256、DEX源码revision、包名/versionCode14/versionName不变、默认scanner=false、旧4assets一致、无native.so均确认；JVM XML tests39/failures0/errors0，API26 `OK (2 tests)`。
- 证据 `tools/archive/evidence/final-package-verification.json`，保留前轮 package/失败报告，不覆盖失败历史。
- not verified：Android35/36同一job仍运行Archive17、进程重启2与Stable43执行；没有最终结果和PDF内容验收，不称MVP验收完成。没有真实官方ZIP/物理设备验证。
- 下一步：下载模拟器完整结果与实际输出，依据证据完成最终交接。

## Nova Archive — Step 4j：修正后最低版本再次通过（2026-10-07）

- 分支 `feature/export-conversation`；实际源码 `880c8fb`；CI `37607311483`。
- success：该源码 build/Gradle/lint/签名/browser/JVM39，以及API26基础2项再次通过。显式null修正后最低版本仍可导入/存储/读取/渲染/清理打印adapter。
- docs：Legacy产品分层表不再把Archive写为永久“未来未实现”，改为引用独立Archive当前实现/验证文档；仍保留旧隔离轮次、失败证据与实验完整性not-proven。
- not verified：API35/36完整Archive/实际PDF/重启/Stable回归仍在同一CI执行，不能将已有部分success当作完整MVP验收。真实官方导出文件未验证。
- 下一步：下载API35/36结果，核对实际输出并完成最终交接。

## Nova Archive — Step 4i：数据库 null 根节点往返与测试生命周期修正（2026-10-07）

- 分支 `feature/export-conversation`；实际失败源码 `4a79a97`，CI `37605668339`，API35/36各执行17方法，8条failure entries（包含同一方法的测试与teardown重复错误）。未运行后续Stable suites，不能说已回归通过。
- 生产原因：Gson默认丢掉显式null的Map字段，root parent/message落库后丢失；重载误判malformedParent，主链变全部节点，HTML包含非当前分支。改为serializeNulls，未知metadata的null同样保留。
- 测试原因：ActivityScenario.launch Reader 默认清前任务，原Archive已销毁；@After无条件onActivity恢复方向触发NPE。清理改为检查真实RESUMED状态，不吞业务失败。新增真实原生会话行点击→Reader检查，保留原来的独立Scenario销毁/重建测试。
- success：JVM39，通过新增raw header/node数据库序列化往返的null根节点/主链/unknown null测试。API26此前2项及原签名验证仍是真实已执行，但新源码须重新验证。
- 可读性修正：首页原生ListView header承载控件，可随列表滚动，解决横屏固定控件挤没列表；仍每页200条。默认状态改为选择导入或阅读现有记录，避免重启已有库误写“尚未导入”。
- 证据 `tools/archive/evidence/null-root-runtime-failure.json`；测试失败也尽力拉取已生成的合成HTML/MD/PDF，不丢失败文件。
- 下一步：新源码 API26/35/36、实际PDF/重启/Stable回归；不以旧的部分通过宣称完成。

## Nova Archive — Step 4h：最低版本与安装包独立验证完成（2026-10-07）

- 分支 `feature/export-conversation`；验证实际源码 `4a79a97`，当前后续提交仅证据/文档。CI `37605668339`。
- success：API26两项基础测试均通过，独立下载 Artifact 确认 `OK (2 tests)`；API容量接口修正已验证。
- success：下载同一源码 JVM XML，tests38/failures0/errors0/skipped0；不是仅引用历史报告。
- success：独立下载签名APK，验证APK v2签名及内容digest、原证书指纹、公开SHA256、DEX源码revision、包名、versionCode14/versionName不变、ENABLE_LEGACY_SCANNER=false、无native .so、旧4个实验assets与源码一致、Markdown/Gson许可证存在。证据 `tools/archive/evidence/package-minsdk-verification.json`。
- not verified：API35/36 Archive17项+真实PDF+process restart及Stable全部回归仍运行中；没有真实官方导出/物理设备证明。
- 下一步：检查实际输出与所有正式回归结果，完成最终MVP报告。

## Nova Archive — Step 4g：Legacy 回归独立完成（2026-10-07）

- 分支 `feature/export-conversation`；Archive当前源码 `4a79a97`（数据库接口修正），Legacy验证源码 `66172a1d`。两者的 legacy scanner、MainActivity、BuildConfig依赖、共享SnapshotWebView没有源码差异；后续差异仅Archive适配/测试及文档。
- success：Legacy CI `37604491058` build与API35/36均success；独立下载两份Artifact，instrumentation各 `OK (19 tests)`，历史Gecko-only PDF ignored保持原状，不据此宣称legacy完整历史。
- 证据 `tools/archive/evidence/legacy-regression.json`。旧JS/ConversationExport/诊断/失败历史全部保留，默认flag仍false。
- success：修正后Archive CI `37605668339` build再次通过；API26/35/36正运行，尚未验收。
- 下一步：完成Archive+Stable实际运行与PDF证据，更新最终文档。

## Nova Archive — Step 4f：首轮 Android 数据库容量接口失败（2026-10-07）

- 分支 `feature/export-conversation`；源码基线 `29be17176cc3ed26ea0139b8f2a6deadf8f3179e`，CI `37604805034`。
- success：该源码 Gradle build/lint/签名/browser与 JVM38 全部通过；已独立下载 XML 核对 tests38/failures0/errors0/skipped0。
- failed：API26 两项基础测试在 import bootstrap 捕捉 A06（事务前）。原因：通过非查询 execSQL 执行返回结果的 PRAGMA max_page_count。改用 Android SQLiteDatabase.setMaximumSize(512MiB)，保持同一安全上限。
- 证据 `tools/archive/evidence/first-runtime-failure.json`；保留 CI 失败记录。API35/36尚在运行，不声明 success；本提交需要新的真实Android验收。
- 下一步：重新执行 API26/35/36、实际PDF和所有正式回归；只修真实运行失败。

## Nova Archive — Step 4e：有界列表分页与导出销毁可见性（2026-10-07）

- 分支 `feature/export-conversation`；基线源码 `66172a1`，CI build 正在运行，运行验收未完成。
- 修正：首页真正 SQL LIMIT/OFFSET 分页，每页最多200条；上一页/下一页替换列表，而非不断累积至100000条常驻内存。搜索/排序重置分页，旋转保留页号。1000会话 instrumentation 核对页首799/999及200条上限。
- 修正：Reader 导出 generation/destroyed 设为 volatile，后台写入能观察 Activity 销毁/切换，避免仅依赖线程间未定义的字段可见性。
- success：JVM38 fixture结果未改变（本步只改Android adapter）；源码 diff/脚本语法检查通过。
- not verified：本提交 Android/实际PDF/正式回归待新CI。没有真实官方ZIP或物理设备验证。
- 下一步：冻结功能范围，完成 CI 验收，只根据实际失败修正。

## Nova Archive — Step 4d：运行前最终边界与 UI 检查（2026-10-07）

- 分支 `feature/export-conversation`；基线源码 `174ad7e`。
- success：该基线正式 CI `37603996077` build 通过（Gradle/签名/lint/JVM36/browser），Android jobs 进行中。此检查点新增后须以新源码重新运行，不能沿用旧结果当新验收。
- success：本地38项JVM synthetic tests；identity/parent key 4096字符限额避免 CursorWindow 组合超限；非字符串/缺 parent 字段保留metadata、明确全部节点安全顺序；相同 message id 的多个mapping节点保留并警告。
- Android tests 现为17项，新增 SAF JSON + malformed ZIP/JSON 的固定错误体验、SQLite事务进行中取消回滚；导入生命周期测试使用真正 orientation change。另 process restart2项、API26基础2项。
- 新 ArchiveUi 复用已有 AndroidX Insets，在target35边到边窗口内保证原生控件不被状态/导航栏遮挡；仅 Archive 使用。Legacy CI 同步关注共享 SnapshotWebView 变更，旧算法/fixtures全部保留。
- not verified：本提交实际Android运行、PDF内容、正式回归待新CI。真实官方ZIP/物理设备未验证。
- 下一步：不增加新功能，完成CI、修正实际失败、归档验收。

## Nova Archive — Step 4c：最低 Android 版本与 Markdown 边界（2026-10-07）

- 分支 `feature/export-conversation`；基线源码 `0eab4e3`。
- success：36 JVM synthetic tests；Markdown AST 先迭代限制深度/节点数，再递归 HTML 渲染，新增极深 quote 明确 A05 测试。
- 为保持 minSdk26 与包规模，改用 Java8 兼容 CommonMark/GFM 0.21.0，功能/安全测试同样通过；撤回不必要的 coreLibraryDesugaring。Gson2.11 保持。许可证随包。
- 新增 API26 独立 2 项基础 instrumentation / CI job，验证 SQLite重复持久化、Java Markdown/GFM、离线 WebView与打印adapter清理；API35/36仍负责实际PDF与全部正式回归。
- not verified：API26/35/36 新 CI 尚未返回。前一轮 `37603606423` 正在检查修正，不写成完整验收；没有 real OpenAI export verified。
- 下一步：读取 CI 运行证据，修正并完成 MVP 验收。

## Nova Archive — Step 4b：安全复查与首轮构建失败修正（2026-10-07）

- 分支 `feature/export-conversation`；基线源码 `fd42862`。
- failed：CI `37603056935` AndroidTest 编译中 ArchiveTest 私有 read(Uri) 与 FixtureActivity 同名包级方法冲突，Android jobs 未执行。保留此失败，不当作运行结果；已改为独立 readFixtureDocument。
- success：35 JVM synthetic tests，128MiB heap；新增 CRC 损坏、10001 entries、中央目录提前限额、对象键总字符预算、5000 节点实际解析。ZIP 中央目录先有界检查条目再创建 ZipFile，避免恶意目录索引提前分配。单 JSON token/depth 与对象键同样有预算。
- UI 列表 SQL 仅取 512 字符标题（完整原文仍私有存储），搜索输入256字符，避免200个巨型标题常驻列表；已有消息重复导入也更新最近来源。新 Archive 文件按统一 Java 格式整理，未重构其他在线功能。
- not verified：修正后的 Gradle/lint/emulator/实际PDF及正式回归等待新 CI。没有真实官方导出文件。
- 下一步：继续 CI 验收，依据失败证据修正直到满足 MVP 标准。

## Nova Archive — Step 4a：Android 验收夹具与 CI 接入（2026-10-07）

- 分支 `feature/export-conversation`；基线源码 `2531aad`，本提交包含 Android tests 与 CI 调度。
- success：30 JVM tests；Android 新源码/测试的本地参考 API 编译与 Python 脚本语法检查通过，不等同 Gradle/运行验证。
- 新增 15 项 instrumentation：SAF ZIP/取消、SQLite重复/缺ID/回滚/取消、1000会话/标题搜索/排序/分页、HTML/MD真实content URI写入、Reader策略/链接/Cookie保留、导入旋转/销毁/慢provider取消、Reader重建清理、删除隔离、实际多页PDF与打印取消重试。另 2 项通过独立 instrumentation + shell force-stop 验证进程重启。
- CI `dom-trial.yml` 在正式回归前运行 Archive；保存实际HTML/MD/PDF、pdftotext标记和重启证据。原有正式 suites、升级与 Legacy job 保留。CommonMark Java11使用 coreLibraryDesugaring，minSdk/package/signature/version 未改。
- not verified：本提交的 emulator/Gradle/lint/实际PDF结果尚未返回，不称 MVP implemented。没有真实官方导出样本。
- 下一步：检查新 CI，按实际失败修正，完成全部正式回归和最终证据。

## Nova Archive — Step 3：原生 UI 与独立导出（2026-10-07）

- 分支 `feature/export-conversation`；源码基线 `80e31dd`，本提交实现 Archive 菜单/原生首页/SAF 导入、标题搜索/排序/分页、离线 Reader、主链/全部分支、HTML/Markdown SAF 保存与 System Print PDF。
- success：30 项 JVM 合成测试；Java Markdown code/list/quote/table、安全 HTML/URL、LaTeX 源文；本地 Android API 参考编译检查通过（不是 Gradle/lint 或模拟器验收）。CommonMark/Gson 许可证随包。
- 架构：Archive 不创建 FrozenPageSnapshot，不读在线 WebView/bridge/cookies。SnapshotWebView 新增静态离线输入，旧构造路径保持；Archive 全部资源请求阻止，JS/file/content/storage 关闭。正式快照回归必须通过。
- 限额补充：10000 会话/200000 mapping 节点；单 node/header 1MiB UTF-8 避免 CursorWindow 大对象。消息统计排除 null roots；分片重复会话统计按本地 row 去重。
- not verified：Android SAF/数据库/生命周期/真正 PDF 输出及正式回归等待下阶段 fixture instrumentation。没有 real OpenAI export verified。核心步骤 CI `37601570418` 正在运行，不将进行中写成 success。
- 下一步：Android instrumentation 与实际 PDF 文件内容检查、CI、失败修正和最终验收。

## Nova Archive — Step 2：核心与存储实现（2026-10-07）

- 分支 `feature/export-conversation`；步骤基线源码 `d9ddca7`，本提交包含核心实现。
- success：逐会话严格 JSON / ZIP discovery、安全路径/CRC/规模限制；保留 mapping、未知 metadata、全部分支；current_node 主链与 cycle/orphan 安全降级；身份 upsert 计划；私有 SQLite schema v1 与事务回滚。
- success：独立 JVM 合成测试 28 项，128 MiB heap；1/100/1000 会话、9999 节点树、超长代码/多语言、恶意 ZIP/JSON、重复合并/旧导入、取消/时间限额。证据 `tools/archive/evidence/core-jvm.txt`，脚本 `tools/archive/jvm-tests.sh`。
- not verified：Android SQLite/SAF/UI/导出尚待实现和 emulator 验证；没有真实官方数据样本。此步骤不是 MVP 完成。
- 下一步：原生 Archive Activity、离线 Reader、HTML/Markdown renderer 与独立 System Print 输入，随后 CI。

# 当前任务：Nova Archive MVP（Step 1 调查与设计，2026-10-07）

当前分支feature/export-conversation，实际远端/源码基线d48aed0da152025902b83b1027b3a8e8bfc918ad；远端默认HEAD main/e8ffa0c6但不是最新开发线。已读取AGENTS/latest/REVIEW/frozen-page-save/README、Main菜单/manifest/Gradle及现有SAF/静态打印/CI。最新正式37593888527与实验37588638326 success；旧failure/cancel保留，不当当前现状。

用户最新要求新增独立Nova Archive：主动官方兼容ZIP/JSON→安全流式解析→私有SQLite→列表/标题搜索/current_node Reader→HTML/MD/System Print PDF。既有Frozen Snapshot和默认禁用Legacy保留，不删scanner/证据，不新增后台/API/Fiber/DOM完整历史路线。原包名/签名/code14不变，不main/forcepush/Release。

Step1 success（调查/设计），实现与新测试not verified。设计详见../nova-archive.md：schema1 sources/conversations/messages，所有mapping节点及未知metadata保留，身份upsert/保守缺ID/旧时间保护，原子导入回滚；ZIP/JSON/size/depth/token/CRC限制；离线无JS无网络Reader；独立HTML数据经最小静态打印入口复用System Print，不构造FrozenPageSnapshot。MVP标题搜索，非全文。仓库无真实Data Export样本，仅synthetic。

下一步实现pure Java parser/model/tree/merge、SQLite与JVM fixtures，完成后按步更新交接commit/push，再实现UI/export与Android/正式回归。下面是前轮Legacy验收历史，不代表Archive已实现。

# 当前交接：Legacy Scanner 隔离（2026-10-07）

工作分支 `feature/export-conversation`；本轮最新构建源码 `3149507ef3fb88d66428dde610894ccac82cf44b`；实验已测源码 `5d9efe8eee0c24d1ce2b1a48b6e14f74360ae583`（应用/legacy源码和夹具完全一致，仅正式原生测试同步变更）；当前文档基点 `1682ecf`（完整HEAD以git log核对）。最终文档提交可由git log核对，不等于APK源码HEAD。远端默认HEAD仍main/e8ffa0c6，本轮全程以工作分支最新远端为准；开始续接先fetch并读AGENTS/latest/REVIEW/legacy-conversation-scanner/frozen-page-save，不回退旧修复。

## 当前结论（优先于下方分步历史）

**本轮 Legacy scanner safely isolated：实现、受控验收与实物核对完成。** Stable仍保存当前已加载网页→HTML/Markdown/System Print PDF；Archive官方导入仍未来规划；Experimental只显式-PnovaLegacyScanner=true启用，默认debug/release=false、不构造controller、不注入或自动扫描。实验设置警告确认→用户开始→前台当前会话→可取消/恢复，结果/文件/脱敏diagnostic永远not-proven。旧ConversationExport类名/包路径保持以兼容package-private测试，职责与三层映射见legacy文档；算法/四份assets/失败历史未删除，不扩数据源或ChatGPT selector。

正式源码3149507ef3fb88d66428dde610894ccac82cf44b，正式CI37593888527 attempt2全success：API35/36各17套43项（default合同2、快照10、Firefox1、web/share/input/IME/native/两个旧签名基线上覆盖升级）。独立下载实际HTML/MD及Nova/Firefox四PDF（各31页）核首尾、Unicode、长代码/表格、320×120图片、后续mutation排除及相同打印源；Firefox首页独立render/OCR。只修原生测试的目的地初态/分页等待；FrozenPageSnapshot/PageSnapshotExport/SnapshotWebView/freeze.js/输入/分享核心源码与基线无diff。

实验已测源码5d9efe8eee0c24d1ce2b1a48b6e14f74360ae583，CI37588638326全success，35/36各旧16+新3=19通过；Gecko2项明确退役。与3149507应用/legacy源码及fixtures完全一致，后续仅正式原生测试/文档/证据变更。本地DOM18/scroll32/progress19/locator18/snapshot13通过。默认APK1916153bytes/SHAb35bed4d50df1a2b7bdeb50d902f7fba0274ddb533e4aaada90d5b4ad5498b96；实验APK1917377bytes/SHA423270eda1b8811372867a5fbc0ed7c466e1241a36d60b08b2af75d305e66204。均独立核原证书/v2签名content digest、DEX默认false/实验true/各自revision及5份assets，无native so。包名/签名/code14不变，无main合并/forcepush/公开Release。

证据：[隔离验收JSON](../../tools/legacy-scanner/evidence/isolation-validation.json)、[实际包核对](../../tools/legacy-scanner/evidence/package-verification.json)、[正式API35](../../tools/legacy-scanner/evidence/stable-35-final-verification.json)、[正式API36](../../tools/legacy-scanner/evidence/stable-36-final-verification.json)。原5d9零页范围打印失败、fbd过窄等待失败、314 API35首次系统无障碍NPE及更早旧文案失败均如实保留，不改写为成功。

下一步仅物理设备/真实账号人工体验与覆盖率对照（未验证）；不把fixture成功/scan completed等同完整历史。保留not-proven，不新增后台/批量/API/internal reader；暂不建议sourceSet，四研究脚本约37.6KB且默认无初始化。正式未来Archive保持独立，不自动访问官方导出。下方是每步当时状态，ac4776f冻结网页验收属于历史。

## 分步历史：最新要求与 Step 4b（2026-10-07）

当前分支feature/export-conversation，待Android验收源码4ca3c0428f46a646fc6c95a476349e35942888d8；审查29ea76d、隔离3d7f051、CI分类755d727、minSdk26与顶层错误码白名单修正4ca3c04。用户要求隔离旧实验scanner，默认false、显式property开启、警告/手动/前台/取消/恢复、脱敏not-proven，保留旧代码与失败证据；不增selector、不扩数据源、不main/Release/版号。

本步独立本地验证success：DOM18、scroll32、progress19、locator18、snapshot13；Python语法/YAML/diff通过。受跟踪文件无删除，FrozenPageSnapshot/PageSnapshotExport/freeze.js/ComposerWebView/WebShareAdapter与原基线无diff。报告：../../tools/legacy-scanner/evidence/isolation-validation.json。

正式CI37587862891构建/lint/unit与原签名检查success，Android35/36进行中；legacy37587862825浏览器success、构建进行中。755d727两次旧CI因新提交concurrency cancelled，不能写通过。当前Android新增default合同2、实验19及原正式回归not verified，待读取结果修复实际失败。真实账号/物理设备未验证。下一步核对两套CI、下载小报告独立验证、更新docs/REVIEW再push。

正式路径不变；默认debug/release不初始化ConversationExport；实验设置需开始扫描；单向stable ID/overlap/拓扑序/冲突/稳定上限/恢复保留，文件Nova-legacy-scan和not-proven。旧Gecko PDF已在此前退役，仅实验HTML/MD；正式System Print PDF保留。详见../legacy-conversation-scanner.md，未来Archive官方导入仅规划。

### Step 4c：实际APK独立核对 success

已通过GitHub connector取回正式artifact11466649749、实验artifact11467092575（CLI token/下载失败由connector解决，不重新登录/不绕代理）。独立核ZIP digest与GitHub元数据、APK SHA与交付校验、APK v2 RSA签名及签名content digest、原证书指纹、四legacy+freeze assets逐字节等于4ca3c04源码、无native so。DEX BuildConfig.ENABLE_LEGACY_SCANNER正式false/实验true。正式1916121bytes/SHA a37d8cd6371591f263ac00dc13eb49283abc0e3adda45d0e933351d941af5eaa；实验1917341bytes/SHA 6856865eb9439088910a02625d7005f2b4f9be68ec54426a4f2c1cdab7e02e3c。

入库报告 ../../tools/legacy-scanner/evidence/package-verification.json。两套build/lint/unit/签名均success，四个Android jobs仍进行中（not verified），不能把包验证当作UI/恢复/正式保存通过。下一步继续读取Android结果，不公开Release、不改版本。

### Step 4d：旧文件语义断言同步

复查Android旧fixture发现一项仍断言旧“滚动收集并缓存可见历史”文案；本轮已更名实验记录，改断言Experimental captured message set/captureMode/historyCompleteness，保留40条逐字/同文不同ID/恢复/不完整性检查，没有删除或放宽。补白名单users/assistants数值，使复制diagnostic保留分角色计数，并在旧方向测试要求20/20与not-proven。4ca3c04 Android仍进行中，本小修需新CI，不据静态发现宣称旧运行实际失败。下一步读取最新两套CI。

### Step 4e：前次实际Android失败已核

读取4ca3c04实际job logs：Android35 job112682669293、36 job112682669294均执行19项、1项失败，均为historyScrollCachesUnmountedMessagesThroughSaf第152行旧文件文案断言；其余18项（含新确认/前台取消/脱敏）通过。两套run最终被新提交取消，不能标overall success。失败摘要入isolation-validation.json previousRuns，不删除证据。最新源码5d9efe8eee0c24d1ce2b1a48b6e14f74360ae583正式37588638317/实验37588638326正在重跑，尚未最终验收。下一步等待新Android结果并独立核对最终文件与APK。

### Step 4f：最终候选包独立核对 success

源码5d9efe8，正式run37588638317/artifact11467268360，实验run37588638326/artifact11467472980。实际APK再次独立核SHA、v2RSA签名+签名content digest、原证书、5份assets、无native so；读取实际DEX核EXPORT_REVISION=5d9efe8、原applicationId、正式flag=false/实验=true。报告package-verification.json；前包报告另存package-verification-4ca3c04.json不覆盖失败历史。

正式1916153bytes/SHA4f9212d7dca36cb1563b88acce17051c89afe03ae9cb3d692a03cc19eb469ea5；实验1917377bytes/SHA423270eda1b8811372867a5fbc0ed7c466e1241a36d60b08b2af75d305e66204。两套build/lint/签名success，四Android jobs尚进行中；本步仅包核对、未完成最终Android验收。下一步等待19项legacy/default2/正式保存与回归，核实际文件，更新最终交接。

### Step 4g：Legacy两档最终验收 success

最新源码5d9efe8的实验CI37588638326全success：legacy-scanner-tests/build/lint/browser/原签名及Android35/36。已独立下载两档artifact11467947062/11467608903，核instrumentation各OK(19 tests)，19个成功方法与2个Gecko明确退役项；文本证据和摘要入tools/legacy-scanner/evidence。旧16个+新确认/前台取消/脱敏3个全部通过，恢复滚动/缺ID/overlap/不稳定/progress/SAF仍通过。仅受控夹具，真实账号未证明完整。

正式CI37588638317的build已success，两档Android仍进行中，尚不能最终声明正式保存回归完成。下一步核默认关闭合同、HTML/MD/PDF实物及IME/upload/download/share/升级报告，再最终交接。

### Step 4h：正式API35通过，API36打印失败保留并重试

源码5d9efe8正式API35 job112685070124全success，独立下载artifact11467699020核checks无失败、默认关闭2/快照10/全部原web/IME/share/native/升级回归通过；实际HTML/MD首尾/变更排除、同打印源、Nova/Firefox两PDF各31页与文本标记独立通过。API36 job112685070153仅realSystemPrintUiSavesFrozenMultipagePdf等待native PDF filename失败，另9项快照/默认关闭2/其余正式套件与Firefox均通过；截图显示printspooler错误/Retry页，不能称PDF成功。失败原instrumentation/UI文本/截图与JSON保留tools/legacy-scanner/evidence。已只重跑该失败job，源码/断言不改，不擅自归为环境问题。当前HEAD e26d9ad；本步实际结果success/failure混合，下一步读取同源API36重试、核PDF实物再最终交接。


### Step 4i：正式API36同源重试失败，修复原生打印测试同步

当前文档基点610fb8708fac93d7334ca9d44292805d67673165；已测源码5d9efe8。API36 job112689954042/artifact11468372879第二次仍native PDF filename超时，failure已保留原测试/UI和摘要，正式整体failure不能验收。日志在07:59:20首轮write(1 range)后测试打开destination并重选默认PDF，07:59:23第二次layout、write(0 ranges)后spooler错误。原测试只等窗口包名就判断Save as PDF缺席，窗口存在不代表目的地标题已加载。新增等待spinner标题非空后才判断是否切换目的地，避免加载时误操作；不改生产SnapshotWebView/PageSnapshotExport，不跳过实际保存/31页/首尾/变更排除/Firefox断言。本步修复待新CI验证，尚非成功结论。下一步核新正式CI与实际文件；legacy应用/fixture不变，其19项结论仍属于5d9efe8。

### Step 4j：修复候选包独立核对 success

当前HEAD/新构建源码fbd7130a7656d4839c6a527d1e509dcc7b4193d6，正式CI37592089643 build/lint/browser/unit/原签名success；两档Android进行中。已独立下载默认artifact11468968104，ZIP digest匹配、APK v2 RSA签名与content digest验证、原证书/5份assets/无native so/DEX flag=false及revision=fbd7130/applicationId通过。1916145bytes/SHA44a337745c9a5c5c3e26bda1dc95f6807f847451600f186c8bc9a8460095046a。旧5d9两包报告另存package-verification-5d9efe8.json，实验包仍5d9（生产/legacy源码无变化）。本步仅包核对success，正式Android仍not verified；下一步核实际PDF和全部回归。

### Step 4k：修正过窄原生目的地等待（failure保留）

当前HEAD8e70aa1，fbd7130正式CI37592089643两API失败均在新增print destination populated等待，并非实际PDF内容断言。新等待错误假设标题有com.android.printspooler:id/title，初态实际Select a printer文本无ID；两档截图已有1/31与2/31完整分页预览。原instrumentation/UI/摘要分别保留stable-35/36-fbd7130-*。修正为等可见Select a printer/Save as PDF状态，再等真实分页预览page_number，才切换目的地，避免未生成页面即切换造成原empty-range路径；所有实际PDF/文件/内容/多页/对照仍保留，不改生产代码。新修正待CI，不能记success；下一步核新两API实际文件。此前“重选默认PDF”属初始假设，fbd截图表明未选择打印机，现修正为初始异步预览/分页未完成便切换的时序假设；Chromium公开源码已确认空页范围onWrite会失败，不擅自认定完整触发原因。

### Step 4l：分页同步候选包核对 success

当前HEAD3149507ef3fb88d66428dde610894ccac82cf44b，正式run37593888527 build/lint/browser/unit/签名success，两API Android进行中。已独立取默认artifact11470220775核ZIP digest、APK v2RSA/content digest、原证书、5份assets、DEX flag=false/revision3149507/applicationId、无native so；1916153bytes/SHAb35bed4d50df1a2b7bdeb50d902f7fba0274ddb533e4aaada90d5b4ad5498b96。此前包报告另存package-verification-fbd7130.json。实验源码/fixture仍和5d9相同，不重跑无关legacy；当前只包验证success，正式两API实际文件待核。下一步等待回归结果并独立核实物，不把预览截图当已保存PDF。

### Step 4m：API35无障碍框架查询失败留证

当前HEADbf54abb；已测源码3149507。API35 job112702223776/artifact11470687017正式job失败，realSystemPrintUiSavesFrozenMultipagePdf在line121等待PDF按钮时Android AccessibilityInteractionClient.checkFindAccessibilityNodeInfoResultIntegrity内部null List抛NPE，不是Nova应用正文/渲染异常。实际截图Save as PDF与1/31、2/31预览正常，无spooler错误，但没有保存Nova PDF，不能标success。原instrumentation/UI/JSON入stable-35-3149507-attempt1-*；其他正式测试继续执行。API36尚进行中，先核其结果，再决定同源重试35；不吞异常/不放宽PDF要求。本步failure，下一步核36并完成必要重试/实物验证。

### Step 4n：正式API36最终实物验收 success，35同源重试

当前HEADc3a4fad，正式源码3149507，run37593888527 API36 job112702223798全success。独立下载artifact11470851615核ZIP digest、checks无失败、17套/43个执行项（默认关闭2、快照10、Firefox1、web9、share5、input5、IME3、native2、两基线升级6）均OK；实际HTML/MD首尾/结构/安全过滤/后续变更排除，同打印源逐字一致；Nova与Firefox两PDF各31页，独立pdftotext/NFKC/长代码/长表格/首尾/320×120图片通过，Firefox粗体首页独立render/OCR确认，不借历史报告。报告stable-36-final-verification.json及默认关闭/快照原instrumentation入库。

只重跑API35失败的无障碍框架NPE job，源码/断言不改；API35原failure不改成success，仍需新实物验证。当前总体正式尚未全绿。实验5d9两API19项已通过且生产/legacy源码不变，真实账号/物理设备未测。下一步核35重试，完成最终文档及推送，不增selector或发布Release。

### Step 4o：最终验收与交接 success（受控）

当前文档基点1682ecf，最终正式源码3149507；35同源重试job112707846774成功，run37593888527 attempt2 overall success。独立取35 artifact11470933678复核checks无失败、17套43项及实际HTML/MD/Nova与Firefox各31页PDF，和36结果一致；31页/首尾/长代码长表格/图片/Unicode/晚变更排除/OCR同打印源全部核对。35初次NPE仍failure留证，不覆盖。实验5d9各19通过且应用/legacy源码与314一致，18项隔离要求在代码/受控夹具范围确认；不证明真实长历史或真机。最新JSON、legacy docs、README和本交接/REVIEW同步完成；下一步真机手动反馈，保持三层边界，不继续selector军备竞赛。本提交仅文档/证据，skip ci；完整最终HEAD从git log读取，推送至feature/export-conversation，无产品发布。

## 此前 Frozen Snapshot 实现与验收（历史）

用户最终要求普通HTML/Markdown/PDF，取消Share/MHTML/新完整历史算法。正式“保存当前网页”→一次evaluateJavascript同步深clone→同clone静态HTML和Markdown→不可变FrozenPageSnapshot。HTML为唯一标准表示；PDF仅同frozenHtml→独立无JS/static WebView→Android System Print。Nova860px阅读/白底print CSS，不复制ChatGPT SPA。无滚动/backend/private reader/React/token/storage读取/ID去重/跨窗口缓存，不重构输入上传下载分享。

阶段A保留Gecko完成实际受控Nova/Firefox×Android35/36 PDF保存/独立重验后，阶段B移除GeckoPdfExporter/唯一旧PDF引用/dependency/仅Mozilla仓库/许可资产/lint例外/专属ABI和650MB交付。保留旧dom/scroll/progress/locator源码及证据，正式菜单不调用；16旧Android例继续，2Gecko专项明确Ignore保留历史。Firefox/OCR只CI，不加入生产。

## 验收与证据

最终CI https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37556129018 全绿：build、Android35、Android36均success。两API小证据下载并独立复核，四份PDF均31页Letter；最终API36 SAF HTML三浏览器file打开通过。实际HTML/MD/PDF、同snapshot live mutation隔离、唯一打印源与Firefox源逐字节相同、标准PrintUI保存/取消/多次/生命周期、旧容器/权限/上传/下载/blob/Share/所有IME/native及A大包→B/v1→B覆盖升级均有受控测试。具体结果和13项限制见docs/frozen-page-save.md及tools/snapshot/evidence。不是物理设备/登录真实账号成功。

最终主APK1910773字节(1.822255MiB)，SHA fa96b8714047ae707bbb013be8473be98a51db92cd826c4652f7ebe0d008e5c5；原包com.example.chatgptnova、versionCode14/1.4.0-scroll-trial、cert f93221ee0d2be2b806233a0b3427ec9c14766100c1bab3208841e6203e2b4289。A7 universal681117497字节；B无native so、Gecko Dex/deps，同一包支持ARM64等原架构，不假造split。总安装占用未知，APK不包括共享System WebView/应用数据。独立主APK artifact11454731981，不混测试APK，无公开Release。

历史CI A7(37552313716)原host首标题文本误判仍红，四实物独立NFKC/必要Firefox首标题144DPI离线OCR+人工图像通过，android-pdf-firefox-A7.json保留原失败。B1 API36仅旧菜单预期失败、其余全部通过；B1 API35取消不记通过。最后只同步4个菜单预期，账号设置/清理断言保留。不得将历史红/取消改称绿。

## 剩余边界与继续工作

真实账号与物理设备、厂商WebView、相机/麦克风硬件手动验收尚未执行；用户ADB条件未知，不反复追问。用户可以直接同签名覆盖安装测试APK核当前加载内容/普通HTML/系统打印质量。不继续追求无法证明的完整历史或自动Share。

DOM100000元素/12MiJSON硬限，无静默截断；SAF后台UTF8/64KiB流式复制/wt截断旧目标、非空长度核对，取消/destroy释放。S01–S10脱敏diag不含正文/真实ID/URLquery/凭据。外链图片字节非冻结/可能需网络与变化；blob/附件原文件/canvas/shadow/runtime未备份，inlineSVG移除，数学可读/公开LaTeX非像素级。静态资源无Cookie/Auth、有数量/大小/时间上限。

保持原签名/包名/版号；不main/forcepush/公开Release。必要源码/测试/文档可提交push本分支。每完成一个工作步骤都更新本交接，验证结论或边界变化时同步REVIEW，并随步骤提交push本分支；所有后续接手AI必须执行。仅实际压缩/迁移或用户明确要求时提供下一会话提示词，普通回复不生成。/tmp/nova-stage-b.py已应用勿重复执行；CI证据永久描述在Git，原artifact有期限。

三种格式严格对应同一次当前网页冻结快照，但无法证明ChatGPT服务器端完整会话历史；如果网页虚拟化未挂载较早内容，保存结果也不会包含那些内容。
