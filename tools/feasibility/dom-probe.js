(() => {
  if (window !== window.top || location.origin !== 'https://chatgpt.com')
    return JSON.stringify({error: 'wrong-origin-or-frame'});

  const all = [...document.querySelectorAll('[data-message-author-role]')]
    .filter(e => ['user', 'assistant'].includes(e.getAttribute('data-message-author-role')));
  const hash = text => {
    let h = 2166136261;
    for (let i = 0; i < text.length; i++)
      h = Math.imul(h ^ text.charCodeAt(i), 16777619);
    return (h >>> 0).toString(16);
  };
  const rows = [];
  let budget = 500000, bounded = all.length > 300;
  for (const e of all.slice(0, 300)) {
    if (budget <= 0) { bounded = true; break; }
    const text = e.textContent || '';
    const limit = Math.min(128000, budget);
    const sampled = text.slice(0, limit);
    if (sampled.length !== text.length) bounded = true;
    budget -= sampled.length;
    rows.push({
      id: e.getAttribute('data-message-id')
        || e.closest('[data-message-id]')?.getAttribute('data-message-id') || '',
      role: e.getAttribute('data-message-author-role'),
      chars: text.length,
      fingerprint: hash(sampled),
      nestedAuthorNodes: e.querySelectorAll('[data-message-author-role]').length,
      codeBlocks: e.querySelectorAll('pre').length,
      tables: e.querySelectorAll('table').length,
      math: e.querySelectorAll('.katex').length,
      texSources: e.querySelectorAll('annotation[encoding="application/x-tex"]').length,
      images: e.querySelectorAll('img').length,
      loadedImages: [...e.querySelectorAll('img')]
        .filter(i => i.complete && i.naturalWidth > 0).length
    });
  }
  return JSON.stringify({
    historyCompleteness: 'not-proven',
    ready: document.readyState,
    streamingHint: !!document.querySelector('[data-testid="stop-button"]'),
    count: all.length, inspected: rows.length, bounded,
    missingIds: rows.filter(r => !r.id).length,
    duplicateIds: rows.filter(r => r.id).length
      - new Set(rows.filter(r => r.id).map(r => r.id)).size,
    nestedAuthorNodes: rows.reduce((n, r) => n + r.nestedAuthorNodes, 0),
    chars: rows.reduce((n, r) => n + r.chars, 0),
    signature: hash(JSON.stringify(rows.map(r => [r.id, r.role, r.chars, r.fingerprint]))),
    // No conversation text, credentials, storage or network access.
    samples: rows.filter((r, i) => i < 2 || i >= rows.length - 2)
  });
})()
