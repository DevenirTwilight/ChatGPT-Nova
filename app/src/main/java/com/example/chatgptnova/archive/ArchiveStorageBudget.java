package com.example.chatgptnova.archive;

/** Storage planning only, not a promise of enough space for every accepted input. */
public final class ArchiveStorageBudget {
  // ~55MiB selected JSON can require both DB pages and transaction WAL, plus indexes.
  public static final long DB_WAL_ALLOWANCE = 128L * 1024 * 1024;
  public static final long FIXED_RESERVE = 32L * 1024 * 1024;

  private ArchiveStorageBudget() {}

  public static void checkBeforeCopy(long available, long declared) throws ArchiveError {
    ArchiveImporter.checkContainerSize(declared);
    long needed = Math.max(0, declared) + DB_WAL_ALLOWANCE + FIXED_RESERVE;
    if (available < needed) throw new ArchiveError("A09_STORAGE_FAILED");
  }

  public static void checkCopySpace(long available) throws ArchiveError {
    checkBeforeCopy(available, 0);
  }

  public static void checkReserve(long available) throws ArchiveError {
    if (available < FIXED_RESERVE) throw new ArchiveError("A09_STORAGE_FAILED");
  }
}
