(function () {
  'use strict';

  const NATIVE_APP = 'finuchyot';
  const OPERATIONS_PATH = '/mybank/operations';
  const core = globalThis.FinUchyotAutomationCore;
  if (!core || location.protocol !== 'https:' ||
      !['tbank.ru', 'www.tbank.ru'].includes(location.hostname) ||
      !(location.pathname === OPERATIONS_PATH || location.pathname.startsWith(`${OPERATIONS_PATH}/`))) {
    return;
  }

  const port = browser.runtime.connectNative(NATIVE_APP);
  let busy = false;

  function labelOf(element) {
    return element.getAttribute('aria-label') ||
      element.getAttribute('title') ||
      element.textContent || '';
  }

  function isVisible(element) {
    if (!element || element.disabled || element.getAttribute('aria-disabled') === 'true') return false;
    const style = getComputedStyle(element);
    return style.display !== 'none' && style.visibility !== 'hidden' &&
      style.opacity !== '0' && element.getClientRects().length > 0;
  }

  function candidates() {
    return document.querySelectorAll(
      'button,[role="button"],[role="menuitem"],[role="option"],a,label'
    );
  }

  function findShare() {
    return core.findUnique(
      candidates(),
      ['Поделиться', 'Экспорт', 'Скачать отчёт'],
      labelOf,
      isVisible
    );
  }

  function findCsvNow() {
    return core.findUnique(candidates(), ['CSV'], labelOf, isVisible);
  }

  async function waitForCsv() {
    for (let attempt = 0; attempt < 50; attempt++) {
      const result = findCsvNow();
      if (result.code === 'found' || result.code === 'ambiguous') return result;
      await new Promise(resolve => setTimeout(resolve, 100));
    }
    return { code: 'not_found', element: null };
  }

  async function start() {
    if (busy) {
      port.postMessage({ type: 'automationResult', code: 'busy' });
      return;
    }
    if (location.pathname !== OPERATIONS_PATH && !location.pathname.startsWith(`${OPERATIONS_PATH}/`)) {
      port.postMessage({ type: 'automationResult', code: 'wrong_page' });
      return;
    }
    busy = true;
    try {
      const code = await core.runAutomation({
        findShare,
        findCsv: waitForCsv,
        click: element => element.click()
      });
      port.postMessage({ type: 'automationResult', code });
    } catch (_) {
      port.postMessage({ type: 'automationResult', code: 'automation_error' });
    } finally {
      busy = false;
    }
  }

  port.onMessage.addListener(message => {
    if (message && message.type === 'downloadCsv') start();
  });
  port.postMessage({ type: 'automationResult', code: 'ready' });
})();
