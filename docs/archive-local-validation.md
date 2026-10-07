# 真实官方导出的本地验收清单

真实ZIP由用户保存在手机/电脑，本轮不上传开发环境/GitHub/CI。开发与CI只使用完全虚构的schema等价fixtures。请只反馈统计、固定错误码、是否通过和测试数量；不要反馈title、正文、ID、姓名、邮箱、附件文件名、路径、截图、真实HTML/MD/PDF或其hash。

## 准备与旧版失败

1. 如果当前仍是已验收的旧版7854f3ea，请先选择同一真实ZIP：预期SAF SIZE已知在复制前A05_ARCHIVE_TOO_LARGE；SIZE未知在复制超过256MiB后A05。记录实际错误/阶段，不能以预期冒充结果。若已升级，标旧版real runtime not verified，不删除本地档案来制造测试条件。
2. 使用新CI签名Artifact `nova-frozen-page-apk` 的ChatGPT-Nova.apk覆盖安装，原包名/签名/code14保持；不公开Release。新旧版号一样，以“复制诊断”的buildRevision确认源码。正式Artifact默认legacy=false；不要误装实验Legacy或测试APK。
3. ZIP保持在系统SAF可读位置。当前样本353.40MiB；复制前约需513.40MiB私有卷空闲（ZIP+128MiB DB/WAL规划+32MiB余量）。SIZE未知时持续检查160MiB空闲；这不是任意数据的充分空间保证。
4. Archive内容全部本地。可以保留现有档案；如果既有数据会影响总行数，则使用导入统计区分本次输入，不能把全部库总数误认为新增量。不清在线登录。

## Level2 — container compatibility

在新版本 Settings → Nova Archive → 导入ChatGPT数据选择ZIP。输入容器通过、两个conversations JSON能被解析后，结合用户已做的non-ZIP64/605-entry结构审计记录container stage passed。若导入报错，反馈固定A码；单凭新限额不能写Level2通过。

本轮605 entries与两JSON大小来自用户先前审计，开发环境未独立读取。需要新增独立central-directory证据时，在用户本地运行有界结构审计；不要把ZIP上传来完成审计。

## Level3 — import and restart

- 记录导入成功/错误、buildRevision和脱敏诊断；fresh档案预期122会话、7250 mappingNodes、7130 messageNodes（含thoughts/分支，不含120 null-message）。currentBranchMessages约4267，fallbackConversations预期0。列表message数含已保存thoughts，Reader仅计可见记录，两者不同不表示丢raw。
- 诊断contentTypeCounts预期约text3216/multimodal_text892/thoughts1874/reasoning_recap1148；统计若不同，先解释输入/已有数据/审计近似，不能只看122标题认定无损。
- 记录copyDurationMs、durationMs（数据库导入）、totalDurationMs、databaseBytes（关闭DB后主库）、temporaryBytes、freeStorageBefore/After、sampledHeapPeakBytes、sampledWalPeakBytes。采样heap是整个应用used heap，WAL只是采样高水位，不当作精确profiler峰值；0可能表示没有测到。
- 导入完成后force-stop/完整退出，再启动Archive。列表/会话/current branch可读；本次导入不会自动恢复/重新扫描。诊断中的临时bytes是曾复制量，不是仍残留量；普通release私有目录不能通过run-as直接查看时，临时文件删除只能标自动fixture通过、设备直接验证not verified。
- 在线ChatGPT登录保持（仅在原本已有登录时确认）；没有真实账号就标not verified。

## Level4 — duplicate import

同一ZIP再次选择。预期parsed122/mapping7250/message7130保持输入量，newConversations/newMessages/updatedConversations/updatedMessages为0，skipped约7250（包括null根）。缺ID保守新增策略仍适用；若真实文件有缺ID，明确解释差异。不能按正文去重。

列表不简单复制；已知会话first_import保留/latest_import更新，来源记录增加一次；DB大小增量只应是来源统计/页面开销。比较诊断databaseBytes与输入/新增量，不根据单次DB大小不完全相等宣称重复导入失败。源码/fixtures验证时间与身份策略，用户release UI不暴露全部数据库内部字段；未直接检查的字段标not verified。

## 取消与生命周期（本地单独记录）

需要取消验收时，避免在已有重要档案上使用卸载/清除数据：SAF复制过程中点取消，已有列表不变；解析阶段取消，事务回滚；计时取消点击到固定A07。无界阻塞provider只能尽力中断，记录实际延迟。

导入中旋转保持任务；真正退出后取消；process death后下次启动清理旧temp且不继续导入。记录是否界面可操作/ANR；没有实际验证时留pending。SQLite一致性、取消、清理和旋转已有synthetic自动回归，也不能当作真实大文件设备通过。

## Level5 — 用户本人至少5个会话

只用ordinal1–5记录pass/fail，不写标题或正文。覆盖短普通、长会话、编辑/重新生成分支、代码/列表/引用/表格/Unicode、multimodal及有附件metadata的会话；检查开头、中间、结尾、角色顺序和current_node主链。全部节点安全视图仍保留分支；thoughts在任何阅读/导出scope隐藏，但数据库raw保留；reasoning recap单独“推理摘要”。

特别确认至少一个原Map.text被误当附件的multimodal文字现在显示；asset不联网下载，未知非文本明确占位。包含thoughts/recap的会话检查普通答案不受影响、摘要标签正确、没有误导的未知空内容占位。

选若干会话在本地分别导出HTML/Markdown/System Print PDF，核对相同scope和object text/摘要、thoughts隐藏、代码表格Unicode/首尾；记录各格式抽查数量/pass/fail，文件只留用户设备。完整性依赖官方导出本身，不能推出服务器完整或任意未来格式兼容。

仅用户本人明确确认至少5个会话后可标Level5：real content sample manually validated。在此之前始终manual content validation pending。

## 可安全反馈的模板

```text
buildRevision=（修复源码SHA，不是私有文件hash）
environment=physical-device local / emulator local
old-version-result=A05 / not verified
first-import=pass / error code / pending
parsed=...
mappingNodes=...
messageNodes=...
currentBranchMessages=...
displayableCurrentBranchMessages=...
fallbackConversations=...
contentTypeCounts=（固定类型与数量）
copyDurationMs=...
durationMs=...
totalDurationMs=...
databaseBytes=...
sampledWalPeakBytes=...
sampledHeapPeakBytes=...
temporaryBytes=...
freeStorageBefore=...
freeStorageAfter=...
restart=pass / pending
same-ZIP-second-import=pass / pending
newConversations=...
newMessages=...
updatedConversations=...
updatedMessages=...
skipped=...
manual-reader-samples=0 / 数量；pass / pending
local-HTML-samples=0 / 数量；pass / pending
local-Markdown-samples=0 / 数量；pass / pending
local-PDF-samples=0 / 数量；pass / pending
cancel-latency-ms=... / not verified
ANR=none observed / observed / not verified
```
