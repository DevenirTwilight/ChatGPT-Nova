package com.example.chatgptnova;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Native Archive controls remain outside system bars under target35 edge-to-edge. */
final class ArchiveUi {
  private ArchiveUi() {}

  static void inset(View root, int marginDp) {
    int p = Math.round(marginDp * root.getResources().getDisplayMetrics().density);
    root.setPadding(p, p, p, p);
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (v, insets) -> {
          Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(p + bars.left, p + bars.top, p + bars.right, p + bars.bottom);
          return insets;
        });
    ViewCompat.requestApplyInsets(root);
  }
}
