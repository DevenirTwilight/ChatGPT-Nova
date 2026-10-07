package com.example.chatgptnova;

import android.database.Cursor;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.example.chatgptnova.archive.*;

/**
 * Narrow private asset provider. MIME comes from verified binary metadata, never the .bin suffix.
 */
public final class ArchiveAssetProvider extends FileProvider {
  @Override
  public String getType(Uri uri) {
    super.getType(uri); // Validate this provider's configured path scope before querying metadata.
    String filename = uri.getLastPathSegment();
    if (filename == null || !filename.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.bin"))
      return ArchiveAssetFiles.UNKNOWN;
    try (ArchiveStore store = new ArchiveStore(getContext());
        Cursor q =
            store
                .getReadableDatabase()
                .rawQuery(
                    "SELECT detected_mime FROM assets WHERE relative_path=? AND state='complete'",
                    new String[] {filename})) {
      return q.moveToFirst() ? q.getString(0) : ArchiveAssetFiles.UNKNOWN;
    } catch (RuntimeException e) {
      return ArchiveAssetFiles.UNKNOWN;
    }
  }
}
