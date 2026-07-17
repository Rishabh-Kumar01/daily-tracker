import React from 'react';

const ICONS = { diet: 'restaurant', workout: 'fitness_center', study: 'school', sleep: 'bedtime' };
const NAMES = { diet: 'Diet', workout: 'Workout', study: 'Study', sleep: 'Sleep' };

export function ActivityCard({ activity = 'diet', name, icon, summary, selected = false, disabled = false, onClick }) {
  const accent = `var(--accent-${activity})`;
  const container = `var(--accent-${activity}-container)`;
  return (
    <div role="button" tabIndex={disabled ? -1 : 0} onClick={disabled ? undefined : onClick}
      style={{
        display: 'flex', alignItems: 'center', gap: 'var(--sp-4)',
        padding: 'var(--sp-4)', borderRadius: 'var(--radius-lg)',
        background: selected ? container : 'var(--surface-card)',
        border: `1.5px solid ${selected ? accent : 'transparent'}`,
        opacity: disabled ? 'var(--disabled-opacity)' : 1,
        pointerEvents: disabled ? 'none' : 'auto',
        cursor: 'pointer', userSelect: 'none', minHeight: 'var(--hit-min)', boxSizing: 'border-box',
      }}>
      <div style={{ width: 44, height: 44, borderRadius: 'var(--radius-full)', flex: 'none',
        display: 'grid', placeItems: 'center',
        background: selected ? accent : container,
        color: selected ? 'var(--on-accent)' : accent }}>
        <span className="material-symbols-rounded" style={{ fontSize: 22 }}>{icon || ICONS[activity]}</span>
      </div>
      <div style={{ minWidth: 0 }}>
        <div style={{ font: 'var(--type-title)', color: 'var(--text-body)' }}>{name || NAMES[activity]}</div>
        <div style={{ font: 'var(--type-body)', color: 'var(--text-secondary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{summary}</div>
      </div>
      <span className="material-symbols-rounded" aria-hidden="true" style={{ marginLeft: 'auto', flex: 'none', color: 'var(--on-surface-faint)', fontSize: 20 }}>chevron_right</span>
    </div>
  );
}
