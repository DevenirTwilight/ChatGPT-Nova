# ChatGPT Nova 图标

两层错位的聊天气泡代表独立的第二登录空间；前景的镂空四芒星代表 Nova。使用原创几何轮廓，不采用 OpenAI 的结形标识或临时的 Z/S 图形。

| 用途 | 色值 |
| --- | --- |
| 深蓝背景 | `#13233E` |
| 薄荷绿前景 | `#6AE7C8` |
| 浅蓝后层 | `#8FAAFB` |
| 原生界面强调色 | `#13836F` |

`nova-icon.svg` 是完整 108 × 108 图层源图；`nova-monochrome.svg` 是透明的单色前景；`nova-launcher.svg` 是 512px 方形导出。预览中的圆形、圆角矩形和主题配色是展示示例，真实桌面形状和主题颜色由 Android Launcher 决定。

Android 使用矢量资源，彩色前景、背景和单色前景分离。108dp 图层的标记位于中心安全区内；API 26+ 为 adaptive icon，API 33+ 提供 monochrome，普通 drawable 请求也有后备图标。启动页与关于页使用同一标识。启动页使用 AndroidX SplashScreen，页面准备好即结束，不额外等待。

用 Inkscape 从源文件重现预览：

```sh
inkscape docs/branding/ChatGPT-Nova-Icon-Preview.svg --export-type=png --export-filename=docs/branding/ChatGPT-Nova-Icon-Preview.png
inkscape docs/branding/nova-launcher.svg --export-type=png --export-filename=docs/branding/nova-launcher-512.png
```
