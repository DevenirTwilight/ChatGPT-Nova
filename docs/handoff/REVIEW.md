## A6现场补充

API35 CI37550505533仅Firefox Save as PDF失败，More正常展开但按钮在ScrollView下方。证据11452764196；新测试通过公开菜单ACTION_SCROLL_FORWARD查找，不修改生产保存实现、不滚聊天页。API36尚需读取。未取得Firefox实物，不记A/B通过，不开始B。

# 验证与审查（2026-10-07）

最新状态以实际远端及docs/handoff/latest.md为准。当前路线普通HTML为唯一快照，HTML/MD同clone，PDF打印同HTML；停止Share/MHTML/完整历史workaround。

| 证据 | 实际结论 | 边界 |
| --- | --- | --- |
| source462ba8c CI37548381063 | build/lint/签名/浏览器、Android35/36新10/旧18/所有容器输入与v1升级通过 | 整体失败仅Firefox菜单操作；unit NO-SOURCE |
| 实际System Print UI保存PDF | 两API均31页，首尾/中法英/长代码/表格/数学/320×120图片/AFTER隔离通过 | 受控模拟器，非真机/真实账号；不是预览字节 |
| Android SAF真实HTML | Chromium143/Firefox144/Edge154普通file打开及白底printCSS通过 | 外链图片/附件不完全离线 |
| MD实际UTF8保存 | 代码fence/列表/表格/引用/链接/中法英及mutation隔离通过 | 同clone文本投影，不是服务器历史基准 |
| Firefox Android157.0.1 | A5正文可见，More Collapsed exact标签导致菜单未打开，尚无PDF | 89b5176修正后CI37550505533运行中，不提前认证 |
| 长fixture | 长代码/表格/31页PDF；浏览器额外100+屏场景通过 | 合成内容不证明真实完整历史 |
| APK基线 | A5 universal681117497字节，installedBaseApk同值 | du权限不足，总安装占用未知；Gecko尚未移除 |

阶段B只有在A全部关键实物检查通过后开展。旧Gecko仅ConversationExport PDF调用，无其他实际用户；不得为移除引擎重构输入/登录上传等。两Gecko专用测试明确退役，剩16旧例继续，保留所有Git证据。新PDF自身已走SystemPrint，但整个productiondependency尚保留Gecko，不能声称已彻底退出。

新静态HTML剥除脚本/事件/应用控件/SPA样式，CSP禁JS/connect/frame/form，仅允许data或HTTPS图片；独立打印WebView无桥/JS/storage，无Cookie/Auth静态资源请求，有总量上限。诊断仅origin/随机snapshotId/字符数/格式/阶段/版本与耗时，不记录正文/真实ID/私密query/认证。HTML源有内容及链接，属于用户主动本地保存，不上传第三方。

最终仍须真实设备/账号人工检查页面加载内容、登录上传/硬件权限与打印；模拟器合成回归不等价。这不阻止独立执行已授权的实现、CI及小包测量。不要再要求用户已未知的ADB条件，稳定包供其实际试用。

三种格式严格对应同一次当前网页冻结快照，但无法证明ChatGPT服务器端完整会话历史；如果网页虚拟化未挂载较早内容，保存结果也不会包含那些内容。

历史审查过程见Git中文档旧版本，不按旧HEAD回退已完成修复。普通更新不自动触发会话迁移。
