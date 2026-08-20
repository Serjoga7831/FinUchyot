(function (root) {
  'use strict';

  function normalizeLabel(value) {
    return String(value || '').replace(/\s+/g, ' ').trim().toLocaleLowerCase('ru-RU');
  }

  function findUnique(elements, allowedLabels, getLabel, isVisible) {
    const allowed = new Set(allowedLabels.map(normalizeLabel));
    const matches = Array.from(elements).filter(element =>
      isVisible(element) && allowed.has(normalizeLabel(getLabel(element)))
    );
    if (matches.length === 0) return { code: 'not_found', element: null };
    if (matches.length !== 1) return { code: 'ambiguous', element: null };
    return { code: 'found', element: matches[0] };
  }

  async function runAutomation(actions) {
    const share = actions.findShare();
    if (share.code !== 'found') return `share_${share.code}`;
    actions.click(share.element);
    const csv = await actions.findCsv();
    if (csv.code !== 'found') return `csv_${csv.code}`;
    actions.click(csv.element);
    return 'csv_clicked';
  }

  const api = Object.freeze({ normalizeLabel, findUnique, runAutomation });
  root.FinUchyotAutomationCore = api;
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
})(typeof globalThis !== 'undefined' ? globalThis : this);
