const fs = require('node:fs');

function audit(reference, snapshots) {
  const validRow = r => r && typeof r.id === 'string' && r.id.length > 0
    && ['user', 'assistant'].includes(r.role);
  if (!reference || !/^\/g\/g-p-[^/]+\/c\/[^/]+\/?$/.test(reference.route)
      || !Array.isArray(reference.rows) || !reference.rows.length
      || !reference.rows.every(validRow)
      || new Set(reference.rows.map(r => r.id)).size !== reference.rows.length
      || typeof reference.source !== 'string' || !reference.source.trim()
      || typeof reference.scope !== 'string' || !reference.scope.trim())
    throw Error('Reference requires project route, independent source, branch scope and unique ordered ID/role rows');
  if (!Array.isArray(snapshots) || !snapshots.length) throw Error('No snapshots');
  const expected = new Map(reference.rows.map(r => [r.id, r.role]));
  const union = new Set();
  const reports = snapshots.map((s, index) => {
    const issues = [];
    if (!s || s.schema !== 1 || !Array.isArray(s.rows) || !s.rows.every(validRow))
      return {index, eligible: false, issues: ['invalid-schema-or-rows']};
    if (s.route !== reference.route || s.routeAfter !== reference.route) issues.push('route-mismatch');
    if (s.bounded !== false || s.count !== s.rows.length) issues.push('bounded-or-count-mismatch');
    if (s.ready !== 'complete' || s.streamingHint !== false) issues.push('page-not-settled');
    if (!s.capturedAt || !Number.isFinite(Date.parse(s.capturedAt))) issues.push('missing-capture-time');
    if (s.rows.some(r => r.nested !== 0)) issues.push('nested-or-unknown-author-structure');
    const ids = s.rows.map(r => r.id);
    if (new Set(ids).size !== ids.length) issues.push('duplicate-ids');
    if (s.rows.some(r => !expected.has(r.id) || expected.get(r.id) !== r.role))
      issues.push('unexpected-id-or-role');
    const missing = reference.rows.filter(r => !ids.includes(r.id)).map(r => r.id);
    const ordered = s.rows.length === reference.rows.length
      && s.rows.every((r, i) => r.id === reference.rows[i].id && r.role === reference.rows[i].role);
    const eligible = issues.length === 0;
    if (eligible) ids.forEach(id => union.add(id));
    return {index, eligible, issues, missing, orderedReferenceMatch: eligible && ordered};
  });
  const missingAcrossEligibleSamples = reference.rows.filter(r => !union.has(r.id)).map(r => r.id);
  const singleSnapshotMatches = reports.some(r => r.orderedReferenceMatch);
  return {
    status: singleSnapshotMatches ? 'reference-id-coverage-match'
      : missingAcrossEligibleSamples.length ? 'reference-coverage-not-demonstrated' : 'union-only-match',
    historyCompleteness: 'not-proven', bodyFidelity: 'not-tested',
    referenceSource: reference.source, referenceScope: reference.scope,
    singleSnapshotMatches, missingAcrossEligibleSamples, reports
  };
}

module.exports = {audit};
if (require.main === module) {
  try {
    const [referencePath, snapshotsPath] = process.argv.slice(2);
    if (!referencePath || !snapshotsPath) throw Error('Usage: node audit.cjs reference.json snapshots.json');
    const result = audit(JSON.parse(fs.readFileSync(referencePath, 'utf8')),
      JSON.parse(fs.readFileSync(snapshotsPath, 'utf8')));
    process.stdout.write(JSON.stringify(result, null, 2) + '\n');
    if (!result.singleSnapshotMatches) process.exitCode = 2;
  } catch (error) { console.error(error.message); process.exitCode = 1; }
}
