const assert = require('node:assert/strict');
const {audit} = require('./audit.cjs');
const rows = Array.from({length: 8}, (_, i) => ({id: `fixture-${i}`, role: i % 2 ? 'assistant' : 'user', nested: 0}));
const reference = {route: '/g/g-p-fixture/c/fixture', source: 'synthetic independent fixture', scope: 'selected branch', rows};
const snapshot = selected => ({schema: 1, route: reference.route, routeAfter: reference.route,
  rows: selected, count: selected.length, bounded: false, ready: 'complete', streamingHint: false,
  capturedAt: '2026-10-06T00:00:00Z'});
const complete = snapshot(rows);
assert.equal(audit(reference, [complete]).status, 'reference-id-coverage-match');
assert.equal(audit(reference, [complete]).historyCompleteness, 'not-proven');
assert.equal(audit(reference, [snapshot(rows.slice(4)), snapshot(rows.slice(4))]).missingAcrossEligibleSamples.length, 4);
assert.equal(audit(reference, [snapshot(rows.slice(4)), snapshot(rows.slice(0, 4))]).status, 'union-only-match');
assert.equal(audit(reference, [snapshot([...rows].reverse())]).singleSnapshotMatches, false);
for (const change of [
  {routeAfter: '/g/g-p-fixture/c/other'}, {bounded: true}, {count: 9},
  {streamingHint: true}, {ready: 'loading'}, {capturedAt: ''},
  {rows: rows.map((r, i) => i === 3 ? {...r, role: 'user'} : r)},
  {rows: rows.map((r, i) => i === 3 ? {...r, id: 'unknown'} : r)},
  {rows: rows.map((r, i) => i === 3 ? {...r, id: rows[0].id} : r)},
  {rows: rows.map((r, i) => i === 3 ? {...r, nested: 1} : r)},
  {rows: rows.map((r, i) => i === 3 ? {...r, id: ''} : r)}
]) assert.equal(audit(reference, [{...complete, ...change}]).singleSnapshotMatches, false);
assert.throws(() => audit({...reference, source: ''}, [complete]));
assert.throws(() => audit(reference, []));
console.log('PASS: coverage audit synthetic checks; no live-account or body proof');
