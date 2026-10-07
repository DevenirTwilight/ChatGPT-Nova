package com.example.chatgptnova.archive;

/** Fixed public errors; never attach parser/provider exceptions containing private data. */
public final class ArchiveError extends Exception {
  private static final long serialVersionUID = 1;
  public final String code;

  public ArchiveError(String code) {
    super(code);
    this.code = code;
  }

  public String explanation() {
    switch (code) {
      case "A01_UNSUPPORTED_FILE":
        return "请选择 ChatGPT 数据导出的 ZIP 或 JSON 文件。";
      case "A02_INVALID_ZIP":
        return "ZIP 损坏或包含不安全、重复的文件路径。";
      case "A03_NO_CONVERSATIONS_DATA":
        return "文件中没有找到可识别的 conversations 数据。";
      case "A04_JSON_PARSE_FAILED":
        return "会话 JSON 格式错误，导入已回滚。";
      case "A05_ARCHIVE_TOO_LARGE":
        return "文件超过安全限制，请使用较小的导出文件。";
      case "A06_DATABASE_WRITE_FAILED":
        return "本地数据库写入失败，请重新核对已有档案。";
      case "A07_IMPORT_CANCELLED":
        return "导入已取消，已有档案未改变。";
      case "A08_SCHEMA_UNSUPPORTED":
        return "此会话数据结构暂不支持。";
      case "A09_STORAGE_FAILED":
        return "私有存储空间不足，或无法读取所选文件 / 写入存储。已有档案请重新核对。";
      default:
        return "无法完成本地档案操作。";
    }
  }
}
