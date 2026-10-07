# Archive 附件恢复：实现与验收报告

本轮保留Legacy研究代码/defaultfalse，并保持Stable Frozen Snapshot独立。官方导出内容由用户主动提供；不读取在线ChatGPT Cookie/backend/React state，不上传Archive数据。

| # | 项目 | 结果与边界 |
|---|---|---|
| 1 | branch | feature/export-conversation |
| 2 | source commit | 787f18f5296e7bbf4148146c21c9c7656e8c7859 |
| 3 | final commit | 本报告所在Git文档提交；完整SHA见最终交付说明，与上方已测源码SHA分开。 |
| 4 | CI | 正式[37677076099](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37677076099)；Legacy[37677076223](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37677076223)；全部job success；独立实物：JVM93、API26原生14、API35/36各Archive34+Stable43、Legacy各19+原Gecko2ignore。 |
| 5 | asset mapping schema | 已观察的string→string文件名map；sediment pointer与metadata file身份精确对应.dat；不按正文/filename/hash/顺序猜归属。 |
| 6 | real asset candidates | 326，来自用户授权的手机重打包compatible export。 |
| 7 | matched assets | 326，精确命中；此为真实host审计，不是手机DB导入结论。 |
| 8 | missing assets | 0；host复制326complete、unavailable0。 |
| 9 | images | 230：PNG82、JPEG148，按binary magic，不按扩展名。 |
| 10 | PDF | 5 |
| 11 | DOCX | 82 |
| 12 | XLSX | 5 |
| 13 | unknown | 4：保存binary与描述，不自动嵌入图片。 |
| 14 | total restored bytes | host临时复制154521964bytes并回滚；真实Android持久恢复量pending。 |
| 15 | private storage size | 真实手机pending；host回滚后剩余asset files0/pending0。 |
| 16 | DB size | 真实schema2手机DB pending。 |
| 17 | asset import duration | 真实Android pending；host有界复制/解析2495ms，非手机导入耗时。 |
| 18 | peak heap | 真实Android pending；host -Xmx128m是cap而不是已测peak。 |
| 19 | storage before/after | 真实手机pending；原始ZIP初始空间估算约661MiB、当前重打包副本约662MiB，仍需更多余量和分阶段检查。 |
| 20 | first real asset import | 真实Nova Android导入pending；host复制/CRC/SHA/type通过。 |
| 21 | duplicate assets | synthetic身份upsert/物理复用/refs不倍增通过；真实手机pending。 |
| 22 | restart | synthetic force-stop后5类附件及原100会话通过；真实手机pending。 |
| 23 | original ZIP removed | synthetic删除input后重新打开/进程重启通过；真实手机pending，只移动/删除测试副本，不删唯一备份。 |
| 24 | airplane mode | Reader JS/storage/file/content/network关闭，精确app-local随机路径白名单；真实飞行模式pending。 |
| 25 | image manual validation | 真实PNG/JPEG人工pending；host230图片已完整解码（PNG82/JPEG148、decode failure0）并通过outer CRC；不等于手机渲染或人工识别内容。 |
| 26 | PDF manual validation | 真实人工打开pending；synthetic signature/存储/MIME/URI验证不等于外部PDF app有效文档验证。 |
| 27 | DOCX manual validation | 真实人工打开pending；synthetic OOXML结构fixture不是有效Word文档阅读验证。 |
| 28 | XLSX manual validation | 真实人工打开pending；synthetic OOXML结构fixture不是有效Excel文档阅读验证。 |
| 29 | attachment-only validation | synthetic显示/存储测试通过，真实≥2消息人工pending。 |
| 30 | HTML consistency | API35/36实际文件与12×8PNG解码通过；portable PNG/JPEG单图2MiB/合计6MiB嵌入，超限描述；文件名/类型/大小描述不含private链接。 |
| 31 | Markdown consistency | API35/36实际文件首尾/Unicode/code/table/对象text/recap/描述通过，thoughts与另一分支排除；保留同scope/顺序/正文及描述，不携binary。 |
| 32 | PDF consistency | API35/36实际16页会话PDF、12×8图片实际RGB像素和DOCX描述通过；static model HTML→System Print包含图片/cards，不拼入附件PDF/Office页。 |
| 33 | FileProvider security | 独立exportedfalse仅private assets根，READ临时grant/ClipData无WRITE；URI不含原name/query；provider query给displayName/size，detected MIME。API26/35/36实际Reader Intent测试通过，监视器阻止外部app启动。 |
| 34 | cleanup/delete | host未commit rollback0残留；API26/35/36 native cancel/delete/migration/orphan cleanup通过；真实手机pending。 |
| 35 | known limitations | 见下方；未保证任意官方格式/服务器历史/真实附件阅读成功。 |
| 36 | AR-Level 1–5 | AR1 synthetic / fixture validation passed；AR2真实compatible repack容器/mapping/326copy-rollback passed；AR3真实Android导入pending；AR4真实重复导入pending；AR5≥5会话各类型/离线重启/来源失效/导出人工pending。 |
| 37 | next step | 安装最终同签名APK，按archive-local-validation.md验证真实AR3–5，只反馈安全统计/固定码/pass-fail；先完成此项，不加入FTS/Cloud/DOM扫描。 |

SQLite schema2非破坏新增`assets`与`message_assets`，官方身份唯一，first/latest来源、MIME/bytes/hash/state/随机private路径、节点ordinal/kind/raw引用；原conversation/message/import_sources、树/分支/current_node/unknown raw metadata保留。binary不放BLOB。覆盖安装保留已有档案；升级本身不访问旧ZIP，用户需手动重新导入以补回附件。

导入：有界container/map/streaming refs planning→exact basename唯一解析→空间预算→private UUID stream copy/CRC/SHA/magic/native image bounds→DB transaction→commit后清理。取消/失败回滚；进程死亡重启按DB清理orphan/pending；旧已校验binary不被坏输入覆盖。极少数事务结束异常可能结果不确定，保留文件由实际DB恢复，A06要求重新核对而不承诺必回滚。

Reader/HTML/Markdown/Print共用ordered display，parts与metadata同消息重复图片去重，附件-only合法；thoughts/raw保留但隐藏、recap明确标签。PDF/DOCX/XLSX以临时content URI交用户选择的系统app，原文件名经provider query提供，不进入URI；第三方实际阅读仍需用户验证。

修改文件按职责：ArchiveZip/AssetFiles/AssetMap/Display/Asset/AssetStore/Store/Renderer/Error；ArchiveReaderActivity/ArchiveAssetProvider/SnapshotWebView、Manifest/窄provider paths；JVM与native fixtures/tests/CI/host scripts；nova-archive/attachment-restoration/local-validation/handoff文档。旧scanner、诊断、29/30与冻结/虚拟化失败证据保留，没有selector扩张。

限制：container512MiB，所选JSON单64MiB/总256MiB，拒绝ZIP64/加密/原样多卷/SFX；输入分卷须先合并。asset单64MiB/新增256MiB、2048/import和256/conversation，图片最大边16384/16M pixels；nested OOXML2048entries/central2MiB/展开64MiB，ratio200；导入全流程总deadline300s，循环间协作检查，阻塞SAF I/O及monitor等候无法保证瞬时中断。未知format只保守描述；HTML总体16Mi chars/图片portable预算如上，MD2Mi chars且不包含binary，LaTeX保留源。标题搜索、列表每页200且可翻页，不含FTS。应用卸载/数据清除会删除私有Archive，请保留官方原始备份。

真实输入、正文、姓名、title、filename、ID、私有hash、Drive链接、截图与真实导出文件没有提交GitHub/CI；合成fixtures完全虚构。AR2检查的是手机重打包compatible export，不称原始官方ZIP逐字节一致，不把host复制当Android导入或人工验收。

锁与历史失败：3c同源API36两次分别在malformed JSON和事务取消测试停滞，失败证据保留；ad30仅test-only线程诊断后全套通过且无超时stack，不能证明根因。787f18f独立统一getReadable/getWritable/close先Archive LOCK再SQLiteOpenHelper monitor，保留文件recovery锁，并增加public ContextWrapper协调的两项原生并发回归。[Android16公开AOSP实现](https://android.googlesource.com/platform/frameworks/base/+/android-16.0.0_r1/core/java/android/database/sqlite/SQLiteOpenHelper.java)还存在可启用的同数据库名共享opening lock，此顺序也覆盖不同helper；没有读取Android隐藏字段或修改其flag，未证明旧失败设备runtime flag/线程栈。历史stall原因仍未最终捕获，新全套通过且35/36无超时stack，新绿色不能改写旧失败。

可复核证据：tools/archive/evidence中的attachment-current-content-counts、attachment-real-bounded-copy、attachment-real-image-codec-audit、attachment-787f-package-verification与本次runtime/foundation/legacy文件；前轮ea26/ad30绿色以及3c失败各自绑定源码，不混用。

最终同签名正式[APK Artifact](https://github.com/DevenirTwilight/ChatGPT-Nova/actions/runs/37677076099/artifacts/11507044105)：解压后覆盖安装ChatGPT-Nova.apk，buildRevision787f18f5296e7bbf4148146c21c9c7656e8c7859；2106973bytes，SHA-256 `726624300448bc653163ed095eab1f5364e51f60bcfcf032efdbdf23c1282a38`。原包名/证书/code14/name保持，未公开Release。然后按[本地验收清单](archive-local-validation.md)手动重新导入，保留唯一备份；只回传计数/固定错误码/pass-fail，不发真实内容或附件名称。

目前不建议把Legacy拆成额外sourceSet：assets/代码成本较小，默认不初始化或注入，BuildConfig显式开关与独立CI已隔离；后续若引入明显更大的依赖或维护成本再评估。全文搜索与citation restoration本轮未实现，先完成真实AR3–5再单独评估。
