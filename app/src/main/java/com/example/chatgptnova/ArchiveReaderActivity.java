package com.example.chatgptnova;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.print.*;
import android.widget.*;
import com.example.chatgptnova.archive.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Independent offline reader/export, never obtains the online WebView or a snapshot. */
public final class ArchiveReaderActivity extends Activity {
  static final int SAVE = 7102;
  private android.widget.FrameLayout host;
  private TextView status;
  private Button export, branch;
  private SnapshotWebView reader, printer;
  private ArchiveModel.Conversation conversation;
  private ArchiveTree.Selection selection;
  private String html, pendingText, pendingName;
  private volatile java.util.Map<String, ArchiveAsset> assets = java.util.Collections.emptyMap();
  private boolean all;
  private volatile boolean destroyed;
  private volatile int generation;
  private long row;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    row = getIntent().getLongExtra("row", 0);
    if (state != null) all = state.getBoolean("all");
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    setContentView(root);
    ArchiveUi.inset(root, 0);
    LinearLayout actions = new LinearLayout(this);
    root.addView(actions);
    branch = new Button(this);
    branch.setText("查看全部分支");
    actions.addView(branch, new LinearLayout.LayoutParams(0, -2, 1));
    branch.setOnClickListener(
        v -> {
          all = !all;
          load();
        });
    export = new Button(this);
    export.setText("导出");
    export.setEnabled(false);
    actions.addView(export);
    export.setOnClickListener(v -> formats());
    status = new TextView(this);
    status.setText("正在读取本地会话…");
    root.addView(status);
    host = new FrameLayout(this);
    root.addView(host, new LinearLayout.LayoutParams(-1, 0, 1));
    load();
  }

  private void load() {
    final int current = ++generation;
    export.setEnabled(false);
    status.setText("正在读取本地会话…");
    branch.setText(all ? "查看主链" : "查看全部分支");
    boolean requestedAll = all;
    new Thread(
            () -> {
              try {
                ArchiveModel.Conversation c;
                java.util.Map<String, ArchiveAsset> capturedAssets;
                try (ArchiveStore store = new ArchiveStore(getApplicationContext())) {
                  c = store.load(row);
                  capturedAssets = store.assets(row);
                }
                ArchiveTree.Selection s = ArchiveTree.select(c, requestedAll);
                String rendered =
                    ArchiveRenderer.html(c, s, capturedAssets, ArchiveRenderer.AssetMode.READER);
                runOnUiThread(
                    () -> {
                      if (destroyed || generation != current) return;
                      conversation = c;
                      assets = capturedAssets;
                      selection = s;
                      html = rendered;
                      showReader(current);
                    });
              } catch (ArchiveError | RuntimeException e) {
                runOnUiThread(
                    () -> {
                      if (!destroyed && current == generation)
                        status.setText(
                            e instanceof ArchiveError
                                ? ((ArchiveError) e).code + "：" + ((ArchiveError) e).explanation()
                                : "A06_DATABASE_WRITE_FAILED");
                    });
              }
            },
            "NovaArchiveReader")
        .start();
  }

  private void showReader(int current) {
    if (reader != null) reader.close();
    try {
      reader =
          new SnapshotWebView(
              this,
              host,
              html,
              conversation.title,
              true,
              new SnapshotWebView.Callback() {
                public void ready() {
                  if (destroyed || current != generation) return;
                  status.setText(
                      displayCount()
                          + " 条可见记录 · "
                          + selection.scope
                          + (selection.warnings.isEmpty() ? "" : " · 有顺序警告")
                          + " · 完整性取决于导入文件");
                  export.setEnabled(true);
                }

                public void failed(String code) {
                  if (!destroyed) status.setText("A10_READER_FAILED：本地阅读器无法加载。");
                }

                public android.webkit.WebResourceResponse localResource(String url) {
                  return localImage(url);
                }

                public void openLink(String url) {
                  if (url.startsWith(ArchiveRenderer.LOCAL_ORIGIN + "/attachments/")) {
                    openAsset(url);
                    return;
                  }
                  Uri u = Uri.parse(url);
                  if (!"https".equalsIgnoreCase(u.getScheme())
                      && !"http".equalsIgnoreCase(u.getScheme())) return;
                  try {
                    startActivity(
                        new Intent(Intent.ACTION_VIEW, u).addCategory(Intent.CATEGORY_BROWSABLE));
                  } catch (ActivityNotFoundException e) {
                    Toast.makeText(ArchiveReaderActivity.this, "未找到浏览器", Toast.LENGTH_SHORT).show();
                  }
                }
              });
    } catch (RuntimeException e) {
      status.setText("A10_READER_FAILED");
    }
  }

  private int displayCount() {
    int count = 0;
    for (ArchiveModel.Node node : selection.messages) if (node.displayable()) count++;
    return count;
  }

  private void formats() {
    if (conversation == null || selection == null) return;
    new AlertDialog.Builder(this)
        .setTitle("导出本地会话 · " + selection.scope)
        .setItems(
            new String[] {"HTML", "Markdown", "打印 / PDF"},
            (d, which) -> {
              if (which == 2) print();
              else save(which == 0);
            })
        .setNegativeButton("取消", null)
        .show();
  }

  private void save(boolean asHtml) {
    int current = generation;
    export.setEnabled(false);
    branch.setEnabled(false);
    status.setText("正在准备导出…");
    ArchiveModel.Conversation c = conversation;
    ArchiveTree.Selection selected = selection;
    java.util.Map<String, ArchiveAsset> captured = assets;
    new Thread(
            () -> {
              try {
                String text =
                    asHtml
                        ? ArchiveRenderer.html(
                            c, selected, captured, ArchiveRenderer.AssetMode.PORTABLE)
                        : ArchiveRenderer.markdown(c, selected, captured);
                runOnUiThread(
                    () -> {
                      if (destroyed || current != generation) return;
                      export.setEnabled(true);
                      branch.setEnabled(true);
                      pendingText = text;
                      pendingName =
                          "Nova-archive-"
                              + PageSnapshotExport.documentName(c.title)
                              + (asHtml ? ".html" : ".md");
                      Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                      i.addCategory(Intent.CATEGORY_OPENABLE);
                      i.setType(asHtml ? "text/html" : "text/markdown");
                      i.putExtra(Intent.EXTRA_TITLE, pendingName);
                      startActivityForResult(i, SAVE);
                    });
              } catch (ArchiveError | RuntimeException e) {
                runOnUiThread(
                    () -> {
                      if (!destroyed && current == generation) {
                        export.setEnabled(true);
                        branch.setEnabled(true);
                        status.setText("A11_EXPORT_FAILED：无法准备本地会话导出。");
                      }
                    });
              }
            },
            "NovaArchivePortableExport")
        .start();
  }

  private ArchiveAsset urlAsset(String url, String kind) {
    if (destroyed) return null;
    String prefix = ArchiveRenderer.LOCAL_ORIGIN + "/" + kind + "/";
    if (!url.startsWith(prefix)) return null;
    String filename = url.substring(prefix.length());
    if (!filename.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.bin")) return null;
    for (ArchiveAsset asset : assets.values())
      if (asset.available() && asset.file.getName().equals(filename)) return asset;
    return null;
  }

  private android.webkit.WebResourceResponse localImage(String url) {
    ArchiveAsset asset = urlAsset(url, "images");
    if (asset == null || !asset.image()) return null;
    try {
      return new android.webkit.WebResourceResponse(
          asset.mime, null, new FileInputStream(asset.file));
    } catch (IOException e) {
      return null;
    }
  }

  private void openAsset(String url) {
    ArchiveAsset asset = urlAsset(url, "attachments");
    if (asset == null) {
      Toast.makeText(this, "附件未包含在导出文件中或无法读取", Toast.LENGTH_SHORT).show();
      return;
    }
    try {
      Uri uri =
          androidx.core.content.FileProvider.getUriForFile(
              this, getPackageName() + ".archiveassets", asset.file, asset.name);
      Intent intent =
          new Intent(Intent.ACTION_VIEW)
              .setDataAndType(uri, asset.mime)
              .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      intent.setClipData(ClipData.newRawUri("Nova Archive attachment", uri));
      startActivity(intent);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, "未找到可打开此类附件的应用", Toast.LENGTH_LONG).show();
    } catch (RuntimeException e) {
      Toast.makeText(this, "无法打开本地附件", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onActivityResult(int req, int result, Intent data) {
    super.onActivityResult(req, result, data);
    if (req != SAVE) return;
    String text = pendingText;
    pendingText = null;
    pendingName = null;
    if (result != RESULT_OK || data == null || data.getData() == null) {
      status.setText("导出已取消");
      return;
    }
    if (text == null) {
      status.setText("A11_EXPORT_FAILED：页面已重建，请重新导出。");
      return;
    }
    Uri uri = data.getData();
    int current = generation;
    new Thread(
            () -> {
              boolean ok = false;
              try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                if (out == null) throw new IOException();
                try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                  for (int p = 0; p < text.length(); p += 32768) {
                    if (destroyed || generation != current) return;
                    writer.write(text, p, Math.min(32768, text.length() - p));
                  }
                  writer.flush();
                }
                ok = true;
              } catch (Exception ignored) {
              }
              boolean saved = ok;
              runOnUiThread(
                  () -> {
                    if (!destroyed && generation == current)
                      status.setText(
                          saved ? "已保存 Archive 会话文件，请核对内容。" : "A11_EXPORT_FAILED：无法写入所选文件。");
                  });
            },
            "NovaArchiveExport")
        .start();
  }

  private void print() {
    if (printer != null) return;
    export.setEnabled(false);
    branch.setEnabled(false);
    status.setText("正在准备本地 PDF…");
    try {
      printer =
          new SnapshotWebView(
              this,
              host,
              ArchiveRenderer.html(
                  conversation, selection, assets, ArchiveRenderer.AssetMode.PRINT),
              "Nova-archive-" + conversation.title,
              false,
              new SnapshotWebView.Callback() {
                public android.webkit.WebResourceResponse localResource(String url) {
                  return localImage(url);
                }

                public void ready() {
                  if (destroyed || printer == null) return;
                  try {
                    PrintManager manager = (PrintManager) getSystemService(PRINT_SERVICE);
                    manager.print(
                        "Nova-archive-" + PageSnapshotExport.documentName(conversation.title),
                        printer.printAdapter(
                            () -> {
                              printer = null;
                              if (!destroyed) {
                                export.setEnabled(true);
                                branch.setEnabled(true);
                                status.setText("打印窗口已关闭；保存结果请在目标文件中核对。");
                              }
                            }),
                        new PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                            .build());
                  } catch (RuntimeException e) {
                    printFailed();
                  }
                }

                public void failed(String code) {
                  printFailed();
                }
              });
    } catch (ArchiveError | RuntimeException e) {
      printFailed();
    }
  }

  private void printFailed() {
    if (printer != null) {
      printer.close();
      printer = null;
    }
    if (!destroyed) {
      export.setEnabled(true);
      branch.setEnabled(true);
      status.setText("A12_PRINT_FAILED：无法启动 System Print。");
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    state.putBoolean("all", all);
  }

  @Override
  protected void onDestroy() {
    destroyed = true;
    generation++;
    pendingText = null;
    if (reader != null) reader.close();
    if (printer != null) printer.close();
    super.onDestroy();
  }
}
