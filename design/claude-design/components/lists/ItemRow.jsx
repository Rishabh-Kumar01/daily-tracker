import React from 'react';

export function ItemRow({ name, value, checked = false, disabled = false, accent = 'diet', onChange }) {
  const ac = `var(--accent-${accent})`;
  return (
    <div role="checkbox" aria-checked={checked} tabIndex={disabled ? -1 : 0}
      onClick={disabled ? undefined : () => onChange && onChange(!checked)}
      style={{
        display: 'flex', alignItems: 'center', gap: 'var(--sp-3)',
        minHeight: 'var(--hit-min)', padding: '0 var(--sp-4)',
        borderRadius: 'var(--radius-md)', boxSizing: 'border-box',
        background: checked ? 'var(--surface-2)' : 'transparent',
        opacity: disabled ? 'var(--disabled-opacity)' : 1,
        pointerEvents: disabled ? 'none' : 'auto',
        cursor: 'pointer', userSelect: 'none',
      }}>
      <span style={{
        width: 22, height: 22, flex: 'none', borderRadius: 'var(--radius-xs)',
        border: `2px solid ${checked ? ac : 'var(--outline)'}`,
        background: checked ? ac : 'transparent', boxSizing: 'border-box',
        display: 'grid', placeItems: 'center', color: 'var(--on-accent)',
      }}>
        {checked ? <span className="material-symbols-rounded" style={{ fontSize: 16, fontVariationSettings: "'FILL' 1, 'wght' 600" }}>check</span> : null}
      </span>
      <span style={{
        font: 'var(--type-body-lg)', minWidth: 0, flex: 1,
        whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
        color: 'var(--text-body)',
        textDecoration: checked ? 'line-through' : 'none',
        textDecorationColor: 'var(--on-surface-faint)',
      }}>{name}</span>
      <span style={{ font: 'var(--type-numeric)', color: 'var(--text-secondary)', flex: 'none' }}>{value}</span>
    </div>
  );
}
