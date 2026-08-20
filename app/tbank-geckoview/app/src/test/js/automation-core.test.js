const test = require('node:test');
const assert = require('node:assert/strict');
const core = require('../../main/assets/finuchyot_automation/automation-core.js');

function element(label, visible = true) {
  return { label, visible };
}

test('finds one exact whitelisted share control', () => {
  const result = core.findUnique(
    [element('Операции'), element(' Поделиться '), element('Поделиться выпиской')],
    ['Поделиться', 'Экспорт'],
    item => item.label,
    item => item.visible
  );
  assert.equal(result.code, 'found');
  assert.equal(result.element.label, ' Поделиться ');
});

test('finds CSV case-insensitively but not longer labels', () => {
  const result = core.findUnique(
    [element('Excel'), element('csv'), element('CSV файл')],
    ['CSV'],
    item => item.label,
    item => item.visible
  );
  assert.equal(result.code, 'found');
  assert.equal(result.element.label, 'csv');
});

test('runs exactly share then CSV and stops on failures', async () => {
  const clicked = [];
  const success = await core.runAutomation({
    findShare: () => ({ code: 'found', element: 'share' }),
    findCsv: async () => ({ code: 'found', element: 'csv' }),
    click: element => clicked.push(element)
  });
  assert.equal(success, 'csv_clicked');
  assert.deepEqual(clicked, ['share', 'csv']);

  const failedClicks = [];
  const failure = await core.runAutomation({
    findShare: () => ({ code: 'ambiguous', element: null }),
    findCsv: async () => ({ code: 'found', element: 'csv' }),
    click: element => failedClicks.push(element)
  });
  assert.equal(failure, 'share_ambiguous');
  assert.deepEqual(failedClicks, []);
});

test('refuses ambiguous and missing controls', () => {
  const ambiguous = core.findUnique(
    [element('Поделиться'), element('Поделиться')],
    ['Поделиться'],
    item => item.label,
    item => item.visible
  );
  assert.equal(ambiguous.code, 'ambiguous');

  const missing = core.findUnique(
    [element('CSV', false), element('Excel')],
    ['CSV'],
    item => item.label,
    item => item.visible
  );
  assert.equal(missing.code, 'not_found');
});
