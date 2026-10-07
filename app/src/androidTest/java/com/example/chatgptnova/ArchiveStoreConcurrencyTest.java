package com.example.chatgptnova;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.chatgptnova.archive.ArchiveStore;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

/** Public Android API hooks; fictional empty DB only, no hidden fields or user data. */
public final class ArchiveStoreConcurrencyTest {
  @Test
  public void openingHelperCannotReverseArchiveLockOrder() throws Exception {
    Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ArchiveStore.deleteArchive(base);
    CountDownLatch readerStarted = new CountDownLatch(1);
    CountDownLatch readerInHelper = new CountDownLatch(1);
    CountDownLatch releasePath = new CountDownLatch(1);
    CountDownLatch ownerOpening = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();
    AtomicReference<Thread> readerRef = new AtomicReference<>();
    AtomicReference<SQLiteDatabase> ownerDb = new AtomicReference<>();
    AtomicReference<SQLiteDatabase> readerDb = new AtomicReference<>();
    Context context =
        new ContextWrapper(base) {
          @Override
          public Context getApplicationContext() {
            return this;
          }

          @Override
          public File getDatabasePath(String name) {
            pauseReaderInHelper();
            return super.getDatabasePath(name);
          }

          @Override
          public SQLiteDatabase openOrCreateDatabase(
              String name, int mode, SQLiteDatabase.CursorFactory factory) {
            pauseReaderInHelper();
            return super.openOrCreateDatabase(name, mode, factory);
          }

          @Override
          public SQLiteDatabase openOrCreateDatabase(
              String name,
              int mode,
              SQLiteDatabase.CursorFactory factory,
              android.database.DatabaseErrorHandler handler) {
            pauseReaderInHelper();
            return super.openOrCreateDatabase(name, mode, factory, handler);
          }

          private void pauseReaderInHelper() {
            // SQLiteOpenHelper calls this while holding its own monitor. Pause
            // only the reader to expose an inversion without accessing internals.
            if (Thread.currentThread() == readerRef.get()) {
              readerInHelper.countDown();
              try {
                if (!releasePath.await(5, TimeUnit.SECONDS))
                  throw new AssertionError("SYNTHETIC_PATH_WAIT_TIMEOUT");
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("SYNTHETIC_PATH_INTERRUPTED");
              }
            }
          }
        };
    ArchiveStore store = new ArchiveStore(context);
    Thread reader =
        new Thread(
            () -> {
              readerStarted.countDown();
              try {
                readerDb.set(store.getReadableDatabase());
              } catch (Throwable e) {
                error.compareAndSet(null, e);
              }
            });
    reader.setDaemon(true);
    readerRef.set(reader);
    Thread owner =
        new Thread(
            () -> {
              try {
                synchronized (ArchiveStore.LOCK) {
                  reader.start();
                  if (!readerStarted.await(2, TimeUnit.SECONDS))
                    throw new AssertionError("SYNTHETIC_READER_START_TIMEOUT");
                  // Before the fix, the reader reaches the helper, holding its
                  // monitor; after the fix it blocks before entering the helper.
                  readerInHelper.await(250, TimeUnit.MILLISECONDS);
                  ownerOpening.countDown();
                  ownerDb.set(store.getWritableDatabase());
                }
              } catch (Throwable e) {
                error.compareAndSet(null, e);
              } finally {
                ownerOpening.countDown();
              }
            });
    owner.setDaemon(true);
    owner.start();
    assertTrue(ownerOpening.await(3, TimeUnit.SECONDS));
    releasePath.countDown();
    owner.join(5000);
    reader.join(5000);
    if (owner.isAlive() || reader.isAlive()) {
      ArchiveThreadEvidence.capture("synthetic-store-lock-order");
      // Do not block the failure on closing a helper whose monitors are stuck.
      fail("ARCHIVE_STORE_LOCK_ORDER_STALLED");
    }
    try {
      assertNull(error.get());
      assertNotNull(ownerDb.get());
      assertSame(ownerDb.get(), readerDb.get());
      assertEquals(2, ownerDb.get().getVersion());
      assertEquals("Reader must wait before entering the helper", 1, readerInHelper.getCount());
    } finally {
      store.close();
      ArchiveStore.deleteArchive(base);
    }
  }

  @Test
  public void closeWaitsForArchiveConsistencyLock() throws Exception {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ArchiveStore.deleteArchive(context);
    ArchiveStore store = new ArchiveStore(context);
    SQLiteDatabase db = store.getWritableDatabase();
    CountDownLatch attempted = new CountDownLatch(1), closed = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();
    Thread closer =
        new Thread(
            () -> {
              attempted.countDown();
              try {
                store.close();
              } catch (Throwable e) {
                error.set(e);
              } finally {
                closed.countDown();
              }
            });
    closer.setDaemon(true);
    try {
      synchronized (ArchiveStore.LOCK) {
        closer.start();
        assertTrue(attempted.await(2, TimeUnit.SECONDS));
        assertFalse("Close must wait for the active import/recovery", closed.await(250, TimeUnit.MILLISECONDS));
        assertTrue(db.isOpen());
      }
      assertTrue(closed.await(5, TimeUnit.SECONDS));
      assertNull(error.get());
      assertFalse(db.isOpen());
    } finally {
      closer.join(5000);
      if (!closer.isAlive()) {
        store.close();
        ArchiveStore.deleteArchive(context);
      }
    }
  }
}
