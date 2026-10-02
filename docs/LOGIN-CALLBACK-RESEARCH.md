# 浏览器登录回调研究（2026-10-02）

## 结论与实现决定

在继续使用 chatgpt.com 网页、Nova 独立 WebView 数据、原 applicationId / 签名、不调用 OpenAI API、不冒用官方认证客户端的约束下，尚无公开且受支持的实现可以完成“Chrome / Brave 登录 → 自动返回 Nova → 建立 Nova 的 ChatGPT 网页会话”。本轮仅更新研究记录，不改变登录代码、版本或 APK。

不能把这一结论归因于“已证明 OpenAI 后台只接受官方包名和签名”。此次没有取得该项后台校验的直接证据。实际阻塞点是没有给 Nova 的 chatgpt.com 网页会话提供可用的服务端交接契约。单独增加 deep link、Custom Tabs 或 PKCE 无法补出这个契约。

## 已确认的证据

| 项目 | 证据与适用范围 |
| --- | --- |
| 官方 Android 浏览器登录 | OpenAI Android 帮助文档列明 Chrome / Brave 是支持的登录浏览器；并未公开 Android 客户端的完整授权请求或注册配置。 |
| Android 原始观测 | 2026-09-16 发布在 Brave 官方项目的原始问题报告，对 ChatGPT 1.2026.251 做同设备对照，记录 Chrome 打开 CustomTabActivity，成功认证后出现 com.openai.chatgpt://auth.openai.com/...。此为报告者的一手观察，不是 Nova 本次实测；路径在原始日志中被省略。 |
| 普通 Custom Tabs | 使用浏览器的 Cookie 和权限上下文；浏览器回调和 Nova 的 WebView Cookie 是两个独立问题。 |
| Auth Tab | Chrome 可把匹配的返回 URI 作为 ActivityResult 交给应用。HTTPS 回调需 Digital Asset Links 验证；接收 URI 并不同时授予网页登录会话。不能据此声称官方 ChatGPT 当前使用了 Auth Tab。 |
| HTTPS App Links | 网站的 assetlinks.json 必须授权具体 package_name 和签名证书 SHA-256。Nova 不能单方面在 OpenAI 的域名上发布这一关联。此为 Android 的域名关联规则，不是 OpenAI OAuth token 服务校验签名的实测证据。 |
| 第三方公开 OAuth | OpenAI 现已公开 Sign in with ChatGPT。开源 / 本地应用可按公开流程动态注册，获取应用自己的 OAuth 凭据；身份集成与 ChatGPT plan usage 是不同权限。不能再笼统说第三方完全没有官方回调能力。 |
| 凭据用途 | 开源流程的 resource / token audience 为 https://api.openai.com/v1，供授权的 Responses API 请求。身份及 plan usage 均不授权读取 ChatGPT 对话；公开文档没有给出把这些 token 转换成 chatgpt.com 网页 Session Cookie 的接口。 |

## 当前公开注册流程（与官方 Android 私有客户端配置分开）

OpenAI 的第三方开源流程说明：

- authorization endpoint：`https://auth.openai.com/api/accounts/authorize`
- token endpoint：`https://auth.openai.com/api/accounts/oauth/token`
- 初次注册使用文档规定的 `dynamic_agent_client`，提交应用真实名称和独立的安装标识；服务端回调返回签发给该注册的 client_id。后续交换及重登使用签发的 client_id。
- 每次授权生成独立 state、OIDC nonce 和 S256 PKCE。公开回调使用 `http://127.0.0.1:<port>/auth/callback`；注册后端口可变化，scheme、host、path 保持不变，同次授权与交换使用完全相同 URI。
- 返回的 OAuth token 按 client、用户和 workspace 范围管理；这是第三方应用会话，不是现成的 ChatGPT 网页登录 Cookie。

该流程是公开协议研究，并未执行注册、用户授权或 token 交换。文档中的 loopback 范例也没有在 Android 13/14/15 或 Brave 上实测。此次不会增加一个只有“返回 App”效果而没有 ChatGPT 网页会话的登录入口。

## Nova 的具体阻塞点

1. **服务端的客户端 / 回调许可。** Nova 可以声明自己拥有的回调地址，但仍需使用适用于该功能、由 OpenAI 允许的 client / redirect 组合。复制官方应用的 client ID 或认领 com.openai.chatgpt 回调不能作为 Nova 的授权方案。OAuth 本身要求登记和核对回调地址。
2. **网页登录会话的交接。** Nova 的 WebView 需要由 chatgpt.com 建立网站会话。公开第三方 OAuth 发出的应用身份 / API token 没有公开的网页 Session Cookie 兑换契约；Custom Tabs / Auth Tab 不会把浏览器 Cookie 交给 Nova。
3. **官方 HTTPS 域名归属。** 若通过 OpenAI 域名的已验证 HTTPS 回调自动唤起 Nova，需要 OpenAI 在服务器声明 Nova 的包名和证书。仅修改 Manifest 无法得到这个关联；即使关联存在，第 2 项仍需单独解决。

判断：如果改为使用官方 Sign in with ChatGPT 和 Responses API、由 Nova 自己实现聊天界面，存在第三方受支持的身份 / API 授权路径；这会改变本项目“只打开 chatgpt.com、不使用 OpenAI API”的目标，因此本轮没有实施。若坚持当前目标，需要 OpenAI 为第三方网页容器提供明确的授权及网页会话交接支持。

## 未能核实的部分

- 官方 Android 当前最新版本的完整 redirect URI、client ID、scope、token audience、PKCE 使用和签名 / Play Integrity 后台校验条件均没有在本次实测。
- 官方 web 登录自身所用的完整 redirect_uri 也没有动态核实；公开登录入口为 https://chatgpt.com/auth/login。公开存在某个 callback 网页，不证明它是第三方可用的注册回调。
- 读取公开 assetlinks.json / OIDC discovery 的请求未能取得内容，不能断言当前文件只列官方应用或不存在 Nova 条目。
- 自动审批拒绝打开 chatgpt.com 登录页，因为这可能暴露已有账号的私密内容，而本次授权仅限公开机制研究；没有绕过这一限制，也没有读取聊天、Cookie、验证码或用户 token。
- 从公共资料推导的“缺少受支持的网页会话交接接口”是公开能力边界判断，不是对 OpenAI 内部所有实现的断言。

## 来源

- [OpenAI：Android 登录浏览器支持](https://help.openai.com/en/articles/8194942-google-chrome-as-the-default-browser-for-the-chatgpt-android-app)
- [Brave 官方项目：2026-09-16 原始 Android 对照日志](https://github.com/brave/brave-browser/issues/30166#issuecomment-5693455861)
- [同一报告者在 Brave 社区的完整版本信息](https://community.brave.app/t/chatgpt-android-login-does-not-open-google-password-manager-passkey-in-brave-chrome-works/658337)
- [Chrome：Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs)
- [Chrome：Auth Tab](https://developer.chrome.com/docs/android/custom-tabs/guide-auth-tab)
- [Android：App Links 的包名 / 证书关联](https://developer.android.com/training/app-links/configure-assetlinks)
- [RFC 8252：原生 OAuth 的外部浏览器、PKCE 与回调登记](https://www.rfc-editor.org/rfc/rfc8252.html)
- [OpenAI：Sign in with ChatGPT Quickstart](https://developers.openai.com/siwc/quickstart)
- [OpenAI：开源应用注册和登录](https://developers.openai.com/siwc/token-sharing-open-source/sign-in)
- [OpenAI：token 的用途和 audience](https://developers.openai.com/siwc/token-sharing-open-source/token-reference)
- [OpenAI：ChatGPT plan usage 的授权范围](https://developers.openai.com/siwc/token-sharing-open-source)
