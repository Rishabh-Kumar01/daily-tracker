import React from 'react';

const STRIPES = 'repeating-linear-gradient(45deg, var(--surface-3) 0 6px, var(--surface-2) 6px 12px)';

export function BrandPickerRow({ brand, product, per100g, thumbnailUrl, accent = 'diet', selected = false, disabled = false, onClick }) {
  const ac = `var(--accent-${accent})`;
  return (
    <div role="option" aria-selected={selected} tabIndex={disabled ? -1 : 0}
      onClick={disabled ? undefined : onClick}
      style={{
        display: 'flex', alignItems: 'center', gap: 'var(--sp-3)',
        minHeight: 'var(--row-height)', padding: 'var(--sp-2) var(--sp-3)',
        borderRadius: 'var(--radius-md)', boxSizing: 'border-box',
        background: selected ? `var(--accent-${accent}-container)` : 'transparent',
        border: `1.5px solid ${selected ? ac : 'transparent'}`,
        opacity: disabled ? 'var(--disabled-opacity)' : 1,
        pointerEvents: disabled ? 'none' : 'auto',
        cursor: 'pointer', userSelect: 'none',
      }}>
      <div style={{
        width: 44, height: 44, flex: 'none', borderRadius: 'var(--radius-sm)',
        background: thumbnailUrl ? `center/cover url(${thumbnailUrl})` : STRIPES,
        display: 'grid', placeItems: 'center', overflow: 'hidden',
      }}>
        {!thumbnailUrl && <span style={{ font: '500 9px/1 var(--font-mono)', color: 'var(--on-surface-faint)' }}>IMG</span>}
      </div>
      <div style={{ minWidth: 0, flex: 1 }}>
        <div style={{ font: 'var(--type-caption)', color: selected ? ac : 'var(--text-secondary)', letterSpacing: 'var(--tracking-label)', textTransform: 'uppercase' }}>{brand}</div>
        <div style={{ font: 'var(--type-body-lg)', color: 'var(--text-body)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{product}</div>
        <div style={{ font: 'var(--type-caption)', color: 'var(--on-surface-faint)', fontFamily: 'var(--font-mono)' }}>{per100g}</div>
      </div>
      {selected && <span className="material-symbols-rounded" style={{ flex: 'none', color: ac, fontSize: 20 }}>check_circle</span>}
    </div>
  );
}
