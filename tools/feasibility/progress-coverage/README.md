# 助手进度遗漏结构诊断（v2已随e56c225测试APK交付）

用户逐条对照30条，第12条进度缺失，正常主消息29条保留，整条文本重复为0。本工具帮助区分具体结构原因，不是进度采集修复或完整性证明。

`probe.js` 仅允许chatgpt.com普通/项目会话主框架，读取公开DOM结构，不调用网络，不访问React、Cookie/storage，不改页面、滚动或持久化。返回原始角色节点、允许角色与可见节点数、作者隐藏分类、可见turn无作者节点、非空Markdown之外的可见文本节点数/字符数。仅报告少量位置数字/白名单角色，绝不返回正文、HTML、ID、标题或URL。最多1000作者/1000turn、20000文本节点、2秒预算、每类80个结构样本；超限标truncated/error，不当成全量。

`turn`候选仅为带公开conversation-turn testid或data-turn标记的article，这是假设性结构诊断范围，不保证匹配实际页面。Markdown之外可能是进度，也可能是署名/状态/普通UI；无作者turn也不能自动断言为进度。不得把候选直接全部转成助手消息，不得放开隐藏分支或将控件标签导出。

运行合成检查：

```sh
NOVA_PLAYWRIGHT_MODULE=/tmp/nova-dom-deps/node_modules/playwright-core node tools/feasibility/progress-coverage/probe.test.cjs
```

10个虚构结构诊断场景包含普通助手标记下进度本就能采集、进度位于非空Markdown之外被当前正文选择遗漏、独立turn缺作者标记、tool角色、隐藏节点、同文不同ID不合并、缺ID、超限和错域；检查只读与输出脱敏。通过不能证明真实第12条采用这些结构。

本批已把同字节探针放入app assets，快速采集及滚动终止时执行，诊断位于dom.progressDiscovery；不在每个轮询执行。采集器现新增明确assistant turn缺作者的有限补采和Markdown之外显式commentary正文，不把无角色候选自动纳入。当前用户尚需确认Nova历史页能否直接看到/展开看到第12条，还是它只存在于原始对照。旧0df561e APK没有本探针，8ed24f7批次已通过Android35/36各16项并直接提供安装包（版本仍1.4.0，无Release），也没有要求用户搭建ADB或执行脚本。若页面可见，下一批可将该只读诊断接到原生复制诊断后取得结构证据，再实现精确的消息类型/身份映射；若页面已经不保留，改选择器和反复滚动都不能补出，应检查用户提供的独立官方导出等材料是否包含该条。

进度/最终回复若共用父消息ID，须识别稳定子节点/分片身份，否则按父ID合并会丢条。role+顺序+DOM对象仅能用于当次快照/对象生命周期，不能跨虚拟化卸载/重建视作稳定ID。本工具不读取真实身份，不实现滚动去重回退。产品实现继续禁止按文本相似度或hash合并不同历史消息。

## 未接入安装包的v2诊断

用户确认遗漏的进度在Nova原页直接可见，但未提供新采集JSON。v1会跳过已含作者的turn，漏统计作者外/turn内文字。`probe-v2.js`补只读outsideAuthor候选统计，`probe-v2.test.cjs`13个合成场景通过，包含作者兄弟进度、隐藏兄弟/按钮排除、turn本身是作者。v2返回source read-only-progress-discovery-v2，仍无正文/ID/URL，保留原预算。候选文字可能为普通UI，不能直接补采成消息。v2仅工具原型；APK仍用v1，probe.js与主asset一致，未另做安装包/版本。

```sh
NOVA_PLAYWRIGHT_MODULE=/tmp/nova-dom-deps/node_modules/playwright-core node tools/feasibility/progress-coverage/probe-v2.test.cjs
```

## 当前定位批次（待CI/交付）

已收到8ed24f7真实诊断，普通作者31全部采集，但article turn探针0；该JSON无法定位仍可见的目标进度。工作分支probe.js/main asset已同步v2，支持非article turn及作者外文字；probe-v2.js保留独立可运行副本，旧8ed24f7包仍用v1。新增定位asset及原生“导出诊断→定位缺失文字”入口，见tools/dom-trial/locator.test.cjs；输入4–160字片段，JSON仅结构/可见性/数字位置和本地转换结果匹配位置，绝不返回查询/正文/ID。当前加载范围无匹配不等于历史不存在；隐藏匹配只是过滤诊断，不会自动导出隐藏文字。

## e56c225交付状态

CI37495451307构建/lint/原签名、浏览器及Android35/36各17项通过，版本仍1.4.0/code14，无Release。v2通用turn/作者兄弟结构计数现已包含在主asset，canonical probe.js与probe-v2.js一致；8ed24f7旧包仍是v1。定位入口为导出诊断→定位缺失文字；在页面显示目标后输入独特片段并复制JSON。16个定位场景通过，实际原会话定位结果尚待用户反馈。详见docs/handoff/latest.md。

## 真机无匹配的范围对照

用户e56c225定位只有6个原始作者/L01且未超限，输入114字符未超160；当前是否显示目标尚待确认。`locator-window.test.cjs`5个合成检查说明：换成6节点窗口可导致找不到，也可能是精确字串差异，或目标位于被排除的可见按钮。原会话根因未确认，不以此宣布换窗口就是修复。沿用现有安装包，先用短独特片段定位；此轮仅工具/文档，未改生产代码。

```sh
NOVA_PLAYWRIGHT_MODULE=/tmp/nova-dom-deps/node_modules/playwright-core node tools/feasibility/progress-coverage/locator-window.test.cjs
```

## e56c225真机L00：可见作者外Markdown遗漏，外层turn待确认

用户短片段定位返回L00_MATCH，总153ms，作者raw12、703文本节点/6736字符、未截断。3处匹配中第1处authorIndex0/role absent/selectedfalse，aria-hidden/hidden/CSS隐藏均false；STRONG→P→Markdown DIV后多层DIV，12层内无角色/turn/channel/ID/testid。其最近作者查询不受12层限制，因此确实位于作者容器之外。另两处属于已选助手作者4/12，其中第12处为表格引用；本地转换D00/count12/matchingMessageIndexes[4,12]。支持可见作者外正文漏采机制，不把后续引用当原进度或按文字去重。

第2处普通助手的外层为SECTION，data-turn=assistant且有conversation-turn testid；第1处链条截于12层，尚不能断定其外层同样SECTION、独立turn还是含普通作者的兄弟。exportSourceHasIdfalse只查询当前作者/article候选，不证明更远SECTION无ID。不能将所有无作者Markdown直接视作助手，亦不能据count12证明完整历史。

已请用户保持目标位置，在现有e56c225包快速导出已加载消息后取消格式选择，再复制导出诊断；v2的turnSamples/outsideAuthorSamples可补外层角色和作者兄弟归属，不需重新安装/保存HTML。等待该结果后集中精确补采。独立新增section-fallback.test.cjs，5项工具内方案检查通过：当前漏独立显式助手SECTION；扩展候选后采集；未知角色/隐藏SECTION不采；含普通作者的SECTION兄弟正文仍漏。仅合成假设验证，未改生产源码或新增APK。缺ID的quick snapshot保留警告，滚动仍H02，不能伪造跨窗口身份。修复与真实完整性验证尚未完成，旧403及附件原文件仍未验证/未包含。

```sh
NOVA_PLAYWRIGHT_MODULE=/tmp/nova-dom-deps/node_modules/playwright-core node tools/feasibility/progress-coverage/section-fallback.test.cjs
```
