# Gecko PDF 导出引擎

导出模块使用 Mozilla 官方 Maven 的 `org.mozilla.geckoview:geckoview:140.0.20250707120347`，固定版本兼容当前 compileSdk35 工程；Gecko 仅渲染本地净化的导出HTML，聊天登录仍在原WebView内。依赖自带多架构本地引擎，安装包会明显增大。版本号与应用版本分别记录在导出诊断中。

PDF调用公开 `GeckoSession.saveAsPdf()`，与Firefox Android `GeckoEngineSession.requestPdfToDownload()`使用同一底层接口；没有复制Firefox浏览器整套下载或会话实现，也没有宣称获取完整会话。采用私密会话，关闭JavaScript和调试输出，导出HTML的CSP禁止外部资源；生成文件后由系统文件选择器保存，可取消、重新生成。

- [Firefox Android调用代码](https://github.com/mozilla/gecko-dev/blob/master/mobile/android/android-components/components/browser/engine-gecko/src/main/java/mozilla/components/browser/engine/gecko/GeckoEngineSession.kt)
- [本批官方依赖与源码包](https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview/140.0.20250707120347/)
- [对应Mozilla源码修订](https://hg.mozilla.org/releases/mozilla-release/rev/65c832029a47e8865556ad3f4dbfe690863f637c)
- [MPL 2.0许可文本](licenses/MPL-2.0.txt)

Mozilla引擎源代码及其内部第三方许可随官方源码提供；本仓库未修改该引擎。依赖的许可证也在官方POM/源码中提供。

Gecko捆绑的ExoPlayer NotificationUtil没有用于本地PDF。app/lint.xml仅按该第三方类的具体诊断文字忽略NotificationPermission，应用自身和其他依赖的检查继续启用，没有加入通知权限。新会话仅在data HTML开始并完成加载后生成PDF，避免把初始化空白页当导出文档。
