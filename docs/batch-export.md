# 逐会话批量导出

Nova Archive 首页选择“批量逐会话导出（HTML + Markdown）”，选择当前分支或全部分支，然后只选择一次 ZIP 保存位置。范围是当前标题筛选的全部本地会话，包含列表后续页；清空搜索即导出全部本地会话。此功能不连接在线历史。

ZIP 中每个会话有独立的序号与标题目录：

- `conversation.html`：可独立打开的聊天正文，含研究报告与预算内内嵌图片。
- `conversation.md`：Markdown 正文，研究报告同样按已恢复时间排列。
- `attachments/`：所选分支可读取的附件原件，HTML/MD 末尾提供相对链接。PDF/DOCX/XLSX 附件不会合并成聊天正文。
- 根目录 `manifest.json`：每个会话的范围、消息/报告/附件数量、缺失引用、警告与失败项，以及文件 SHA256。清单标题最多512字符，正文标题保留。

序号使相同标题不会互相覆盖；目录过滤路径字符。不同主题始终是不同文件，ZIP 只是统一保存的容器。研究报告位置说明、隐藏 thoughts、分支警告均沿用单会话导出。完整性仍取决于导入数据；校验成功代表输出文件没有在打包或保存时损坏，不证明源导出包含线上所有内容。

程序逐项处理并显示进度，可取消。旋转屏幕后保留任务和完成提示。单个会话渲染失败时继续并记入清单；读取/写入失败、取消或整体限额则不报成功，尝试删除未完成的新目标文件，不支持删除的保存服务会显示清理提示。不会更改档案数据库。

预算：最多10000会话、32000个ZIP成员、展开输出512MiB、manifest16MiB与300秒操作时限。私有临时包核验每个文件CRC/SHA后保存；保存后重新读取实际文件，核对同一ZIP及整个包SHA。需要能读写新文件的保存位置和足够临时空间。

批量提供HTML与Markdown。单会话“打印 / PDF”入口保持原有系统打印操作；不同会话不会被合并成一个PDF。

开发者可通过 `bash tools/archive/batch-local.sh INPUT.zip OUTPUT.zip PRIVATE_STAGING` 在仓库外生成相同逐会话包。脚本只输出汇总，真实ZIP、附件与正文禁止提交仓库。设备验收与真实host验收是两类证据，当前状态见[交接](handoff/latest.md)。

## 本轮验收（2026-10-08）

实际源码 `e702a77a974ec5a762f1cc30f3dbae4d923461ea`，正式[CI37757051125](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37757051125)所有job成功。独立JVM112，Android26实际22，35/36各Archive42与Stable43全通过。新批量3项含201跨页/筛选同名、取消、单次SAF/任务及完成态旋转/实际ZIP报告排列。旧实际17页Archive与31页Stable/System/Firefox PDF、图片、Unicode和报告时间顺序也独立核对；此处PDF是单会话回归，不冒称批量打印。

真实私有官方ZIP已自动逐份导出122会话（当前分支），失败0；独立核对1897条原始plain消息、4份97577字符研究正文、275个原件匹配源文件SHA、0缺失引用，以及每个HTML消息数量与全部ZIP CRC/SHA。产物只保存在仓库外，没有真实正文/标题/IDs/文件链接入Git/CI。这不等同真实手机导入SQLite或线上全历史已验证。

[原签名更新APK artifact11540247734](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37757051125/artifacts/11540247734)：2117745bytes，SHA256 `6cba6c30915c70a2b21d77001472020634c97803b65daf548f83e83f1552f52e`。原package/code14/签名保持，可覆盖安装；已有本地档案无需为批量功能重新导入。匿名证据见 `tools/archive/evidence/batch-e702-*.json`。未合并main或发布公开Release。
