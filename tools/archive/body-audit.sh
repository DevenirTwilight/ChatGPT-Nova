#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
archive_deps="${NOVA_ARCHIVE_JARS:-/tmp/nova-archive-jars}"
archive_classes=$(mktemp -d)
trap 'rm -rf "$archive_classes"' EXIT
if [ "$#" -ne 1 ]; then
  echo 'Usage: body-audit.sh /absolute/path/to/private-export.zip' >&2
  exit 2
fi
java -jar "$archive_deps/ecj.jar" -17 \
  -cp "$archive_deps/gson.jar:$archive_deps/commonmark.jar:$archive_deps/tables.jar" \
  -d "$archive_classes" \
  app/src/main/java/com/example/chatgptnova/archive/Archive{Research,Error,Model,Tree,Importer,Renderer,Zip,StorageBudget,AssetFiles,AssetMap,Display,Asset}.java \
  tools/archive/ArchiveBodyAudit.java
java -Xmx128m -cp "$archive_classes:$archive_deps/*" \
  com.example.chatgptnova.archive.ArchiveBodyAudit "$1"
