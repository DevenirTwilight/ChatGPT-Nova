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
