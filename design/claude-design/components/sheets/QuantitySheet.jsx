import React from 'react';

function fmt(n) { return n % 1 === 0 ? String(n) : n.toFixed(1); }

export function QuantitySheet({ brand, product, per100g = { kcal: 0, protein: 0, carbs: 0, fat: 0 },
  initialGrams = 100, step = 10, accent = 'diet', edited = false, disabled = false, onAdd, onCancel }) {
  const [grams, setGrams] = React.useState(initialGrams);
  const ac = `var(--accent-${accent})`;
  const k = grams / 100;
  const macros = [
    { label: 'kcal', v: Math.round(per100g.kcal * k), hot: true },
    { label: 'protein', v: fmt(per100g.protein * k) + 'g' },
    { label: 'carbs', v: fmt(per100g.carbs * k) + 'g' },
    { label: 'fat', v: fmt(per100g.fat * k) + 'g' },
  ];
  const stepBtn = (dir) => (
    <button aria-label={dir > 0 ? 'more' : 'less'} disabled={disabled}
      onClick={() => setGrams(g => Math.max(0, g + dir * step))}
      style={{
        width: 'var(--hit-min)', height: 'var(--hit-min)', borderRadius: 'var(--radius-full)',
        border: 'none', background: 'var(--surface-3)', color: 'var(--on-surface)',
        display: 'grid', placeItems: 'center', cursor: 'pointer', font: 'inherit',
      }}>
      <span className="material-symbols-rounded" style={{ fontSize: 22 }}>{dir > 0 ? 'add' : 'remove'}</span>
    </button>
  );
  const changed = edited || grams !== initialGrams;
  return (
    <div style={{
      background: 'var(--surface-sheet)', borderRadius: 'var(--radius-xl) var(--radius-xl) 0 0',
      boxShadow: 'var(--shadow-sheet)', padding: 'var(--sp-2) var(--sp-4) var(--sp-4)',
      opacity: disabled ? 'var(--disabled-opacity)' : 1,
      pointerEvents: disabled ? 'none' : 'auto', boxSizing: 'border-box',
    }}>
      <div style={{ width: 32, height: 4, borderRadius: 'var(--radius-full)', background: 'var(--outline)', margin: '0 auto var(--sp-3)' }}></div>
      <div style={{ font: 'var(--type-caption)', color: 'var(--text-secondary)', letterSpacing: 'var(--tracking-label)', textTransform: 'uppercase' }}>{brand}</div>
      <div style={{ font: 'var(--type-title-lg)', color: 'var(--text-body)', marginBottom: 'var(--sp-4)' }}>{product}</div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 'var(--sp-6)', marginBottom: 'var(--sp-4)' }}>
        {stepBtn(-1)}
        <div style={{ textAlign: 'center', minWidth: 88 }}>
          <span style={{ font: 'var(--type-numeric-lg)', color: changed ? ac : 'var(--on-surface)' }}>{grams}</span>
          <span style={{ font: 'var(--type-body)', color: 'var(--text-secondary)' }}> g</span>
        </div>
        {stepBtn(1)}
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 'var(--sp-2)', marginBottom: 'var(--sp-4)' }}>
        {macros.map(m => (
          <div key={m.label} style={{ background: 'var(--surface-3)', borderRadius: 'var(--radius-md)', padding: 'var(--sp-2)', textAlign: 'center' }}>
            <div style={{ font: 'var(--type-numeric)', color: m.hot ? ac : 'var(--on-surface)' }}>{m.v}</div>
            <div style={{ font: 'var(--type-caption)', color: 'var(--on-surface-faint)' }}>{m.label}</div>
          </div>
        ))}
      </div>
      <div style={{ display: 'flex', gap: 'var(--sp-3)' }}>
        <button onClick={onCancel} disabled={disabled} style={{
          flex: 'none', height: 'var(--hit-min)', padding: '0 var(--sp-5)', borderRadius: 'var(--radius-full)',
          border: '1px solid var(--outline)', background: 'transparent', color: 'var(--on-surface)',
          font: 'var(--type-label)', letterSpacing: 'var(--tracking-label)', cursor: 'pointer',
        }}>Cancel</button>
        <button onClick={() => onAdd && onAdd(grams)} disabled={disabled} style={{
          flex: 1, height: 'var(--hit-min)', borderRadius: 'var(--radius-full)', border: 'none',
          background: ac, color: 'var(--on-accent)',
          font: 'var(--type-label)', letterSpacing: 'var(--tracking-label)', cursor: 'pointer',
        }}>Add to log</button>
      </div>
    </div>
  );
}
