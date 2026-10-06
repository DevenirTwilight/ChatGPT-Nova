# Shared Conversation 数据源：已停止升级

2026-10-06，经用户提供的真实公开共享链接授权，在Chromium实际加载后验证。链接、正文、标题和消息ID不入库。桌面Chromium151.0.7922.173及412×850移动视口均观察到虚拟化，不等同Android真机验证。

顶部8个公开作者节点、5411正文字符、26个pre；底部6节点、1866字符、0个pre。顶部第一节点随后脱离DOM，正文也不在底部任何作者节点。桌面首标记在顶部存在、底部不存在；尾标记两端存在。初步仅滚document无效，后续正确选择消息祖先中的实际可滚动容器。移动再次运行还出现15秒SHARE_NOT_STABLE；不重试无限等待。

因此不适合作为“一次DOM冻结完整会话”的已验证来源，停止Android/UI/三格式Shared实现，不堆滚动缓存workaround。未验证同URL更新、总消息数、中间覆盖或官方progress是否包含。最新产品目标改回当前网页普通HTML/Markdown/System Print，MHTML取消。

`SharedConversationSource.cjs`是独立可行性探针，不是生产数据源。`waitStable`最多15秒、250ms轮询、四次相同稳定观察；两次端点移动仅用于判虚拟化，不合并/抓全历史。诊断只有结构、字符数、有限fingerprint与标记布尔值，不输出私密URL/正文。源码不自动创建分享，不读Cookie/认证/React/私有reader。

运行（显式提供已创建共享链接，不把私密参数写进仓库）：

```sh
NOVA_SHARE_URL="$your_explicit_share_link" node tools/shared-source/probe.cjs
node tools/shared-source/browser.test.cjs
```

需要playwright-core与Chromium，支持NOVA_PLAYWRIGHT_MODULE/NOVA_CHROMIUM_EXECUTABLE。可选NOVA_SHARE_DESKTOP=1、NOVA_SHARE_FIRST_MARKER/LAST_MARKER只输出存在性。测试7场景包括同条数虚拟窗口、非虚拟化、登录按钮不误判、loading上限、不可用页及URL变化；fixture不代表真实全量覆盖。
