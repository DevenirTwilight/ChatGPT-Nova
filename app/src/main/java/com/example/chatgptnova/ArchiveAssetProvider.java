package com.example.chatgptnova;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.core.content.FileProvider;
import com.example.chatgptnova.archive.*;

/** Narrow provider: binary MIME/display name come from private metadata, never URI parameters. */
public final class ArchiveAssetProvider extends FileProvider {
  private String[] metadata(Uri uri) {
    super.getType(uri); // Validate the configured path scope first.
    String filename = uri.getLastPathSegment();
    if (filename == null || !filename.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.bin"))
      return null;
    try (ArchiveStore store = new ArchiveStore(getContext());
        Cursor q =
            store
                .getReadableDatabase()
                .rawQuery(
                    "SELECT detected_mime,display_name FROM assets WHERE relative_path=? AND"
                        + " state='complete'",
                    new String[] {filename})) {
      return q.moveToFirst() ? new String[] {q.getString(0), q.getString(1)} : null;
    } catch (RuntimeException e) {
      return null;
    }
  }

  @Override
  public String getType(Uri uri) {
    String[] values = metadata(uri);
    return values == null ? ArchiveAssetFiles.UNKNOWN : values[0];
  }

  @Override
  public Cursor query(
      Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    // Keep FileProvider's scope checks and supported projections/size behavior.
    try (Cursor base = super.query(uri, projection, selection, selectionArgs, sortOrder)) {
      if (base == null) return null;
      MatrixCursor result = new MatrixCursor(base.getColumnNames(), 1);
      if (!base.moveToFirst()) return result;
      String[] values = metadata(uri);
      Object[] row = new Object[base.getColumnCount()];
      for (int i = 0; i < row.length; i++) {
        String column = base.getColumnName(i);
        if (OpenableColumns.DISPLAY_NAME.equals(column) && values != null) row[i] = values[1];
        else if (OpenableColumns.SIZE.equals(column)) row[i] = base.getLong(i);
        else row[i] = base.getString(i);
      }
      result.addRow(row);
      return result;
    }
  }
}
