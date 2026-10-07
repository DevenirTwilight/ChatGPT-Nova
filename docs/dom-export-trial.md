> 历史记录：旧滚动/Gecko试用方案已从正式入口退役。当前保存路线为普通HTML与Markdown同一次clone，PDF使用系统WebView打印同HTML；实际最新状态见docs/handoff/latest.md和docs/frozen-page-save.md。以下原证据保留，不代表当前产品行为。

> 历史研究记录，下面的阶段菜单/内部接口/PDF/发布描述不代表当前产品能力。当前正式功能是[保存当前网页](frozen-page-save.md)；旧滚动方案仅为默认禁用的[Legacy Conversation Scanner](legacy-conversation-scanner.md)，完整历史永远未证明。保留原失败、旧证据和阶段结论，不改写为成功。

# 新方案导出试用版

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


当前0df561e真机反馈：第一秒缓存29条，65步/约85秒扫描未增加条数。已加载内容可直接选菜单“采集并选择格式”，该路径不滚动，生成文件前复核当前页面，省去扫描；需加载历史时才选“滚动收集历史”。两种路径均不证明完整历史。此反馈已记录，本轮不另做安装包。

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

只读同一40条夹具操作成本对照：已交付7216e92四趟56滚动/197采样/28上行+28下行；本批单向8滚动/32采样/8上行+0下行，均缓存40条。仅夹具操作次数，不是用户设备实测提速百分比或实际耗时。Android35/36各15项已在本批最终CI通过，新增顶部单向状态/恢复和窄窗口HTML保存；源码/最终包以CI提交为准。版本仍1.4.0/code14，无新增Release；已通过必要检查并直接提供本批测试包。[独立ID/附件边界设计](export/independent-validation.md)不包含基准导入/附件打包的新产品实现。

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

用户随后明确要求“包可以不公开发布，但发给我”。本批改为在当前会话直接提供已验证的原签名APK和仅含主APK/校验值的ZIP，不创建公开Release、不增加版本；可覆盖安装，源码224dae2，版本仍为1.4.0/code14。APK为1888419 bytes，SHA256 `7495ec8792b29d2f47e74d6f92a50fe7baa4e7419737833ef78d6be578c7b88c`。如会话下载不可用，可登录GitHub从[本批CI构建产物](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37461566300/artifacts/11411334253)下载ZIP，解压只安装 `ChatGPT-Nova.apk`，不要安装 `ChatGPT-Nova-tests.apk`。Actions产物有保留期限，不是永久Release。

复测原H06会话：菜单→导出聊天历史（试用）→滚动收集历史，保持前台且不操作页面；成功后检查此前遗漏的中间消息，失败复制诊断。应显示DOM-SCROLL-TRIAL-2及buildRevision=224dae2完整提交。Android35/36各12项与浏览器检查已通过，但用户WebView153的原会话尚未复测，完整历史仍未证明。下方“尚未发布/后续交付”是本次直接提供安装包前的记录，旧Release链接不包含本修正。

## 同一1.4.0内集中修正：H06（未发布）

用户实测已发布1.4.0在Android36/WebView153返回H06_UNSETTLED：61.23秒，向上第一轮leg0、49滚动步、缓存10条（5用户/5助手，12573字符），未观察到顶部/底部、未开始第二轮。不能从旧诊断确定根因。本地已复现正文/ID不变、仅滚动高度2px变化使旧版误报H06；修正采用三次相同正文快照即可缓存，边界另按2px容差连续核对，边界持续变化仍拒绝。新增settling分类/计数/窗口年龄/几何数据（不含正文ID），scheme为DOM-SCROLL-TRIAL-2，buildRevision记录构建提交。版本号保持1.4.0/code14，修正尚未发布，旧已安装包不包含本修正。Chromium17原采集+18滚动场景本地通过，Android35/36各12项在工作流37461566300全部通过（新增受控布局抖动保存40条及持续正文变化拒绝/脱敏），已独立下载核对两份OK (12 tests)与实际PDF。源提交224dae2；publish按规则跳过，未发布修正包。用户要求集中修复/功能后再交付，取消每次push自动发布，仅显式workflow_dispatch publish_trial=true才发布；本轮先验证提交，不新增Release。真实原会话仍待后续集中测试包复测，完整历史未证明。

H06分类字段：coverage.settling.reason区分message-list-changing、body-or-structure-changing、edge-layout-changing、awaiting-content/page-not-ready；listChanges/bodyChanges/positionChanges/extentChanges为本窗口次数，total*为全程计数；contentStable、windowAgeMs、polls、mountedCount、scrollTopPx/scrollMaxPx/viewportPx与delta均为数字。缓存跨窗口正文/顺序冲突保护不放宽，不以布局变化忽略正文变化，不增加超时或完整性声明。已验证的仅是合成可复现缺陷，不认定用户原页面具有相同布局抖动。

发布节奏：push继续编译与检查，但publish job默认跳过；manual dispatch的publish_trial默认为false。后续统一交付时才显式开启。相同APK版本通过buildRevision区分，避免每条反馈都递增版本。下方1.4.0链接仍为上一批已发布源码5cdb631，不含本次修正。


本批最终验证：[CI37461566300](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37461566300)，源码 `224dae2e30daba3305cfe4d9a0c4f9f09df8a7b4`。构建/lint、Chromium17单快照/18滚动场景、Android35/36各12项通过；publish skipped，符合控制发布频率。独立核对CI APK仍为1.4.0/code14、原证书记录、两份脚本字节与源码一致、DEX中buildRevision等于224dae2完整提交。未发布APK 1888419 bytes/SHA256 `7495ec8792b29d2f47e74d6f92a50fe7baa4e7419737833ef78d6be578c7b88c`。

独立核对两份 `OK (12 tests)`，包含持续2px滚动高度变化下HTML实际保存40条、恢复原位置、连续正文变化仍H06且分类正确/脱敏、原滚动/取消/SAF/打印。固定正文系统PDF均4页且有首尾/代码/表格标记：API35 140385 bytes/SHA256 `ac5254dd238c36c673df01c79c30587a2977bcffaff4f7767a84842e2a7d8181`，API36 141748 bytes/SHA256 `590cacc777aad09ac5915a5a9a65075b3028cc0e37889fc0bbb0d3c699462c40`。这是受控夹具，不是真实账号完整历史。

额外复现：边界状态仅允许1px、几何稳定允许2px时，持续2px抖动能让扫描卡在第一轮底部；已统一两处2px容差，连续抖动回归通过，不放宽消息正文/顺序校验或完整性结论。先前CI37460549233为本修正被取消，不记为验证通过。当前用户安装的5cdb631/DOM-SCROLL-TRIAL-1仍可能出现原H06；本修正留在工作分支，后续按批统一交付，无新增版本和Release。


## 1.4.0 滚动缓存测试版（已签名交付）

用户已授权将滚动采集做成可安装测试版。版本1.4.0-scroll-trial/code14，新增菜单“导出聊天历史（试用）”→“滚动收集历史”；保留“采集并选择格式”的单次采集。自动识别消息滚动祖先，按半窗口上行/下行两轮，立即缓存净化正文，以稳定ID合并并用顺序约束图排序，同ID正文或角色变化、顺序冲突/歧义、缺ID、第二轮遗漏或新增均停止，不生成部分成功文件。取消会释放缓存并尽力恢复滚动位置，可重新采集。

结果页显示缓存数、用户/助手数、顶部/底部观察、二次核对、首尾本地预览及四维完整性状态。正文和ID仅留在采集会话内存，完成/取消/失败清理；可复制诊断不含正文/ID/URL，scheme为DOM-SCROLL-TRIAL-1，coverage聚合统计。扫描使用页面内临时内存和公共DOM观察，会改变滚动位置；不再称作纯只读探针。不调用旧私有接口、不hook fetch/history、不读取React/Cookie/storage。

边界：完整性始终未确认；到顶、无新增、两轮一致或唯一消息顺序不能证明无遗漏，未接入独立基准核对。公共DOM观察和每轮URL检查不能绝对排除同一事件任务内无DOM变化的SPA往返，也不能证明已选分支从未变更。附件元数据未完整核对，附件/图片原文件未包含；不能称为完整备份。限额为1000消息、200万净化文字字符、64MiB缓存、8MiB结果、300滚动步和120秒，单窗口5秒稳定等待、回调15秒。失败和取消当前均不提供部分文件保存，避免错误顺序输出。

新增错误码：H01_SCROLL_CONTAINER滚动区域不可识别/不可移动；H02_MISSING_ID无法合并；H03_ORDER顺序冲突或歧义；H04_CHANGED/H04_SECOND_PASS/H04_SESSION内容/会话变化或第二轮无法核对；H05_LIMIT限额；H06_UNSETTLED窗口未稳定；H07_CANCELLED取消。原D/N/S/P诊断保留。

本地Chromium17单快照+14滚动场景通过，包含始终仅挂载7条但缓存有序40条、同文不同ID、缓存清理、缺ID、正文变化、顺序冲突、SPA变化、流式/超限/超时和不宣称完整。Android35/36各10项夹具及签名发布均已通过工作流，详见以下独立核对证据。详细设计见 [历史完整性设计](export/history-integrity.md)，本实现是有界滚动试验，用户原长会话尚未验证。


已交付：[下载1.4.0 APK](https://github.com/DevenirTwilight/ChatGPT-Nova/releases/download/nova-dom-trial-5cdb631/ChatGPT-Nova.apk)；[Release备用入口](https://github.com/DevenirTwilight/ChatGPT-Nova/releases/tag/nova-dom-trial-5cdb631)。原包名/原证书，可以覆盖安装，无需卸载或清除登录数据。源提交 `5cdb63168716a174a9ed406ef9a69f2da046cfb0`；[工作流37457721588](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37457721588) build、android35、android36、publish均成功。

已独立下载APK核对code14/1.4.0、原证书记录、SHA256、包内两份脚本等于源码及测试夹具未打包。APK 1887387 bytes，SHA256 `04a0aed96a1a39bd50eef5092bb86895931afa4d27fe973f8ea4921c96fb1e24`，Release资产digest一致；证书SHA256仍为 `f93221ee0d2be2b806233a0b3427ec9c14766100c1bab3208841e6203e2b4289`。

独立下载Android35/36证据，核对各自 `OK (10 tests)`，包含实际WebView虚拟化夹具→滚动缓存→覆盖页→HTML→SAF写入，断言40条article/逐条文字、只挂载7条、同文不同ID不丢失、取消清理并重试、缺ID诊断脱敏。三项滚动Android测试使用smooth-scroll样式；旧七项单快照/SAF/打印仍通过。两份实际系统保存PDF均4页A4且有首尾/代码/表格标记：API35 140541 bytes/SHA256 `1371291674a69748e42d2f6f7a71b971c76b305c367fb9a7e61216b4f30717c8`，API36 141748 bytes/SHA256 `8825e80f8f3393c1f29652f637a807f933da0abcdeccf1460cd11bd4c94665dd`。模拟器WebView m124/m133，不是用户153；PDF仍由旧固定正文夹具验证，不当作真实长会话PDF证据。

首次CI37456995811在正文编辑合成测试失败：未处理的滚动事件重绘覆盖了测试修改，已改为等待缓存/滚动事件就绪，未放宽失败标准。随后独立复现CSS平滑滚动误报H01，改为明确instant滚动并加入对应夹具；37457201606为此修正被取消，最终37457721588全部通过。

手机测试：打开此前仅导出7条的原会话，等生成结束，菜单→“导出聊天历史（试用）”→“滚动收集历史”。保持Nova前台且不操作页面，等待覆盖结果后选“选择导出格式”→HTML→保存到本地。核对之前已知缺失的中间消息；反馈缓存条数、是否恢复已知遗漏及复制诊断即可，无需发送正文。失败/超限时复制诊断，不会自动生成部分成功文件。完整历史真实验证仍未完成，不能因为本次夹具通过而宣称完整备份。


最新覆盖反馈：用户转述导出只有7条且缺失大量历史。源码确认当前版本无跨滚动窗口缓存，D09修正不解决历史缺失。已完成 [滚动采集与完整性校验设计](export/history-integrity.md) 和当前脚本限制的合成复现；尚未接入新APK，下面1.3.9仍是单快照版本。到达顶部或无新增不能据此宣称完整。

## 1.3.9 修正版：D09 与正文定位

用户实测1.3.8在Android36/WebView153.0.8010.36的普通会话报D09：找到5条消息、无缺失/重复ID、已解析2个代码块，但没有指出哪条正文失败。用户确认全是文字消息（可能含代码）。这是第一次用户真机新路线失败证据，不是旧403，也不代表完整历史覆盖结论。

已在合成页面复现一种对应缺陷：`getClientRects().length > 0` 会把 `display:contents` 正文包装层误判为不可读，导致有文字却返回D09；也可能漏掉页面外已加载的延迟布局内容。没有用户实际DOM，不能断定该布局就是其唯一根因。

1.3.9-dom-trial/code13修正：正文可读性改为检查自身与祖先的hidden/aria-hidden、display:none、visibility隐藏和content-visibility:hidden；不再要求自身有布局框。明确隐藏的正文仍过滤，offscreen的content-visibility:auto及display:contents可读取。`.markdown`为空时，只尝试同一author容器的结构化过滤，不使用原始textContent绕过隐藏/控件过滤，不跳过失败消息。复核签名覆盖完整author子树，包含备用正文来源。

新诊断scheme为DOM-TRIAL-2、source为read-only-dom-v2。D09记录失败消息序号/角色、author与选中正文字符数、选择器命中数、过滤类别、布局状态、图片/媒体/折叠计数和已处理条数；不含正文、HTML、ID、类名、标题、URL或登录凭据。弹窗会指出第几条用户/助手消息失败。字段read-only-dom只描述采集方式，不是失败码。

本地Chromium17场景通过，包含旧版失败的无布局框正文、无布局框author、已加载但页面外的延迟布局、祖先隐藏过滤、正文选择器为空时的安全备用、备用来源变化复核、真正空正文的拒绝与诊断脱敏。Android35/36各新增两项对应夹具（共7项/版本），工作流均已通过（见下方证据）；真实用户原会话仍需复测，完整历史仍未证明。发布检查不把合成复现当成用户问题已解决。

修正版已交付：[下载1.3.9 APK](https://github.com/DevenirTwilight/ChatGPT-Nova/releases/download/nova-dom-trial-f2d0cce/ChatGPT-Nova.apk)；[试用Release](https://github.com/DevenirTwilight/ChatGPT-Nova/releases/tag/nova-dom-trial-f2d0cce)。源提交 `f2d0cced4020a075fb30940f65daeef3d201ee22`；[工作流37454082614](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37454082614) 的build、android(35)、android(36)、publish均成功。Chromium17场景、构建/lint/原证书检查、Android35和36各7项仪器夹具通过，包含无布局框正文保存和真空正文定位/脱敏。模拟器WebView分别为m124、m133，不等同于用户WebView153。

已独立下载Android35/36证据，核对各自 `OK (7 tests)` 及系统保存PDF的首尾/代码/表格文本标记。两份均4页A4：API35为140541 bytes、SHA256 `ca08e6dff3bec222769687add5c2eab548bf1d4946ba7532a6a652b340ca86c0`；API36为141613 bytes、SHA256 `0e9f10ae9419053732d58b321fabe5e60520be594600c8276753048d8a65ae0f`。这些是受控夹具，不是真实账号历史。

新版APK为1878464 bytes，SHA256 `fd6edd4379801577cc1bfe0b6c10ce5899e483a18da050871375bef4fa2a0dc5`。独立核对版本code13/1.3.9、原证书指纹、APK SHA256及包内DOM脚本等于源码，Release资产digest一致。原包名/证书可覆盖安装。请在原D09会话等待生成结束后重新导出；若仍失败，复制DOM-TRIAL-2诊断，定位失败消息。用户原会话是否修复仍未确认，完整项目长会话验证尚未完成，不能开始以完整历史为承诺的生产功能实现。

下文1.3.8下载和证据为前一版本记录。


用户在2026-10-06明确要求制作可安装的新方案试用版本并增加报错诊断。本轮因此允许修改实现和发布工作分支试用APK；不合并main。此前“只评估、不改生产源码”是上一阶段范围，不阻止此试用请求。

版本：1.3.8-dom-trial / code12，包名保持 `com.example.chatgptnova`。由GitHub Actions使用原发布证书签名，试用发布不设为最新正式版。只有签名、构建、lint和本轮Android35导出夹具通过后，独立DOM试用工作流才发布永久APK下载。旧私有读取器的历史测试不是本试用版验证。

已交付：[直接下载APK](https://github.com/DevenirTwilight/ChatGPT-Nova/releases/download/nova-dom-trial-895d32b/ChatGPT-Nova.apk)；[试用Release](https://github.com/DevenirTwilight/ChatGPT-Nova/releases/tag/nova-dom-trial-895d32b)。实现提交 `895d32b12a90db872e2faa175fe6751b7437fc4a`；[构建与验证工作流](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37451344432)。

APK 1877400 bytes，SHA256 `8f7a6736621e5e8c629797fce4b7c1858410d449ceb127f10d58ba9fc679a5e6`；发布证书SHA256 `f93221ee0d2be2b806233a0b3427ec9c14766100c1bab3208841e6203e2b4289`。已独立下载构建产物核对版本、证书记录、APK SHA256和包内DOM脚本与源码一致；Release API资产digest与之相符。

## 手机上怎么试

1. 下载试用Release中的 `ChatGPT-Nova.apk` 并安装。原证书与包名相同，可以覆盖安装；不要卸载原版或清除数据。升级后的登录是否保持仍以实际设备为准。
2. 打开一段短会话，等待回复结束。菜单选“导出已加载消息（试用）”，阅读范围说明，点击“采集并选择格式”。
3. 选HTML或Markdown，随后“保存到本地…”；也可打开或分享缓存文件。PDF使用系统打印界面的“保存为PDF”，需实际打开保存文件检查。
4. 再打开目标项目长会话，滚到顶部等待历史加载，再回到底部试一次。核对开头、中间已知消息、末尾和代码/表格；只核对首尾不能证明完整。若数量随滚动变化或缺历史，记录操作及症状。
5. 失败时点击“复制诊断”，或者从菜单“导出诊断”复制最近一次结果。提供诊断和错误现象即可，不必发送聊天正文、Cookie或Token。

**范围：只导出当次采集时DOM已加载的消息，完整历史未确认。** 不自动滚动、不请求内部接口、不修改fetch、不读取React、Cookie或storage。旧捕获器不再安装；这不等于旧403已修好。网页仍可以自己正常发起网络请求。

Markdown由渲染DOM转换，不能唯一恢复原始Markdown。保留可读代码、表格、链接和可用TeX；图片保留占位、附件不打包，折叠内容需要用户先展开。HTML/PDF离线渲染不加载远程图片或脚本。此版本是检验新数据路线与保存交互的试用版本，不能承诺完整导出。

## 诊断与错误码

诊断包含方案版本、APK版本、Android API、WebView版本、阶段、错误码、总耗时、消息数量、正文字符数、ID缺失/重复数、结构计数与非加密内容签名。不会包含标题、消息ID、会话/项目地址、正文、凭据、保存URI或异常原始message；仅记录异常类型。内容签名也不是完整性证据。

| 错误码 | 含义与处理 |
| --- | --- |
| D01_ORIGIN / D02_ROUTE | 页面来源或路径不支持，打开真实ChatGPT会话 |
| D03_LOADING / D04_STREAMING | 等待页面加载或回复生成结束 |
| D05_NO_MESSAGES / D09_EMPTY_BODY | 未找到消息或可读正文；检查登录、展开正文，复制诊断 |
| D06_LIMIT | 超过1000条、200万正文字符或8MiB JSON，拒绝生成截断文件 |
| D07_NESTED / D08_DUPLICATE_ID | author嵌套或ID重复，消息结构有歧义 |
| D10_CHANGED | 采集、转换或复核期间内容/页面改变，等待稳定后重试 |
| N01_PAGE / N02_TIMEOUT / N03_* / N04_RENDER | 页面状态、15秒回调超时、读取解码或客户端转换失败 |
| S01_CACHE / S02_APP / S04_URI / S05_WRITE | 缓存、打开应用、保存位置或写入失败 |
| S03_CANCELLED / S00_SAVED | 用户取消或写入流成功关闭；S00仍应打开文件核验 |
| P01_LOAD_TIMEOUT / P02_START / P03_JOB_FAILED | PDF渲染超时、无法打开系统打印、任务失败 |
| P04_CANCELLED / P05_JOB_COMPLETED / P06_FINISHED | 打印取消、系统任务完成、适配器结束；不据此认定保存文件内容正确 |

菜单“导出诊断”支持成功后复制。PDF不在onPause/onResume时提前释放打印WebView，而在适配器结束或Activity销毁时清理。用户离开后回来，系统任务失败可被记录；诊断不是系统打印服务所有错误的完整日志。公共SDK无法新建layout/write结果回调包装器，因此只记录这两阶段的调用、页范围与公开PrintJob状态，不能把阶段到达当作布局或写出成功。

## 验证状态

本地Chromium合成检查10场景通过：已加载400条、首尾/代码/表格/TeX、控件及隐藏内容过滤、危险链接/属性剔除、只读行为、复核变化、生成中拒绝、ID重复、数量/字符超限与origin/route拒绝。真实账号与用户真机未执行，完整历史和富文本保真仍待用户检查。

Android35新增5项仪器夹具已全部通过：HTML经SAF保存、Markdown取消重试及FileProvider分享、错误诊断复制与脱敏、系统PDF取消、实际系统PDF保存多页并检查首尾/代码/表格文字。构建、lint、签名检查和Chromium10场景均通过。下载Android证据独立核对 `OK (5 tests)` 和PDF文本标记：USER-FIRST、ASSISTANT-LAST、CODE-LAST、TABLE-LAST；实际PDF为4页A4、140385 bytes、Skia/PDF m124，SHA256 `0d2493bdf3aa1d637e941f0d78d25642548d1c6247c730c798e7c230a37109bb`。

不包含全量登录/输入/分享回归，也未验证所有Android/WebView版本。原旧测试文件保留用于追溯，旧export-validation工作流改为手动入口；不得把它的旧内部读取器结果算作新方案通过。

首次Android35运行 `37450098092`：5项中4项通过，PDF最终保存失败（系统打印提示Sorry, that did not work，未进入Save界面），没有发布。后续将WebView写出改为完整页范围，由系统处理用户选择的页，并增加layout/write调用阶段诊断；必须重跑实际文件检查，不能把预览通过当成保存通过。

## 真机明确助手turn无作者：有限补采批次待CI

e56c225快速采样N00_READY，总224ms；12普通作者均采集（6用户/6助手），13个可见turn，其中domIndex3 declaredRole=assistant/authorNodes0/visibleTextNodes4/visibleTextChars124；无截断。结合上一条定位可见作者外Markdown，明确旧采集器只补article导致这类独立助手会话容器漏采。其他12个turn外侧文字83字符大多署名/UI，不自动采集。12层定位未显示第3个turn标签，不能声称已拿到完整原始DOM或真实ID。

本批将有限fallback扩至带conversation-turn testid且data-turn=assistant的容器，仍排除隐藏、自身/后代含作者、嵌套容器与未知角色，保留原article兼容。滚动容器选取及装饰spinner排除、文字定位来源同步同一范围。未知channel仍assistant-unknown，不猜commentary；缺ID单快照保留正文及警告，跨窗口滚动仍H02，不伪造ID/按文本去重。版号保持1.4.0/code14，未发布Release。

新增浏览器检查覆盖无ID正文顺序/控件排除、验证签名覆盖新增正文、DIV/SECTION标记容器、未知角色/无testid/隐藏拒绝、正常作者不重复，以及滚动已知ID/缺ID、定位与实际转换对照。Android新增无ID助手SECTION经快速入口→SAF保存→诊断脱敏及顺序检查，CI待执行。用户原第12条实际正文位置仍须新批次复测，不仅核对count，完整历史仍not-proven。历史section-fallback脚本固定读取e56c225旧源码用于前后对照，不冒充当前APK测试。

## 用户授权Gecko导出：三格式保留，批次待构建

用户要求参考Firefox保存PDF并选择引入Gecko、接受大包及更多改动。现在PDF引擎改为官方GeckoView140.0.20250707120347（固定兼容compileSdk35），调用Firefox相同公开saveAsPdf接口，净化导出HTML→私密无JS会话→PDF流→后台写缓存/PdfRenderer验证页数→直接SAF保存，不再打开PrintManager打印机界面。HTML/Markdown保留，独立无作者助手turn补采一起交付。聊天WebView、登录与输入组件未迁移，不把PDF渲染器换成Gecko当历史完整性修复。

Gecko仅加载本地data HTML，原导出CSP拒绝外部资源，关闭JS/调试输出，结束/失败/销毁关闭会话，异常分类G01–G07和engine/version/pages/bytes诊断无正文。增加依赖官方Maven及MPL许可/来源文档。版本仍1.4.0/code14，不公开Release。Android两项PDF检查替换成直接SAF取消/重试和多页实物、包含缺ID进度；脚本预期18项并检查PDF-PROGRESS。

旧37505642553环境卡住已取消；37506735779构建/lint/签名成功，但Android35虽然18项通过，脚本仍要求旧17导致失败；Android36另有旧htmlSaved测试JS回调超时，新助手SECTION测试通过。以上不写成全CI通过，本批修正脚本并移除系统打印UI路径后重新验证；旧f943c83包未交付，下载核对的APK1896898bytes/SHA037754bdfc0748133fbafe5f89dfc098ea6db6c12e55d3a01f3d9c9da0730693不是Gecko批次。用户完整历史not-proven/附件原文件不含/旧403未复测。
