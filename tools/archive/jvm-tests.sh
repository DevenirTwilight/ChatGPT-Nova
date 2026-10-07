#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
archive_deps="${NOVA_ARCHIVE_JARS:-/tmp/nova-archive-jars}"
archive_classes=$(mktemp -d)
trap 'rm -rf "$archive_classes"' EXIT
java -jar "$archive_deps/ecj.jar" -17 -cp "$archive_deps/gson.jar:$archive_deps/commonmark.jar:$archive_deps/tables.jar:$archive_deps/junit.jar:$archive_deps/hamcrest.jar" -d "$archive_classes" \
  app/src/main/java/com/example/chatgptnova/archive/Archive{Error,Model,Tree,Merge,Importer,Renderer,StorageBudget,Zip,AssetFiles,AssetMap,Display}.java \
  app/src/test/java/com/example/chatgptnova/archive/Archive{Core,AssetFiles,AssetMap,Display}Test.java
java -Xmx128m -cp "$archive_classes:$archive_deps/*" org.junit.runner.JUnitCore com.example.chatgptnova.archive.ArchiveCoreTest com.example.chatgptnova.archive.ArchiveAssetFilesTest com.example.chatgptnova.archive.ArchiveAssetMapTest com.example.chatgptnova.archive.ArchiveDisplayTest
