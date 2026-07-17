// Loads Daily Tracker components: prefers the compiled _ds_bundle.js, falls back to
// transpiling the .jsx sources with Babel standalone. Usage: loadDS('../').then(NS => …)
window.loadDS = async function (root) {
  function findNS() {
    for (const g of [window.DailyTracker, window.DailyTrackerDS, window.DS]) { if (g && g.ActivityCard) return g; }
    for (const k of Object.getOwnPropertyNames(window)) {
      try { const v = window[k]; if (v && typeof v === 'object' && v.ActivityCard && v.QuantitySheet) return v; } catch (e) {}
    }
    return null;
  }
  let ns = findNS();
  if (ns) return ns;
  try {
    const r = await fetch(root + '_ds_bundle.js');
    if (r.ok) { (0, eval)(await r.text()); ns = findNS(); if (ns) return ns; }
  } catch (e) {}
  const paths = [
    'components/cards/ActivityCard.jsx',
    'components/lists/ItemRow.jsx',
    'components/lists/BrandPickerRow.jsx',
    'components/sheets/QuantitySheet.jsx',
    'components/sheets/ConfirmSheet.jsx',
  ];
  const NS = {};
  for (const p of paths) {
    const src = await fetch(root + p).then(r => r.text());
    const code = Babel.transform(src, { presets: [['env', { modules: 'cjs' }], ['react', { runtime: 'classic' }]], sourceType: 'module', filename: p }).code;
    const module = { exports: {} };
    new Function('exports', 'module', 'require', 'React', code)(
      module.exports, module,
      (n) => { if (n === 'react') return React; throw new Error('Cannot require ' + n); },
      React
    );
    Object.assign(NS, module.exports);
  }
  return NS;
};
