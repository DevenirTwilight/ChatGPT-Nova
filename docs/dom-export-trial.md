# 新方案导出试用版

## 1.3.9 修正版：D09 与正文定位

用户实测1.3.8在Android36/WebView153.0.8010.36的普通会话报D09：找到5条消息、无缺失/重复ID、已解析2个代码块，但没有指出哪条正文失败。用户确认全是文字消息（可能含代码）。这是第一次用户真机新路线失败证据，不是旧403，也不代表完整历史覆盖结论。

已在合成页面复现一种对应缺陷：`getClientRects().length > 0` 会把 `display:contents` 正文包装层误判为不可读，导致有文字却返回D09；也可能漏掉页面外已加载的延迟布局内容。没有用户实际DOM，不能断定该布局就是其唯一根因。

1.3.9-dom-trial/code13修正：正文可读性改为检查自身与祖先的hidden/aria-hidden、display:none、visibility隐藏和content-visibility:hidden；不再要求自身有布局框。明确隐藏的正文仍过滤，offscreen的content-visibility:auto及display:contents可读取。`.markdown`为空时，只尝试同一author容器的结构化过滤，不使用原始textContent绕过隐藏/控件过滤，不跳过失败消息。复核签名覆盖完整author子树，包含备用正文来源。

新诊断scheme为DOM-TRIAL-2、source为read-only-dom-v2。D09记录失败消息序号/角色、author与选中正文字符数、选择器命中数、过滤类别、布局状态、图片/媒体/折叠计数和已处理条数；不含正文、HTML、ID、类名、标题、URL或登录凭据。弹窗会指出第几条用户/助手消息失败。字段read-only-dom只描述采集方式，不是失败码。

本地Chromium17场景通过，包含旧版失败的无布局框正文、无布局框author、已加载但页面外的延迟布局、祖先隐藏过滤、正文选择器为空时的安全备用、备用来源变化复核、真正空正文的拒绝与诊断脱敏。Android35/36各新增两项对应夹具（共7项/版本），运行结果以修正版本工作流为准；真实用户原会话仍需复测，完整历史仍未证明。发布检查不把合成复现当成用户问题已解决。

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
