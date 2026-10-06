# 最新复核摘要

## 普通 HTML Commit A 已实现，验证待CI

基于实际远端最新源码c28947b，已将PageSnapshotExport的MHTML保存路径彻底替换为普通UTF-8.html直接SAF；唯一FrozenPageSnapshot含final HTML/MD/字符数与复制型diagnostic metadata。freeze.js单次同步公开状态+clone，阅读主体使用Nova内联样式，不复制ChatGPT CSS/脚本/控制/原属性；同clone派生Markdown，PDF打印同份frozenHtml。100000元素/12Mi UTF16 payload硬上限，明确S01–S10错误，新路径无旧H系列。相对资源同步绝对化，data图片保留，blob明确占位，外链图片并非离线备份。用户/助手标签只取公开角色，保留此前无普通作者的明确conversation-turn助手正文修复。

本机浏览器12场景通过（新增角色/pre-wrap、100+屏fixture），旧17单DOM通过。当前工具compatibility.cjs实际运行Chromium/Firefox/Edge阅读和print CSS检查，结果需核对；云Chromiumfile://若被运行时策略阻止，会明确记录localhost原文件服务作为渲染验证，不能冒充无条件file打开成功。实际Android保存HTML和PDF待最终CI，Firefox Android A/B仍待真实验收。

新Android10测试取消MHTML，改实际HTML/MD字节与同对象打印；静态renderer等待visual state再启动打印。上轮PDF打印界面实物显示Sorry, that didn't work（不是只找不到Save），因此去掉测试中在预览写入期间不必要的打印目的切换，保留失败实物与适配器layout/write生命周期记录，继续核对真正PDF。新回归脚本补明确CAMERA/RECORD_AUDIO权限前置（之前未授予造成两个超时），不改生产上传/媒体实现。保留全部旧18/回归；单元任务无JVM用例时NO-SOURCE。

CI构建/lint/Android35/36及实际PDF通过前不移除Gecko，不把桌面PDF冒充Android，也不把未完成FireFox人工对照称通过。旧复杂历史扫描/Gecko仍保留源码供对照，无Release/版号变更。最新任务与下一步均在本段，下方Shared/MHTML段为已终止历史。


## 当前任务：普通静态 HTML 为唯一快照表示，取消 MHTML / Share（2026-10-06）

最新用户重新指定当前网页一次同步冻结→同clone派生HTML/Markdown→PDF打印同份frozenHtml。停止Share和MHTML，不扩展旧滚动/私有来源，不声称完整服务器历史。Commit A保留Gecko，Android35/36及真实PDF验证通过后Commit B才移除生产Gecko/切正式UI。当前源码基线c28947b，远端文档HEAD13dedf9；勿执行下方被替代任务。

Share实验已证实虚拟化，保留tools/shared-source脱敏证据作为结论而非新功能：移动/桌面顶部8节点5411字符26代码块、底部6节点1866字符0代码块；顶部第一个节点isConnected=false且其正文不在底部任何作者节点。桌面有用户提供首标记在顶部、底部消失，尾标记两端存在（不证明全量），另一次移动bounded稳定SHARE_NOT_STABLE。未读存储/私有API、不存链接正文、不创建分享。明确停止该主要来源路线，不做滚动workaround。

前一A源码c28947b CI37534482218构建/lint/原签名通过、Android35/36旧18通过、新10各9通过/真实PDF保存按钮超时；有camera callback/media permission回归失败，尚未全通过。当前普通HTML A将替代MHTML代码和测试；保留旧失败证据，不把预览字节/回调当成实际保存PDF。新任务需修系统Print UI验收并重跑完整回归。


## 当前任务已改为官方 Shared Conversation 可行性验证（2026-10-06）

实际远端 HEAD 起点043642448b5eb56c3bd3135ef8e2a26c2c6b2485，应用源码c28947b。最新用户指令替代下方“当前网页PDF/MHTML/Markdown Commit B”计划：先验证SharedConversationSource，真实长会话覆盖、同share URL更新、虚拟化实验通过后才升级主要来源；失败停止workaround。保留旧代码与Gecko，不继续扩展旧主DOM滚动算法，不发布Release、不增版。最终目标为同一次shared DOM clone派生普通HTML/Markdown，PDF打印相同frozenHtml的独立静态WebView。

用户已提供一条官方共享链接并同意读取；不把链接或正文提交仓库。独立Chromium已HTTP200并识别6个user/assistant节点，尚待真实总数及首中尾基准。初步document滚动不是有效实验（页面实际为内层滚动），正在针对真正scrollable ancestor核对。是否为长会话、完整覆盖、同URL新增后重新Share更新均未证明。不要把Log in按钮误判成登录页；已渲染公开消息的share页可以有该按钮。

上一任务CI37534482218构建/lint/签名通过，但Android35/36总体失败，需检查产物checks.json和各独立测试；不能写成全通过或删除Gecko依据。本机无Android SDK/adb，真实用户WebView153及人工PDF/Firefox仍待验证。

已新增tools/shared-source/probe.cjs及shared/probe.js可重复运行公开页结构/稳定/滚动实验，不存URL/正文/消息ID，无内部API、账号存储或旧扫描fallback。Android独立WebView计划使用androidx.webkit1.12.1公开MULTI_PROFILE API建立临时独立profile避免影响主页面Cookie；不支持时明确失败，不改global CookieManager。官方Share只能在用户明示告知公开链接风险且继续后辅助打开UI，不自动创建/更新链接。

下一步：确认真实基准与同URL更新；完成独立SharedSnapshotWebView稳定检查/临时profile销毁、可见Share DOM URL读取或主动粘贴入口，受控fixture检查。只有真实验证适用才推进FrozenSharedSnapshot三格式及正式入口，保留历史证据。下方旧任务记录为历史，不是继续执行Commit B授权。


## 浏览器式冻结快照 Commit A：源码 c28947b，最新完整验证进行中

用户新目标为 PDF + MHTML + Markdown，不再扩展完整历史/自动滚动算法。实际起点远端2b86b68；原型363cb69及后续集中修正至c28947be326755511deaca3155f6e5e27d07b9a8。保存当前网页（原型）→一次evaluateJavascript同步读取公开呈现状态、clone→同clone派生frozenHtml/markdown→不可变FrozenPageSnapshot。Markdown直接SAF，MHTML独立无JS SnapshotWebView.saveWebArchive，PDF相同renderer标准PrintManager。已移除clone脚本/事件/执行容器、SVG脚本/动画，CSP禁执行、导航与桥隔离；静态资源无Cookie/Auth加载，拒绝backend API。当前仍保留生产Gecko与旧入口，原型PDF本身不用Gecko。版本仍1.4.0/code14，无Release。

[说明/人工验收](../frozen-page-save.md)。本地新快照10浏览器场景、旧17单次/32滚动/19进度/18定位均通过。CI37532352447首次测试编译失败（打印回调非公开构造器、误用私有focus），已改为真实系统Print UI。CI37532804939源码4778c92构建/lint/签名通过，Android35/36旧18项通过、新10项各9通过/1失败：真实PDF测试未打开保存选择器，control Save超时；日志显示17页预览已生成82680bytes，不能当已保存PDF。已修正大小写/描述匹配、明确等候打印目标popup防过渡误点，增加失败UI实物保留。不要把这些失败写成最终通过。

最新CI[37534482218](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37534482218)为c28947b，正在验证：snapshot10项、真实MD/MHTML/PDF文件、MIME解析、Chromium打开MHTML、旧WebView9/Share5/ClipboardUi5/ClipboardProbe三项完整矩阵/native生命周期及重启/原签名v1升级三步。每个独立检查失败仍继续并写checks.json；unit任务仓库无现有JVM用例时为NO-SOURCE，不声称额外单测通过。Gradle本机wrapper初次下载因Java未使用代理失败，实际构建依靠CI。截图与字节内容需下载产物后独立核对。

待A全部受控检查通过后才做Commit B：正式入口改保存当前网页，移除生产Gecko依赖及GeckoPdfExporter引用，重测/测APK/原签名/交付。可保留旧DOM扫描源码为不在正式UI的legacy，继续旧非PDF检查；若退役旧Gecko PDF两测试，明确标注理由、保留测试与历史证据，不暗删。勿重写ComposerWebView/上传/下载/分享。当前所有生产功能未撤掉Gecko，不要提前报告小包已交付。

远程CSS/图片字节不冻结且可能缺失，canvas/shadow/runtime不序列化；图片srcset目前移除、保留src，后续可评估公开currentSrc但必须仍在同一同步事务中，不能后续读live DOM补内容。真实账号/用户WebView153、人工PDF阅读/Firefox Android同fixture对照尚未完成；三格式只证明此刻已加载网页，不恢复虚拟化未挂载历史，不证明旧403修复。

本地证据：/tmp/nova-snapshot-A-35、/tmp/nova-snapshot-A-36为旧失败轮实物日志；API36产物11446165241/API35产物11446245308，旧构建11444773611（309MB wrapper；当前保留Gecko所以巨大，不需重复直接下载大包）。GitHub下载工具返回file_id后用download_file获取，小证据包约4MB；不要打印临时签名download_url。后续移除Gecko的主APK应直接可下载。CLI gh可读run/artifacts，但写Git对象采用GitHub create_tree/create_commit/update_ref expected_sha/force:false，核对远端、树与本地相同再同步，不强推。

继续指令：读实际远端HEAD与AGENTS/本段/REVIEW/冻结说明，等待并检查37534482218全部结果；失败则修复重测，未通过不切B；通过后按上述B完成生产轻量化、全回归和真实文件独立检查，给测试APK/CI与准确边界，不发布Release。需要压缩/迁移继续入库交接，不为普通回复生成迁移提示词。

## 功能与实现审查文档已整理；Gecko 轻量化尚未实施

用户现要求整理软件全部功能及实现方式，交给其他 ChatGPT 评估改善。[独立审查资料](../APP-FUNCTIONS-IMPLEMENTATION-REVIEW.md)已按代码 eaeadce、实际 APK 源码190113f整理：原生容器与官网功能边界、登录/输入/文件/媒体/分享/导航、三格式与诊断、构建包体、源码链接及审查指令。本轮只改文档，无新 APK、应用源码或版号变更。文档源码路径存在性及关键版本/包体/验证状态已核对，纯文档未重跑应用测试。

用户质疑170MiB仍比Firefox大。当前聊天仍WebView、Gecko仅PDF；ARM64 Gecko原生文件约153.9MiB，ABI分包无法消除引擎成本。已提出撤掉专用于PDF的Gecko、评估复用系统WebView打印的方向，但尚未改代码，也不能默认系统打印可等价静默生成并直接SAF保存。请先审查轻量方案，保留HTML/Markdown/PDF及原容器能力，集中修复不频繁增版。原进度最终复测、真实历史not-proven、附件原件未含、旧403未复测等边界不变。

## 手机首选改为ARM64包：170MiB APK / 81MiB ZIP，版本不变

用户质疑650MiB是否等同Firefox大小。独立检查旧通用包未压缩文件：公共22.8MiB，ARM64 153.9、ARM32 120.7、x86 185.8、x86_64 172.5MiB，其他架构贡献约479MiB。Mozilla官方版本metadata为157.0.1，[官方ARM64 APK](https://archive.mozilla.org/pub/fenix/releases/157.0.1/android/fenix-157.0.1-android-arm64-v8a/fenix-157.0.1.multi.android-arm64-v8a.apk)HEAD Content-Length134275592（约128MiB）；不能把Nova四架构通用包称为Firefox手机版实际大小，也不把Firefox安装后含缓存的占用与下载APK比较。

[ARM64交付CI37512229753](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37512229753)通过：使用此前已验证37509953925原主APK，仅保留ARM64库与公共内容，去旧签名、zipalign16KiB、原证书重新签名；apksigner/zipalign验证通过。没有重新编译/改源代码/增版，诊断buildRevision仍190113f、1.4.0/code14。此前Android35/36各18项是在通用包的x86_64引擎上执行，不冒充ARM64真机测试；ARM64本机运行仍待用户安装。这一交付提交是打包流程，不是新应用源码提交。

本地独立取回3个小部分，核对wrapper和分块摘要、完整ZIP摘要、APK校验及原证书记录，逐字节核对172个保留文件（含所有DEX/资源/ARM64库/脚本/许可），只剩arm64-v8a且DEX完整190113f。主APK178491277 bytes（约170MiB），SHA256 `7259f4704ca30e70dafa735ff5a832e92140b8a8533d41ae2ac356b99d8495c6`；ZIP84711177 bytes（约81MiB），SHA256 `7cd8e3031e9674af224bbb66fca5d86d4fbe347fb3eb12e166c391882ab51ea0`。会话直接提供ARM64 APK/ZIP，均无测试APK；[GitHub备用ARM64产物](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37512229753/artifacts/11435138812)需登录解压。原650MiB通用包仅为其他架构备用，不再作为手机首选。不创建Release。

PDF/HTML/Markdown以及独立助手turn补采完全保留，历史完整性/附件原件/旧403边界不变。后续手机交付优先按ABI分包，不重复把模拟器架构发给手机；打包后需沿用相同来源/字节/签名核对。单向跨窗口缺ID仍H02，原会话缺失进度的内容及位置仍须本批快速导出复测。


## Gecko三格式批次已通过并交付：190113f / 1.4.0

APK源码`190113f9387cd55e9dc3eb816a57d666840a9b03`，[CI37509953925](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37509953925)构建/lint/原签名、浏览器17单快照/32滚动/19进度/18定位，以及Android35/36各18项全部通过；publish skipped。此前751da199首次因误删View导入编译失败，20bc9ae随后因Gecko捆绑未使用的NotificationUtil lint失败，均已修正，未把失败轮次记作通过。lint例外仅按该第三方类具体消息匹配，应用自己的权限检查继续；无新增通知权限。

PDF现在使用Mozilla官方GeckoView140.0.20250707120347的saveAsPdf（固定兼容compileSdk35），渲染本地净化导出HTML，生成/验证后直接进入SAF保存位置选择；可取消生成和保存、重试，有G01–G08错误诊断以及pdfEngine/gecko/pdfPages/bytes。HTML/Markdown保留，当前无作者独立显式助手turn补采一起交付，不按文本去重。Gecko只用于导出，聊天与登录仍原WebView。独立只允许已标助手conversation-turn或旧article，无标角色/隐藏/UI候选不补；未知channel不假称进度；缺ID单次保留、跨窗口仍H02。用户原会话12普通作者+1明确助手turn的漏采机制已有真机证据，原目标内容/位置恢复仍需本批原会话复测。

独立下载两套Android证据确认OK(18 tests)，包括Gecko直接SAF取消/重试、多页保存，以及缺ID助手SECTION经HTML保存的顺序/诊断脱敏。实际PDF各99166 bytes、5页Letter，Producer cairo1.18.0；首条→PDF-PROGRESS→末条顺序与代码/表格标记均检出。API35 SHA256 `eb06dba591849763dd46015d7b7191c40badabcc303db9d31a61197bc8c2effb`；API36 `90fb9e637a66d3345b34ad1728534a17793f13149fe73adaa1af8468f8507c20`。这是模拟器受控文档，不是用户153网页或完整真实长历史实物证明。

主APK681095398 bytes（约650MiB，包含ARM64/ARM32/x86/x86_64），SHA256 `4637b9ae1b4862388d5c12a9833730a98584df9cbc0c9e7e8da63427f5f54fd6`，原证书签名，仍1.4.0-scroll-trial/code14。独立核对分块wrapper和内容摘要、重组ZIP摘要、APK与原CI校验相同、manifest包名/版号/无debug、证书记录、4份JS与源码相同、MPL资产、DEX完整190113f及saveAsPdf、4架构libxul、无测试夹具。[GitHub原构建产物](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37509953925/artifacts/11434676119)需登录/解压，仅安装ChatGPT-Nova.apk。

会话直接提供已组装主APK及ZIP（300645094 bytes/约287MiB，主APK/校验/manifest/签名，无测试APK）；ZIP SHA256 `956b4ba080c463b05467c1eeb70e507d74649344110c9f13db30868eaed62371`。没有公开Release。文件通道32MiB限制与直接shell下载403通过[交付工作流37510858526](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37510858526)解决：从原已签APK包生成10个30MiB以内部分，经授权文件工具取回并逐段核对后组装，不重新构建或改APK。交付提交不等同APK源码；当次APK仍190113f。工具只传交接在GitHub，不提交APK/私密会话/登录资料。

复测：覆盖安装→原会话显示缺失进度→导出聊天历史（试用）→快速导出已加载消息，分别检查HTML/Markdown/PDF正文与前后顺序；PDF生成后直接选择保存位置。PDF诊断应pdfEngine=gecko/gecko=140.0.20250707120347/buildRevision=190113f，可发脱敏JSON。不要仅凭条数判完整；当前count受挂载范围影响。真实完整历史not-proven、原附件未打包、旧403未复测，试用实现与格式集成检查完成，原会话进度和覆盖尚待用户复测。下方为早期阶段记录。


## 用户授权Gecko导出：三格式保留，批次待构建

用户要求参考Firefox保存PDF并选择引入Gecko、接受大包及更多改动。现在PDF引擎改为官方GeckoView140.0.20250707120347（固定兼容compileSdk35），调用Firefox相同公开saveAsPdf接口，净化导出HTML→私密无JS会话→PDF流→后台写缓存/PdfRenderer验证页数→直接SAF保存，不再打开PrintManager打印机界面。HTML/Markdown保留，独立无作者助手turn补采一起交付。聊天WebView、登录与输入组件未迁移，不把PDF渲染器换成Gecko当历史完整性修复。

Gecko仅加载本地data HTML，原导出CSP拒绝外部资源，关闭JS/调试输出，结束/失败/销毁关闭会话，异常分类G01–G07和engine/version/pages/bytes诊断无正文。增加依赖官方Maven及MPL许可/来源文档。版本仍1.4.0/code14，不公开Release。Android两项PDF检查替换成直接SAF取消/重试和多页实物、包含缺ID进度；脚本预期18项并检查PDF-PROGRESS。

旧37505642553环境卡住已取消；37506735779构建/lint/签名成功，但Android35虽然18项通过，脚本仍要求旧17导致失败；Android36另有旧htmlSaved测试JS回调超时，新助手SECTION测试通过。以上不写成全CI通过，本批修正脚本并移除系统打印UI路径后重新验证；旧f943c83包未交付，下载核对的APK1896898bytes/SHA037754bdfc0748133fbafe5f89dfc098ea6db6c12e55d3a01f3d9c9da0730693不是Gecko批次。用户完整历史not-proven/附件原文件不含/旧403未复测。


## CI环境安装停滞，重试构建

源码29d3c49的本地17单快照/32滚动/19进度/18定位检查通过。CI37505642553在setup-android停滞超过7分钟（上次该步27秒），尚未编译；两次取消API返回502。工作流改用ubuntu-latest预装SDK，先检查sdkmanager/adb存在并补PATH，再确保所需platform/build-tools，分别2/5分钟超时。版本与采集源码不变；新推送触发替换运行，旧运行不算验证成功，待新CI证据。


## 真机明确助手turn无作者：有限补采批次待CI

e56c225快速采样N00_READY，总224ms；12普通作者均采集（6用户/6助手），13个可见turn，其中domIndex3 declaredRole=assistant/authorNodes0/visibleTextNodes4/visibleTextChars124；无截断。结合上一条定位可见作者外Markdown，明确旧采集器只补article导致这类独立助手会话容器漏采。其他12个turn外侧文字83字符大多署名/UI，不自动采集。12层定位未显示第3个turn标签，不能声称已拿到完整原始DOM或真实ID。

本批将有限fallback扩至带conversation-turn testid且data-turn=assistant的容器，仍排除隐藏、自身/后代含作者、嵌套容器与未知角色，保留原article兼容。滚动容器选取及装饰spinner排除、文字定位来源同步同一范围。未知channel仍assistant-unknown，不猜commentary；缺ID单快照保留正文及警告，跨窗口滚动仍H02，不伪造ID/按文本去重。版号保持1.4.0/code14，未发布Release。

新增浏览器检查覆盖无ID正文顺序/控件排除、验证签名覆盖新增正文、DIV/SECTION标记容器、未知角色/无testid/隐藏拒绝、正常作者不重复，以及滚动已知ID/缺ID、定位与实际转换对照。Android新增无ID助手SECTION经快速入口→SAF保存→诊断脱敏及顺序检查，CI待执行。用户原第12条实际正文位置仍须新批次复测，不仅核对count，完整历史仍not-proven。历史section-fallback脚本固定读取e56c225旧源码用于前后对照，不冒充当前APK测试。


## e56c225真机L00：可见作者外Markdown遗漏，外层turn待确认

用户短片段定位返回L00_MATCH，总153ms，作者raw12、703文本节点/6736字符、未截断。3处匹配中第1处authorIndex0/role absent/selectedfalse，aria-hidden/hidden/CSS隐藏均false；STRONG→P→Markdown DIV后多层DIV，12层内无角色/turn/channel/ID/testid。其最近作者查询不受12层限制，因此确实位于作者容器之外。另两处属于已选助手作者4/12，其中第12处为表格引用；本地转换D00/count12/matchingMessageIndexes[4,12]。支持可见作者外正文漏采机制，不把后续引用当原进度或按文字去重。

第2处普通助手的外层为SECTION，data-turn=assistant且有conversation-turn testid；第1处链条截于12层，尚不能断定其外层同样SECTION、独立turn还是含普通作者的兄弟。exportSourceHasIdfalse只查询当前作者/article候选，不证明更远SECTION无ID。不能将所有无作者Markdown直接视作助手，亦不能据count12证明完整历史。

已请用户保持目标位置，在现有e56c225包快速导出已加载消息后取消格式选择，再复制导出诊断；v2的turnSamples/outsideAuthorSamples可补外层角色和作者兄弟归属，不需重新安装/保存HTML。等待该结果后集中精确补采。独立新增section-fallback.test.cjs，5项工具内方案检查通过：当前漏独立显式助手SECTION；扩展候选后采集；未知角色/隐藏SECTION不采；含普通作者的SECTION兄弟正文仍漏。仅合成假设验证，未改生产源码或新增APK。缺ID的quick snapshot保留警告，滚动仍H02，不能伪造跨窗口身份。修复与真实完整性验证尚未完成，旧403及附件原文件仍未验证/未包含。


## e56c225真机定位L01：本次raw作者6，上次31；输入114字符，原因未定

用户已实际运行e56c225/Android36/WebView153.0.8010.36定位，L01_NOT_FOUND/text-located，总93ms；locator.matches空、authorNodes6、visitedTextNodes633、scannedChars4833、elapsed77ms、truncatedfalse。对照此前8ed24f7的31个作者，两次挂载范围不同；新authorNodes只是原始角色节点计数，不能当完整历史消息数。此结果没有匹配，未执行后续本地转换对照；不能把它当捕获错误或证明进度不存在。

用户随后提供输入的是完整目标进度句；本环境仅核算UTF16长度114，符合4–160上限，没有将完整私密句子写入仓库/脚本。确实是长精确匹配，标点/空格/跨块结构差异可能导致无匹配，但尚无真实DOM证据，不能断定是唯一原因。用户此前确认页面能看到目标；这次尚未回答打开定位前是否正显示它。已请其沿用现有包，在原会话先把目标滚到屏幕上，只输入短独特片段并复制JSON，同时说明当时可见性；不要求新安装/保存HTML/ADB。

独立新增tools/feasibility/progress-coverage/locator-window.test.cjs，5个只读合成检查通过：31个作者+作者外目标可定位且不在转换结果；只挂载末6个作者时L01；挂载包含目标的6作者窗口时L00；目标已挂载但输入错1字符时仍L01；目标位于可见button时当前locator按控件排除仍L01。说明相同6作者/L01不能唯一归因虚拟化，当前诊断还有控件匹配范围限制。以上是对照/边界，不是真机根因复现。未改生产源码、未新增APK/版本/Release；遗漏仍待实际短片段匹配结构，若确认为控件/跨块或原生输入过程更换挂载窗口，再集中改定位/提取逻辑，不盲目放开所有可见文字。


## 定位诊断包已验证并交付：e56c225 / 1.4.0

最终源码`e56c2250719b32d3212683bb96e5240e35a5ad5d`，[CI37495451307](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37495451307)构建/lint/原签名、Chromium17单快照+30滚动+12进度+16定位场景、Android35/36各17项全部通过，publish skipped。本地v2结构探针14场景和canonical探针10场景通过。未改变消息采集规则、放开隐藏导出或增加滚动轮次。本批新增定点定位/本地转换对照与v2结构诊断，不宣布用户原进度已修复。

独立下载35/36证据核对`OK (17 tests)`及新增`locatorCopiesOnlyStructureForVisibleAuthorSibling`：页面显示作者旁目标→定位→原生复制JSON，authorIndex0、CSS非隐藏、不含目标字串、不写文件；系统PDF各4页且包含首尾/代码/表格标记。API35 140385 bytes/SHA256 `5e87f0557740e5c06b9d3254922d3541ae61f34a1ed85dcc350f685fe04b8fb2`；API36 141613 bytes/SHA256 `b916ca6edda6224174354f83364c69f05bd82401093d289b19b6bf232089b3c8`。模拟器WebView124/133，不是用户153或真实长会话证据。

主APK1896838 bytes/SHA256 `cc869e0f36aee65670eae721b2d2026bd072629e84dfc8eeea14c482f1e4de1b`。独立核对下载ZIP摘要、APK校验、manifest版本仍1.4.0-scroll-trial/code14、原证书记录、4份导出/定位脚本与源码逐字节一致、DEX完整buildRevision=e56c225、无测试夹具。会话直接提供主APK和仅主APK/校验值ZIP，不创建Release。[备用GitHub构建产物](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37495451307/artifacts/11427666558)，需登录/解压，只安装ChatGPT-Nova.apk，不装ChatGPT-Nova-tests.apk；有保留期限。

用户复测无需保存HTML：安装本批包，原会话先显示目标进度→右上菜单导出诊断→定位缺失文字→输入独特4–160字片段→定位→复制诊断。应为buildRevision=e56c225，locator.source=read-only-text-locator；重点看matches的authorIndex/authorSelected/visibility/insideMarkdown/chain、exportSourceHasId/capturedHere与capture.code/count/matchingMessageIndexes。明确消息ID对应的本地转换正文存在片段，才能支持“已包含在某条消息内”；没有ID的capturedHere=false或L01_NOT_FOUND不单独证明消息丢失，查找不含控件/iframe/闭合shadow等范围。多个匹配可能为后续正常回复引用了原句，不按查询文字去重或把引用当目标原消息。

真实8ed24f7 JSON已核实31普通作者全部采集、turn探针0，采集/复核签名一致；目标进度页面直接可见，但未知为何未包含/是否共享消息容器。旧30条基准与新31采样非同一时点，不能据条数宣布完整或修好。下一步等待用户本批定点结构/转换对照结果，基于实际标记修正；完整历史仍not-proven，附件原文件未打包，旧403未复测。下方“待CI/未接入APK”为本批交付前的记录。


## 8ed24f7真实JSON已收到：31普通作者均采集，turn探针零命中；新增缺失文字定位批次（待CI）

用户提供8ed24f7/Android36/WebView153.0.8010.36的真实诊断：N00_READY/snapshot-ready，总463ms；作者raw/supported/visibleSupported均31（16用户/15助手），processed31、缺ID0/重复ID0，turnFallbacks0/explicitProgressBlocks0，Markdown外文字0，article turn探针raw0；采集/复核signature均243269ab。正文过滤ariaHidden33/displayNone8/controls23不是33条消息，更不能直接定位目标进度。用户已确认目标进度在Nova页面直接可见，但未保存新HTML。31条属于新采样时点，不能直接与旧30条基准相减或宣称完整。已核实安装提交；遗漏仍未解决，原始外层DOM/过滤原因尚未知。

本批保留采集器/滚动次数与版本1.4.0/code14，新增原生“导出诊断→定位缺失文字”：用户在当前页面显示目标后输入4–160字独特片段。只读查找当前加载正文，支持同块内跨inline节点、空白归一，报告数字作者序号、白名单祖先标签/角色/turn/channel/有ID布尔、aria-hidden与CSS/hidden属性分开；不返回输入文字、正文、真实ID、URL。定位同时在WebView本地运行当前转换器，输出capture.code/count/matchingMessageIndexes及每个匹配的capturedHere/exportSourceHasId；用于区分文字已经在某条导出正文内、同一作者Markdown外、作者旁边、可见性规则过滤，而不靠count推论文字缺失。无稳定ID的capturedHere=false不能单独当遗漏证明；须结合matchingMessageIndexes/结构/捕获错误。

定位预算：50000文本节点、200万扫描字符、2秒查找、20匹配/每条12层白名单结构、15秒原生回调。查找包含隐藏文本以诊断过滤，但不会放开导出过滤/保存隐藏内容；按钮/脚本/输入/iframe等不查，L01_NOT_FOUND仅当前范围未匹配。局部转换沿用原1000消息/200万字符/8MiB界限；不调用网络/React/Cookie/storage，不滚动、不改网页。v2结构探针接入assets，turn诊断去除article限定，补作者外兄弟文字计数，候选仍不自动导出成助手。v1安装包缺上述功能。

新增16个Chromium定位场景通过，包括已采集正文对照、兄弟/Markdown外文字、aria-hidden但CSS可见、隐藏CSS、跨inline、按钮/输入排除、无匹配/超限/错域/JS字符串注入防护、只读和输出脱敏；v2结构诊断14场景通过。Android新增定位→原生复制JSON不含目标正文/不写文件，目标35/36各17项；CI尚待执行。原会话实际定位结果仍须用户使用本批测试包反馈，不再凭未核实选择器宣布根因或自动拆出消息。


## 用户仍报告缺第12条；页面直接可见，尚未提供本次采集诊断

8ed24f7交付后用户回复“还是缺那条”，追问后回答“没导出呢，能”：尚未保存新HTML，Nova原网页能直接看到目标进度。不能再把网页已不保留当默认解释，也不能将这一反馈记成本环境独立解析新HTML或已核实安装buildRevision的结果。原问题仍未解决；已请用户在现有包快速采集后取消格式选择，再从右上菜单导出诊断复制完整JSON，不要求保存文件/搭建ADB/重装。

独立源码复核找到现有诊断盲区：v1遇到turn含普通user/assistant作者节点就跳过整个turn，因此作者外、turn内的进度兄弟节点不会进入outsideMarkdown或无作者turn统计。该盲区已由合成结构复现，不能宣称就是实际第12条的DOM。新增工具目录内的probe-v2.js及13场景检查，统计withOutsideAuthorText/outsideAuthorTextNodes/outsideAuthorChars/outsideAuthorSamples，并修正turn自身是作者时的统计；隐藏文字/按钮仍不算可见候选，诊断无正文/真实ID/URL。候选也可能为UI标签，不能自动当助手进度。

v2尚未接入APK，app内仍是8ed24f7/v1，probe.js保留与已交付asset逐字节一致，既有产品检查不因此变更。13个只读合成场景独立通过，无生产源码修改、无新APK/版本/Release。先核对当前JSON中的buildRevision、作者/turn数与Markdown外文字，再决定下一批结构采样/精确提取；用户JSON未回复前，不盲目拓宽正文或制造分片ID。


## 本批已验证并交付：8ed24f7 / 1.4.0（进度补采、诊断、快速入口）

最终源码`8ed24f74d0f6fc8f4183e0b797a199926300b5e9`，[CI37487134618](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37487134618)构建/lint/原签名、Chromium17单快照+30滚动+12进度场景、Android35/36各16项通过，publish skipped；本地10个只读探针场景也通过。早先37486497116/37486763034因本批修正被取消，不算完整验证结果。新增Android测试实际通过快速入口→HTML→SAF保存并核对USER-FIRST→PROGRESS-BODY→ASSISTANT-LAST顺序、助手进度标签、复制诊断count3/turnFallbacks1/原作者2及无正文/ID泄露。均为明确标记的合成结构，不是用户缺失条目的真实DOM复现。

独立下载35/36证据核对`OK (16 tests)`及新增进度测试名称。系统保存PDF分别4页且包含首尾/代码/表格标记：API35 140385 bytes/SHA256 `027111ce658f8b31b7ed3ba39805e49f702b85c2d1011e42e89fc749117bbe6f`；API36 141613 bytes/SHA256 `a68cedd02d60cd89261506c5987cd97cb6cbc1a032f921ab4e582dba5c3fbf78`。模拟器WebView分别124.0.6367.219、133.0.6943.137，不是用户153.0.8010.36或真实长会话PDF证据。

原签名主APK1892188 bytes/SHA256 `e5f2ad82e37eab80de28d6801e7c853014416d69680f6542540f6480de57544e`。独立核对下载ZIP摘要、APK校验值、版本1.4.0-scroll-trial/code14、原证书记录、三份采集脚本与源码逐字节一致、DEX完整buildRevision=8ed24f7、主APK不含测试夹具。版本不递增、不创建Release；会话直接提供主APK和仅主APK/校验值ZIP。[GitHub备用构建产物](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37487134618/artifacts/11423542887)，需登录/解压，仅安装ChatGPT-Nova.apk，不安装ChatGPT-Nova-tests.apk；产物有保留期限。

手机复测：原会话等待回复结束→导出聊天历史（试用）→快速导出已加载消息→HTML。该入口不滚动；只在需要加载更早内容时用单向扫描历史。核对缺失的第12条进度文字是否恢复，并在第11和13条之间；不要只检查count是否30。若仍缺，复制导出诊断，应为buildRevision8ed24f7且dom.source=read-only-dom-v3，包含dom.progressDiscovery、authors、turnFallbacks、explicitProgressBlocks（滚动终止只保证progressDiscovery，普通采集计数在快速路径完整提供）。无需用户搭建ADB。

本批仅补明确助手turn与明确commentary正文，不猜未标角色的候选，不按文本去重，不伪造分片ID；同一消息内的进度/最终正文可能保持一条。真实第12条可见性及结构仍没有设备证据，30/30尚待原会话复测；网页若已不保留，DOM不能还原。普通消息29/29、可见有序参照29/30为用户此前提供对照，不是本环境独立解析。完整历史始终not-proven，原附件未备份，旧403未复测。下方待CI/未接入APK为本批交付之前的历史记录，以上结果优先。


## 进度补采与结构诊断批次（待CI及原会话复测）

用户已要求继续修。版本保持1.4.0-scroll-trial/code14，不发布Release。快速导出已加载消息改为显眼的主按钮；单向扫描历史仍为可选入口，不新增遍历。采集v3只补明确`article[data-turn="assistant"]`且没有自身/后代作者标记的可见turn，保持DOM顺序；未标角色的turn/工具角色/隐藏内容不会猜作助手。整体明确commentary的作者保留完整可见正文（复现检查曾发现非空Markdown外文字仍遗漏，已修）；已选Markdown之外明确`data-message-channel="commentary"`的可见内容也保留，但没有独立身份的分片仍归原消息，不人为拆出第30条。类型仅明确channel时标进度/最终，否则assistant-unknown；同文本不同ID保留，重复ID仍拒绝D08，缺ID不能用于跨窗口缓存。

已将只读progress-discovery探针包含在APK并接入快速采集和滚动终止诊断`dom.progressDiscovery`。不每个滚动采样执行探针；诊断仅计数、白名单角色、数字位置、隐藏分类及Markdown之外字符数，不含正文、ID、URL、登录资料。不保证其turn候选匹配真实网页。真实第12条是否仍在网页/实际markup尚未知，因此这是有限兼容补采与可安装定位批次，不能宣布30/30或完整历史修复。

本地Chromium17个既有单快照、12个新增进度场景和10个只读探针场景通过；30个滚动场景通过，包含新增进度缓存/顺序场景。合成30条有序夹具补回显式助手turn进度，不是用户原会话复现。Android新增SAF保存进度顺序/标签/脱敏诊断检查，目标35/36各16项，CI尚待执行。后续仅在同次真实导出里确认第12条内容及位置，不能只以count=30放行。


## 第12条助手进度已由用户逐条定位；结构机制待真实页面证据

用户提供30条有序对照：15用户、14普通助手最终回复、1助手进度；唯一缺失为第11条用户请求制作转移包之后、第13条用户再次请求之前的第12条进度。HTML29个article对应全部29条普通主消息。用户报告全29条标准化整条文本重复为0，两组高相似内容是真实不同消息，不该去重。本环境未收到该HTML字节，不把用户逐条核对写成本环境独立解析；此前缺失条目未知现已解决，具体网页结构/角色为何没采到仍未确认。

独立源码审查：采集器没有显式排除commentary/channel，普通assistant作者节点的无Markdown正文可安全备用且能导出。真实进度可能没有作者标记、位于已选非空Markdown之外、隐藏/折叠，或已不保留在历史DOM；不能仅从messageType推论某一种。已新增 [只读进度结构探针](../../tools/feasibility/progress-coverage/README.md)，10个虚构结构检查独立通过，含非空Markdown外进度被漏、无作者turn、普通assistant进度本就可采集、同文不同ID不去重、只读与脱敏。该工具尚未接入APK，未改生产采集器、未做新APK/版本/Release。

已向用户提问：Nova当前历史页第12条是否直接可见、展开后可见或只在原始对照中存在；答案决定是否能沿DOM补采。若可见，先把只读探针接入复制诊断取得原始/可见作者数、无作者turn与Markdown外文字统计，再以实际结构实现user/assistant-final/assistant-progress及未知类型，保留位置顺序与稳定身份。若页面不保留，不能靠选择器或多滚几轮还原，需确认独立数据材料实际包含该条。

身份规则：不按正文/相似度去重。progress/final可能共享父messageId，需稳定分片/子节点身份，不能假定messageId单字段已区分两条。role+原始序号+DOM对象仅当次快照/对象生命周期有效，不能跨虚拟化卸载/重建去重。已有官方JSON候选基准也须实际检查目标分支和进度是否存在，不能预先承诺包括所有进度。当前状态普通主消息29/29、用户有序可见记录29/30；进度修复未完成。

## 用户手数30条但采集29：已定位计数阶段，具体缺失条目待真实材料

用户上传手机截图，弹窗为“已采集29条已加载消息”，表示手数30条，明确要求查清漏哪条/为何漏。截图背后为正常助手回复底部，但只展示局部页面，不能从截图复原30条基准。截图仅本地查看，不提交图片/私密会话内容到公开仓库。上一轮成功诊断的15用户/14助手是采集结果，未独立证明目标会话完整；“29条第一秒均已加载”仅说明这29条没有靠滚动新增，不等于30条全部取得。

独立追踪：格式选择弹窗count由render返回messages.length，出现弹窗时尚未写HTML/Markdown/PDF；Java逐条渲染不跳过数组元素。因此计数差异发生在网页采集或消息划分阶段，不是生成HTML时掉了一条。DOM按可见user/assistant的data-message-author-role容器计数，非按屏幕段落/文字数计数；同一author多markdown块合并为一条。整条author隐藏过滤发生在nodes筛选，现有diagnostic.filtered仅计正文遍历过滤，缺少“原始作者节点→角色筛选→可见筛选”的统计，这是定位盲点。

本轮独立Chromium7个针对性只读检查：正常30个作者节点全取30（15/15）；最后助手缺角色属性、角色不在允许范围、祖先aria-hidden各可成功返回29（15/14）；两块正文置于同一author可返回29但两块文字都保留（该夹具角色14/15，不是用户15/14的真实复现）；重复ID报D08，真空正文报D09，均不是悄悄丢1条。另有既有永不挂载消息负例，不能排除DOM从未提供第30条。以上是可能路径/排除依据，不宣称任何一种就是用户设备根因。没有按相同正文去重，不能随意归因重复文本。

已用异步文本问题请用户提供手数的用户/助手各几条及缺失条目的开头，未知可回答不知道。还需要同次导出HTML与对应缺失消息文字/截图或有序30条参照，才能识别实际缺失/是否两段计数差异；HTML不保留源消息ID，单凭HTML仍不能区分缺属性、隐藏或未挂载，需要具体条目后进一步采样。当前不改选择器、不放开隐藏内容、不再制作APK/版本，不以合成案例当修复。下一批诊断可补原始/角色/可见候选数及隐藏分类，但不得输出真实ID/正文到脱敏诊断。实际根因未查清，保持问题待定位。

## 0df561e 真机成功，但滚动没有新增：优先使用现有快速路径

用户提供明确buildRevision0df561e的真机成功诊断：Android36/WebView153.0.8010.36，H00_READY/scroll-collected-unproven，缓存29条（15用户/14助手），单向向上65步、leg1/plannedLegs1/secondPassfalse，无缩步重试；采集84449ms、总85333ms、scrollMax38590/viewport747，终点挂载29条且contentStable7。随后明确反馈“第一秒就读取所有信息了，滚动完全没增加计数”。本次滚动没有带来用户观察到的新增消息；不能把完成扫描说成85秒获取了新的历史。

独立核查当前APK对应源码：菜单“采集并选择格式”已有不滚动capture路径，执行一次DOM采集→后台转换→保存前同页面签名复核，支持既有HTML/Markdown/PDF流程。用户可以直接使用该按钮，无需另装APK即可省去本次扫描。其范围仍是当前DOM已加载消息，完整历史未确认，工具进度/原附件边界不变；首次29条不证明还有没有未挂载历史，不能在滚动无新增几次后自动声称全量。

诊断totalListChanges/totalBodyChanges等仅累积窗口内变化；resetWindow会清空前一快照，因此0本身不能证明跨窗口的列表从未变化。用户直接观察首轮缓存29、终点挂载29与缓存29共同支持本次没有新增，但本环境没有真实DOM或HTML独立全量核对。

本轮未改生产源码、不新增APK/版本/Release，也未重复运行已通过的测试。此前0df561e的CI/独立核对结果继续有效。下批集中改进：将现有快速导出作为显眼默认入口、明确按钮范围；按需要选择单向历史扫描，不用重复扫描当完整性；增加initialCachedCount/addedAfterInitial/snapshotCalls/解析与等待耗时及真正跨窗口变化的脱敏计数，再基于真实计时优化。不要把快速路径写成尚不存在而又重复制作安装包。

## 单向版本已验证并直接提供：0df561e / 1.4.0

用户最新要求为“到顶端或底部来一遍，直接一边过”，取代强制上下与第二轮方案。[CI37478242887](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37478242887)，最终源码 `0df561e822e4c3efe8c95f20140e71d60bae80ff`，构建/lint/原签名、Chromium17单快照+29滚动场景、Android35/36各15项全部通过，publish skipped。默认只单向一遍：在顶部向下、在底部向上、中间开始先定位到底部再向上，到另一端结束。完成恢复原始像素位置后不继续采集。不会出现自动第二轮或把secondPass标true。

本批提高步幅并保留相邻ID重叠、有界缩步重试、加载等待、正文/顺序冲突和取消限额。独立下载两份证据核对 `OK (15 tests)`，含顶部单向/无第二轮/恢复、3节点窄虚拟窗口保存40条、8秒加载等待后保存、原取消/SAF/打印/脱敏。两个固定正文系统PDF均4页且含首尾/代码/表格：API35 140385 bytes/SHA256 `08eb9f55012f7790b815da75f396e1159b69f0a74a1f96342a3c151418d1c796`，API36 141748 bytes/SHA256 `d63c38e197746ecab923515389c3fa286938066ade649261e47b783d6b569302`。不是用户WebView153的实测速度或真实长会话PDF证据。

本批原签名APK 1889683 bytes/SHA256 `6ffff37b2ce2d71057d4b0017cc619386e8e49c6669c9f3f6f5a8e44acaa3f75`；版本仍1.4.0-scroll-trial/code14。独立核对版本/原证书记录、两份脚本字节与源码一致、DEX完整buildRevision等于0df561e、测试夹具未含于主包。按用户既有授权在会话提供主APK及仅主APK/校验值ZIP，不创建公开Release。备用：[GitHub构建产物](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37478242887/artifacts/11420236145)，需登录/解压，只安装ChatGPT-Nova.apk，不安装ChatGPT-Nova-tests.apk；有保留期限。

复测看方向是否保持单向并在端点结束、之前已核对的27条主消息是否仍齐全、耗时是否改善。诊断应为scheme DOM-SCROLL-TRIAL-2/buildRevision0df561e、traversal single-direction/plannedLegs1/secondPassfalse，可反馈steps与elapsedMs/overlapRetries，不必提供正文。用户此前27条正面覆盖反馈保留，下方旧四趟/待验叙述为历史；当前单向APK的真机覆盖/耗时待复测，完整性仍未确认。独立基准导入和附件备份仅完成设计，未实现新的产品入口。

## 用户确认27条主对话覆盖；本批改为单向一遍（验证与交付见上方）

用户提供新HTML解析/逐轮对照结果：27条（14用户、13助手），此前包名修正、增量提示词、转移包、GitHub连接、d143a79/bd2a6bd审查及M1等遗漏轮次已出现，普通用户消息/助手最终回复截至该导出时刻未发现大段丢失，顺序正常。工具执行进度未包含，附件元数据部分保存、附件/图片原文件未包含。此为用户提供的真实单样本正面覆盖观察；本环境没有HTML原件/独立基准或本次buildRevision，不记成本环境独立解析，不推广成所有长会话完整或100%备份。

用户进一步明确否决“一次上行一次下行”和第二轮，要求单向扫一遍到另一端就结束。当前实现因此去除所有自动折返/第二轮：底部起点向上、顶部起点向下、中间起点先定位到底部再向上，完成/取消仍尝试恢复原始像素位置（完成时回原位置不代表继续扫描）。覆盖数据固定plannedLegs=1/secondPass=false，direction和traversal=single-direction明确方向，输出说明不再声称两轮核对；history/text仍not-proven。

每次推进从0.45提高至0.8窗口；相邻稳定窗口必须共享至少一个消息ID，否则按上次命令步幅减半回退重试，最多5次后拒绝H03，不把断开的窗口缓存成成功。仅为保留重叠的小幅重试，不是全程折返核对。缓存正文/角色冲突、唯一顺序、加载等待和限额/取消保留；正常轮询250→120ms，加载等待400ms，不缩短历史加载的30秒硬限或增加数据源hook。UI显示单向方向、加载等待、定位起点和缩小步幅重试。

本地Chromium17单快照+29滚动场景通过，包括顶部向下/底部向上/中间定位与恢复、窄3条虚拟窗口缩步后有序40条、慢加载、取消、冲突及初始布局30秒截止。新增负例：一条消息永不挂载，单向扫描可完成39条且观察两端，仍明确not-proven；这证明端点/缓存条数不是独立完整性校验。初次窄窗口测试直接删末4节点实际使末4永不挂载而得到36条，已修正为真正覆盖40条的3节点虚拟窗口；未以放宽完整性断言掩盖该输入限制。

只读同一40条夹具操作成本对照：已交付7216e92四趟56滚动/197采样/28上行+28下行；本批单向8滚动/32采样/8上行+0下行，均缓存40条。仅夹具操作次数，不是用户设备实测提速百分比或实际耗时。Android35/36各15项已在本批最终CI通过，新增顶部单向状态/恢复和窄窗口HTML保存；源码/最终包以CI提交为准。版本仍1.4.0/code14，无新增Release；已通过必要检查并直接提供本批测试包。[独立ID/附件边界设计](../export/independent-validation.md)不包含基准导入/附件打包的新产品实现。

## 真机反馈：到顶后回底继续滚动（尚未分类）

用户在最近测试包交付后反馈“一直往上滚，到顶之后跳转底部继续滚”；追问阶段/结果后回答“没注意，但是没报错”。本次没有新的buildRevision/诊断，不能独立确认安装提交、跳动时leg或是否瞬间跳转，不能认定正常、更不能宣称网页自身自动滚动就是根因。

独立审查现有源码：阶段仅为leg0向上收集→leg1向下收集→leg2再次向上核对→leg3再次向下核对→leg4完成。换方向只重置稳定窗口，不调用跳到底部；活动移动为0.45窗口逐段。只有完成/失败/取消的dispose(true)尝试恢复原始scrollTop，开始在底部可能看到回底。原始scrollTop是像素值，动态网页高度变化下并不保证原消息锚点。

本轮只读Chromium轨迹复核40条虚拟夹具：完成/40条/history not-proven；360px窗口活动最大移动162px；阶段切换0→1位置0→0，1→2位置2200→2200，2→3位置0→0，3→4完成位置2200→2200。该夹具不能复现或排除真机页面跳跃/锚定干扰。没有改生产源码、增加版本或交付另一个APK；下一步需要真实结果诊断和阶段，区分逐段上下核对、完成恢复与活动期异常跳跃。如确认活动期跳跃，下一批集中增加跳跃/移动方向/阶段与恢复原因诊断、异常移动保护并验证，不能仅凭无报错当正常或完整。

## 本批已验证并直接提供测试包：7216e92 / 1.4.0（不创建 Release）

[CI37464896999](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37464896999)最终源码 `7216e924f10eb41a1839cdb657e8fd4a8068c585`，build/lint/原签名、Chromium17单快照+25滚动场景、Android35/36各13项全部通过；publish skipped。先前37464449161、37464495282、37464530438在本批测试计数/步数预算修正时被取消，不能记作完整通过。

已独立下载两份仪器证据核对 `OK (13 tests)`，包含新增8秒历史加载等待后HTML保存40条/首尾/位置恢复、持续正文变化仍H06且脱敏、布局抖动、滚动缓存、取消、SAF/打印。固定正文系统PDF均4页且有首尾/代码/表格标记：API35 140385 bytes/SHA256 `82496ea4acda2730aa897c700f065b9920d928e8b50af5becfa1dde975e9e90b`；API36 141748 bytes/SHA256 `d63e1badd9ba3e564c565efc27188d43c2b9db15dbb8ff75d620b521e3c5564b`。全部是受控夹具，不是真实账号历史或用户WebView153验证。

本批APK 1889055 bytes/SHA256 `cfdfdba75a798d4782cdfedb55b954bc1bb22e33ac5e59619fd7844663fdd382`。独立核对版本仍1.4.0-scroll-trial/code14、原证书记录、两份导出脚本与源码字节相同、DEX中的完整buildRevision=7216e924f10eb41a1839cdb657e8fd4a8068c585、测试夹具未包含。依照用户明确要求，在会话提供主APK及主APK/校验值ZIP，可覆盖安装；不升级版本、不创建公开Release。[GitHub构建产物备用下载](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37464896999/artifacts/11414128595)需登录/解压，仅安装ChatGPT-Nova.apk，不安装ChatGPT-Nova-tests.apk；产物到期2027-01-04。旧224dae2包不含本批修正。

手机复测：原H06会话等待回复结束→导出聊天历史（试用）→滚动收集历史，保持前台且不操作页面；加载时应显示等待状态。失败复制诊断，应为scheme DOM-SCROLL-TRIAL-2/buildRevision7216e92；loadingSignals/loadingPolls/loadingObserved与readyAgeMs可区分识别到加载和列表换入，单窗口30秒、整次10分钟/1200步有界。成功后逐项核对此前遗漏的中间消息。用户原会话修复与完整历史仍未确认，不能宣布完整功能验证完成或旧403修好。

## 224dae2 真机再次 H06：历史加载等待修正（已通过 Android CI，见上方交付）

用户安装224dae2/DOM-SCROLL-TRIAL-2在Android36、WebView153.0.8010.36仍H06：86.537秒、leg0、68步、缓存19条（10用户/9助手），未到顶部/底部、未二次核对。最后窗口5.382秒仅6次采样，列表变化4次、正文变化1次、高度变化4次、contentStable=1；用户观察到转圈未结束即中断。旧诊断无加载指示字段，不能断言真实转圈的DOM形态或唯一根因，也不能把前一APK说成已经修好。

源码确认固定5秒截止不识别历史加载。已独立对比旧224dae2与本修正：5.382秒才换入列表，旧报H06，新继续等待。新逻辑仅检查所选滚动区域内可见的公开aria-busy/progressbar/loading-spinner/animate-spin，正文装饰旋转、隐藏或区域外指示不算；加载时不缓存、不移动、不确认边界，消失后重新核对。无已识别指示时，消息ID/角色列表换入也给予新的5秒准备宽限；单纯同ID正文变化不延长宽限，缓存正文/角色冲突仍拒绝。

每个滚动窗口固定30秒总上限（列表反复换入/转圈不能无限延长），边界稳定核对也在该上限内；整次上限调整为10分钟，避免旧120秒在已经86秒却仍第一轮的真实反馈中提前截断两轮扫描。本次真机scrollMax34221/viewport747按0.45窗口估算四次遍历约408步，旧300步也不足，步数上限同步改为1200；1000消息、正文/字节限额、15秒单次回调、取消/清理不变。新增loadingSignals/loadingPolls/loadingObserved、readyAgeMs/windowLimitMs/scanLimitMs/stepLimit诊断及“网页正在加载历史”界面提示。指示选择器是有限兼容尝试，不保证识别真实网页所有转圈；完整历史始终not-proven。

本地Chromium17单快照+25滚动场景通过：实际6.5秒转圈后40条核对/位置恢复、无转圈的迟到列表宽限、永久转圈/不断换入在30秒拒绝、取消清理、装饰/隐藏/区域外指示忽略；旧正文持续变化和顺序/二次核对拒绝仍通过。新增Android8秒延迟加载→HTML保存40条测试，Android35/36各13项已在最终CI通过，证据见上方；不当作真实历史成功。版本保持1.4.0/code14，scheme2，通过buildRevision区分；只做CI验证，不自动Release，本修正现已按用户授权直接提供同版本测试包，下一步仍是用户原会话复测。

用户实测已发布1.4.0在Android36/WebView153返回H06_UNSETTLED：61.23秒，向上第一轮leg0、49滚动步、缓存10条（5用户/5助手，12573字符），未观察到顶部/底部、未开始第二轮。不能从旧诊断确定根因。本地已复现正文/ID不变、仅滚动高度2px变化使旧版误报H06；修正采用三次相同正文快照即可缓存，边界另按2px容差连续核对，边界持续变化仍拒绝。新增settling分类/计数/窗口年龄/几何数据（不含正文ID），scheme为DOM-SCROLL-TRIAL-2，buildRevision记录构建提交。版本号保持1.4.0/code14，修正尚未发布，旧已安装包不包含本修正。Chromium17原采集+18滚动场景本地通过，Android35/36各12项在工作流37461566300全部通过（新增受控布局抖动保存40条及持续正文变化拒绝/脱敏），已独立下载核对两份OK (12 tests)与实际PDF。源提交224dae2；publish按规则跳过，未发布修正包。用户要求集中修复/功能后再交付，取消每次push自动发布，仅显式workflow_dispatch publish_trial=true才发布；本轮先验证提交，不新增Release。真实原会话仍待后续集中测试包复测，完整历史未证明。

当前用户授权可安装滚动历史测试。已接入1.4.0/code14滚动缓存、两轮上下核对、稳定ID合并/顺序冲突拒绝、取消/清理、覆盖结果与DOM-SCROLL-TRIAL-1诊断；完整历史仍未证明。Chromium17单快照+14滚动合成场景本地通过，Android35/36各10项、构建/lint/原签名与发布在工作流37457721588全部通过，已独立下载核对APK及两份4页PDF。源提交5cdb631，1.4.0已签名交付，永久下载和SHA256见试用说明。用户原会话仍待复测，完整历史未证明。当前采用页面临时内存缓存（完成/失败/取消清理），不是完整生产实现；APK与证据以试用说明为准。下方“尚未接入APK”是上一阶段记录。

最新历史覆盖反馈：用户转述GPT-5.6 sol对HTML的检查为7条消息（3用户/4助手）且缺多段已知历史；本环境没有HTML字节，未独立核文件或用户页面。源码确认只做单次DOM采集与同快照签名复核，无滚动缓存。新增 [历史完整性设计](../export/history-integrity.md) 与生产脚本限制合成复现：40条基准仅挂载7条时导出/复核成功但缺33条，虚拟窗口并集不能证明完整，ID匹配不核正文；3场景及既有audit测试通过。设计采用有界上下滚动缓存、稳定ID合并、顺序/正文冲突拒绝、独立基准核对及四维状态；到顶/无新增不允许标完整。审查设计已完成，滚动采集尚未接入APK，真实完整历史未验证。下一步可做有限滚动采集试验，不能承诺完整导出。此前D09修正不能补出未挂载历史。

最新修正版：源提交f2d0cce，1.3.9-dom-trial/code13已原签名发布。用户实测1.3.8普通全文字会话D09；修正合成复现的display:contents误过滤，新增失败消息序号/角色等脱敏诊断。Chromium17场景与Android35/36各7项夹具通过，独立核对两份系统保存4页PDF及APK版本/字节/脚本/证书记录。工作流37454082614全部成功，下载与证据见 [试用说明](../dom-export-trial.md)。模拟器WebView m124/m133，并非用户153。用户原会话仍待复测，不能声称具体根因已确认或问题已修复。read-only-dom是只读采集方式，不是失败状态。完整项目长会话验证未完成，不具备承诺完整历史的生产实现条件；可继续已授权有限试用。下方1.3.8及可行性内容为历史记录。

最新进度：用户已授权新方案试用版本，生产导出实现因此已修改。1.3.8-dom-trial/code12，源提交895d32b，已原证书签名发布。Chromium10场景、构建/lint/签名与Android35五项夹具通过，独立核对系统实际保存的4页PDF首尾/代码/表格文字。安装和证据详见 [试用说明](../dom-export-trial.md)。当前是“试用链路已验证可交付”，完整项目历史、真实账号/用户设备、富文本保真与全量回归仍未完成；旧403未确认修复。下面“生产源码未改动”描述此前只评估阶段。

2026-10-06 续接：远端起点核实为 `fc26979`，无后续提交。新增 [逐条历史覆盖验证工具](../../tools/feasibility/history-coverage/README.md)，修补旧探针只返回首尾四条、不能核对中间历史的验证材料缺口；未修改旧探针或生产源码。

本轮独立执行覆盖 audit 合成检查和 Chromium 元数据探针检查均通过；没有重跑下方历史检查。真实Android/账号仍未接入。用户说“可连接设备”，连接与版本细节回复“未知，最新版”，故仍不能执行真机采样或判断旧403。

工具退出0只代表给定独立基准的单次ID/角色/顺序匹配，不证明基准完整、正文正确或Android导出成功；并集匹配不能批准单次DOM路线。完整性始终标未证明。

当前结论：可以进入真实设备可行性验证，不能认定完整会话导出可行，不能说旧403已修好。生产源码未改动。

独立执行：DOM 9/9；Java API35 编译通过；指定基线补丁 index 检查通过；PDF15页确认。报告其他测试结果属于历史材料记录，API26本轮未重跑。

必须携带的两项修正：
- main 为1.3.6，当前feature为1.3.7，最小补丁目标另有指定基线。不要混用。
- onPageStarted代次保护不覆盖所有SPA路径，不能保证丢弃全部迟到回调。

风险总表、成本排序、最小 DOM/PDF/SAF 代码、手动接入片段与验收标准已在 feasibility/REPORT.md、README.md、ExportFeasibilityProbe.java、dom-probe.js 中。不需重写完整功能。

下一项最高优先级：真实项目内长会话历史覆盖。如果要求完整历史而 DOM 无法可靠提供全量，换数据来源；不要把稳定消息数量或模拟页面通过当成完整性证据。
