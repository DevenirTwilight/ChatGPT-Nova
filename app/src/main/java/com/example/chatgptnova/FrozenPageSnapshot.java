package com.example.chatgptnova;

import org.json.JSONObject;

/** One immutable DOM transaction. Raw source URLs and body never enter diagnostics. */
final class FrozenPageSnapshot {
    final String snapshotId, capturedAt, sourceUrl, title, baseUrl, frozenHtml, markdown;
    final int htmlCharacters, markdownCharacters;
    private final String metadata;
    FrozenPageSnapshot(JSONObject value) throws Exception {
        snapshotId=value.getString("snapshotId"); capturedAt=value.getString("capturedAt");
        sourceUrl=value.getString("sourceUrl");title=value.getString("title");baseUrl=value.getString("baseUrl");
        frozenHtml=value.getString("frozenHtml");markdown=value.getString("markdown");
        htmlCharacters=frozenHtml.length();markdownCharacters=markdown.length();
        metadata=value.getJSONObject("metadata").toString();
        if(!PageSnapshotExport.trustedUrl(sourceUrl)||!sourceUrl.equals(baseUrl)||frozenHtml.isEmpty()||markdown.isEmpty())
            throw new IllegalArgumentException("Invalid snapshot");
    }
    JSONObject diagnosticMetadata() throws Exception {return new JSONObject(metadata);}
}
