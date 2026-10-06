# 新方案导出试用版

## 224dae2 真机再次 H06：历史加载等待修正（待 Android CI）

用户安装224dae2/DOM-SCROLL-TRIAL-2在Android36、WebView153.0.8010.36仍H06：86.537秒、leg0、68步、缓存19条（10用户/9助手），未到顶部/底部、未二次核对。最后窗口5.382秒仅6次采样，列表变化4次、正文变化1次、高度变化4次、contentStable=1；用户观察到转圈未结束即中断。旧诊断无加载指示字段，不能断言真实转圈的DOM形态或唯一根因，也不能把前一APK说成已经修好。

源码确认固定5秒截止不识别历史加载。已独立对比旧224dae2与本修正：5.382秒才换入列表，旧报H06，新继续等待。新逻辑仅检查所选滚动区域内可见的公开aria-busy/progressbar/loading-spinner/animate-spin，正文装饰旋转、隐藏或区域外指示不算；加载时不缓存、不移动、不确认边界，消失后重新核对。无已识别指示时，消息ID/角色列表换入也给予新的5秒准备宽限；单纯同ID正文变化不延长宽限，缓存正文/角色冲突仍拒绝。

每个滚动窗口固定30秒总上限（列表反复换入/转圈不能无限延长），边界稳定核对也在该上限内；整次上限调整为10分钟，避免旧120秒在已经86秒却仍第一轮的真实反馈中提前截断两轮扫描。300步、1000消息、正文/字节限额、15秒单次回调、取消/清理不变。新增loadingSignals/loadingPolls/loadingObserved、readyAgeMs/windowLimitMs/scanLimitMs诊断及“网页正在加载历史”界面提示。指示选择器是有限兼容尝试，不保证识别真实网页所有转圈；完整历史始终not-proven。

本地Chromium17单快照+24滚动场景通过：实际6.5秒转圈后40条核对/位置恢复、无转圈的迟到列表宽限、永久转圈/不断换入在30秒拒绝、取消清理、装饰/隐藏/区域外指示忽略；旧正文持续变化和顺序/二次核对拒绝仍通过。新增Android8秒延迟加载→HTML保存40条测试，Android35/36共13项尚待本次CI运行，不把本地结果说成Android通过或真实历史成功。版本保持1.4.0/code14，scheme2，通过buildRevision区分；只做CI验证，不自动Release，本修正未交付安装包。下一步核对CI、原签名APK和新增仪器结果，再按用户授权直接提供同版本测试包复测原会话。

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
