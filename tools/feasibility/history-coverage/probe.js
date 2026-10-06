(() => {
  // Read-only metadata, deliberately no network, storage, Fiber or page hooks.
  if (window !== window.top || location.origin !== 'https://chatgpt.com')
    return JSON.stringify({error: 'wrong-origin-or-frame'});
  const route = location.pathname;
  if (!/^\/g\/g-p-[^/]+\/c\/[^/]+\/?$/.test(route))
    return JSON.stringify({error: 'not-project-conversation'});
  const nodes = [...document.querySelectorAll('[data-message-author-role]')]
    .filter(e => ['user', 'assistant'].includes(e.getAttribute('data-message-author-role')));
  const rows = nodes.slice(0, 2000).map(e => ({
    id: e.getAttribute('data-message-id')
      || e.closest('[data-message-id]')?.getAttribute('data-message-id') || '',
    role: e.getAttribute('data-message-author-role'),
    nested: e.querySelectorAll('[data-message-author-role]').length
  }));
  return JSON.stringify({
    schema: 1, route, routeAfter: location.pathname, capturedAt: new Date().toISOString(),
    historyCompleteness: 'not-proven', ready: document.readyState,
    streamingHint: !!document.querySelector('[data-testid="stop-button"]'),
    count: nodes.length, bounded: nodes.length > 2000, rows
  });
})()
