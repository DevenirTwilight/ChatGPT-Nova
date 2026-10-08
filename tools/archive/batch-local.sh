#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
archive_deps="${NOVA_ARCHIVE_JARS:-/tmp/nova-archive-jars}"
archive_classes=$(mktemp -d)
trap 'rm -rf "$archive_classes"' EXIT
if [ "$#" -ne 3 ]; then
  echo 'Usage: batch-local.sh private-input.zip private-output.zip private-staging-directory' >&2
  exit 2
fi
java -jar "$archive_deps/ecj.jar" -17 \
  -cp "$archive_deps/gson.jar:$archive_deps/commonmark.jar:$archive_deps/tables.jar" \
  -d "$archive_classes" \
  app/src/main/java/com/example/chatgptnova/archive/Archive{Batch,Timeline,Research,Error,Model,Tree,Importer,Renderer,Zip,StorageBudget,AssetFiles,AssetMap,Display,Asset}.java \
  tools/archive/ArchiveBatchLocal.java
java -Xmx128m -cp "$archive_classes:$archive_deps/*" \
  com.example.chatgptnova.archive.ArchiveBatchLocal "$@"
