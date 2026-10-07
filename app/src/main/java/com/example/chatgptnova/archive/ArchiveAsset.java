package com.example.chatgptnova.archive;

import java.io.File;

/** Private runtime descriptor. Renderers never serialize identity, hash or the filesystem path. */
public final class ArchiveAsset {
  public final String name, mime, state;
  public final long bytes;
  public final int width, height;
  public final File file;

  public ArchiveAsset(
      String name, String mime, String state, long bytes, int width, int height, File file) {
    this.name = ArchiveAssetFiles.displayName(name);
    this.mime = mime;
    this.state = state;
    this.bytes = bytes;
    this.width = width;
    this.height = height;
    this.file = file;
  }

  public boolean available() {
    return state.equals("complete") && file != null && file.isFile() && file.length() == bytes;
  }

  public boolean image() {
    return mime.equals("image/png") || mime.equals("image/jpeg");
  }
}
