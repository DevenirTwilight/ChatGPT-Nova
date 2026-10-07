# 真实官方导出的本地验收清单

真实原始ZIP由用户保存在手机/电脑。用户后来主动提供15个Drive分卷，开发环境已在仓库外私有目录合并并做结构/CRC检查；没有提交真实内容到GitHub/CI，CI只使用完全虚构的schema等价fixtures。请只反馈统计、固定错误码、是否通过和测试数量；不要反馈title、正文、ID、姓名、邮箱、附件文件名、路径、截图、真实HTML/MD/PDF或其hash。

## 准备与旧版失败

1. 如果当前仍是已验收的旧版7854f3ea，请先选择同一真实ZIP：预期SAF SIZE已知在复制前A05_ARCHIVE_TOO_LARGE；SIZE未知在复制超过256MiB后A05。记录实际错误/阶段，不能以预期冒充结果。若已升级，标旧版real runtime not verified，不删除本地档案来制造测试条件。
2. 使用[最终CI签名Artifact](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37634607647/artifacts/11487454505)（buildRevision1990cb1ffb7a0832e94c552cc349023e6e5989d9） `nova-frozen-page-apk` 的ChatGPT-Nova.apk覆盖安装，原包名/签名/code14保持；不公开Release。新旧版号一样，以“复制诊断”的buildRevision确认源码。正式Artifact默认legacy=false；不要误装实验Legacy或测试APK。
3. ZIP保持在系统SAF可读位置。当前样本353.40MiB；复制前约需513.40MiB私有卷空闲（ZIP+128MiB DB/WAL规划+32MiB余量）。SIZE未知时持续检查160MiB空闲；这只是SAF复制阶段的预算，不是任意数据的充分空间保证。附件恢复阶段还会在ZIP cache之外规划新增asset bytes；本样本约147.36MiB附件，因此初始空间原始ZIP约661MiB、当前重打包副本约662MiB（ZIP+assets+128MiB DB/WAL+32MiB余量），实际DB/WAL和provider行为仍需留额外空间，不把此值当充分保证。
4. Archive内容全部本地。可以保留现有档案；如果既有数据会影响总行数，则使用导入统计区分本次输入，不能把全部库总数误认为新增量。不清在线登录。

## Level2 — container compatibility

在新版本 Settings → Nova Archive → 导入ChatGPT数据选择ZIP。输入容器通过、两个conversations JSON能被解析后，结合用户已做的non-ZIP64/605-entry结构审计记录container stage passed。若导入报错，反馈固定A码；单凭新限额不能写Level2通过。

开发环境已独立检查手机重打包container：607 entries含2目录，两个conversations JSON，122 conversations/7250 mapping/7130 messages；它不是原官方ZIP的逐字节验证。原始官方ZIP仍应由用户在手机上测试。

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

特别确认至少一个原Map.text被误当附件的multimodal文字现在显示；附件从本次ZIP精确恢复到Nova私有目录，不联网下载；未知非文本明确占位。包含thoughts/recap的会话检查普通答案不受影响、摘要标签正确、没有误导的未知空内容占位。

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

## Attachment Restoration：AR3–AR5 手机验收（新 schema2 构建）

上方1990cb1链接属于历史schema1兼容修复，**不能用于附件恢复验收**。使用[附件恢复正式签名APK](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37677076099/artifacts/11507044105)（buildRevision787f18f5296e7bbf4148146c21c9c7656e8c7859），完整证据见[37项报告](archive-attachment-validation.md)；以诊断buildRevision识别，不按相同versionName判断。当前正式包签名/源码/默认flag与全套fixture输出已核对；真实手机恢复与人工验收尚未完成。覆盖安装，不卸载/清空已有数据；schema1→2只新增表，升级不会自动重新读取旧ZIP。请主动重新导入一次，才能为已有会话补回附件binary与引用。

1. 导入原始官方ZIP，记录新附件数、更新/跳过数、引用数、不可用数、MIME不一致数、assetBytes/assetStorageBytes及导入耗时。当前结构审计参考：unique326，PNG82/JPEG148/DOCX82/PDF5/XLSX5/unknown4，mapped154521964bytes；实际统计可因已有档案/输入不同而变化。unknown卡片不自动打开为图片。
2. 同ZIP二次导入，预期没有复制同一binary或额外重复引用；首次来源保留，最近来源更新。用计数与存储量核对，不反馈真实文件名/ID/hash。
3. 完全退出/强制停止应用后重启；在至少5个会话抽查附件。移动或删除**测试副本ZIP**、撤销其SAF授权，再开启飞行模式；已恢复图片仍应可读，文档仍应能用系统应用打开。请保留原始官方备份，勿删除唯一副本。
4. 至少PNG2、JPEG2、multimodal1、attachment-only2、PDF2、DOCX2、XLSX2（若没有对应类型则明确not available）。只反馈类型、数量与pass/fail。检查图片在原消息中的前后文字顺序、重复引用不会重复展示、未知/缺失有卡片说明。
5. PDF/DOCX/XLSX通过系统外部应用打开；没有兼容应用时Nova应明确提示，这是not verified，不是文件损坏。它们不被合并到会话PDF页内。
6. 在同一阅读scope导出HTML/Markdown/System Print PDF。HTML只在单图2MiB/累计6MiB范围嵌入PNG/JPEG，超限保留描述；Markdown保留可编辑正文和附件描述，不携带binary；PDF应包含实际图片和文档描述。检查首/中/尾、角色、代码/表格/Unicode；文件留在手机，不上传真实输出。
7. 如测试删除，使用Archive自己的删除入口，确认档案及私有附件移除，原ZIP和在线登录保持。不要在重要档案上卸载应用来模拟删除。

AR1 synthetic / fixture passed；AR2 compatible repack容器/map/host binary复制与图片完整decode passed；AR3真实Android恢复、AR4真实重复导入、AR5真实内容人工核对仍pending，不能用JVM/模拟器通过替代。可安全反馈：buildRevision；first-import；asset counts/bytes；second-import；restart；source-ZIP-unavailable；airplane-mode；每类型抽查数量与pass/fail；HTML/MD/PDF数量与pass/fail；固定A码；ANR是否观察到。请勿发真实截图、导出文件、聊天内容或文件名。
