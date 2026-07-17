import React from 'react';

export function ConfirmSheet({ title, fields = [], accent = 'diet', focusedField = -1, disabled = false,
  confirmLabel = 'Confirm', cancelLabel = 'Cancel', onConfirm, onCancel, onFieldChange }) {
  const ac = `var(--accent-${accent})`;
  return (
    <div style={{
      background: 'var(--surface-sheet)', borderRadius: 'var(--radius-xl) var(--radius-xl) 0 0',
      boxShadow: 'var(--shadow-sheet)', padding: 'var(--sp-2) var(--sp-4) var(--sp-4)',
      opacity: disabled ? 'var(--disabled-opacity)' : 1,
      pointerEvents: disabled ? 'none' : 'auto', boxSizing: 'border-box',
    }}>
      <div style={{ width: 32, height: 4, borderRadius: 'var(--radius-full)', background: 'var(--outline)', margin: '0 auto var(--sp-3)' }}></div>
      <div style={{ font: 'var(--type-title-lg)', color: 'var(--text-body)', marginBottom: 'var(--sp-4)' }}>{title}</div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--sp-2)', marginBottom: 'var(--sp-4)' }}>
        {fields.map((f, i) => (
          <label key={i} style={{
            display: 'flex', alignItems: 'center', gap: 'var(--sp-3)',
            minHeight: 'var(--hit-min)', padding: '0 var(--sp-3)', boxSizing: 'border-box',
            background: 'var(--surface-3)', borderRadius: 'var(--radius-md)',
            border: `1.5px solid ${i === focusedField ? ac : 'transparent'}`,
          }}>
            <span style={{ font: 'var(--type-caption)', color: 'var(--text-secondary)', letterSpacing: 'var(--tracking-label)', textTransform: 'uppercase', flex: 1 }}>{f.label}</span>
            <input value={f.value} disabled={disabled}
              onChange={(e) => onFieldChange && onFieldChange(i, e.target.value)}
              style={{
                background: 'transparent', border: 'none', outline: 'none', textAlign: 'right',
                font: 'var(--type-numeric)', color: i === focusedField ? ac : 'var(--on-surface)',
                width: 110, padding: 0,
              }} />
            {f.suffix && <span style={{ font: 'var(--type-caption)', color: 'var(--on-surface-faint)' }}>{f.suffix}</span>}
          </label>
        ))}
      </div>
      <div style={{ display: 'flex', gap: 'var(--sp-3)' }}>
        <button onClick={onCancel} disabled={disabled} style={{
          flex: 'none', height: 'var(--hit-min)', padding: '0 var(--sp-5)', borderRadius: 'var(--radius-full)',
          border: '1px solid var(--outline)', background: 'transparent', color: 'var(--on-surface)',
          font: 'var(--type-label)', letterSpacing: 'var(--tracking-label)', cursor: 'pointer',
        }}>{cancelLabel}</button>
        <button onClick={onConfirm} disabled={disabled} style={{
          flex: 1, height: 'var(--hit-min)', borderRadius: 'var(--radius-full)', border: 'none',
          background: ac, color: 'var(--on-accent)',
          font: 'var(--type-label)', letterSpacing: 'var(--tracking-label)', cursor: 'pointer',
        }}>{confirmLabel}</button>
      </div>
    </div>
  );
}
