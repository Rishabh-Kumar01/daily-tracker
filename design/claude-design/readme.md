# Daily Tracker Design System

Design system for **Daily Tracker** — a personal, offline-first Android app for tracking daily activities: **Diet, Workout, Study, Sleep**. Material 3 feel, dark-mode-first (dark is the only authored theme). Built from scratch per the owner's spec — no external Figma/codebase sources.

## Content fundamentals
- Copy is terse and data-forward: values over sentences. "1,840 kcal · 132g protein", not "You ate 1,840 calories today."
- Middle-dot `·` separates values on one line. Units are lowercase and tight: `132g`, `7h 20m`, `45 min`.
- Sentence case everywhere (buttons too: "Confirm", "Add to log"). No exclamation marks, no emoji.
- Second person only in empty states ("Nothing logged yet"). Never "I".
- Numbers use tabular mono type (`--type-numeric`) when right-aligned or live-updating.

## Visual foundations
- **Base**: near-black cool neutrals (`--surface-0…3`), tonal elevation (lighter surface = higher), no drop shadows except sheets.
- **Accents**: one hue per activity, identical chroma/lightness (oklch 0.80/0.14): Diet green · Workout orange · Study blue · Sleep violet. Each has a `-container` tint for selected fills and icon chips. Never mix two accents in one component.
- **Type**: Roboto (UI) + Roboto Mono (numbers). Scale in `tokens/typography.css`.
- **Shape**: cards 16px, rows 12px, thumbs 8px, sheets 28px top corners, steppers/pills full-round.
- **States**: selected = 1.5px accent border + `-container` fill; pressed = `--state-pressed` overlay; disabled = `opacity: var(--disabled-opacity)` (0.38) + no pointer events. No hover-dependent affordances (touch-first).
- **Motion**: quick and functional — 150–250ms, ease-out; sheets slide up with scrim fade. No bounces.
- **Layout**: 16px screen gutters, 4px grid, 48px minimum hit targets, 56px list rows.
- Imagery: no photography; product thumbnails are user content — use striped placeholder blocks until real images exist.

## Iconography
Google **Material Symbols Rounded** (FILL 1), loaded from Google Fonts CDN in `tokens/fonts.css`. Use via `<span class="material-symbols-rounded">restaurant</span>`. Canonical activity icons: Diet `restaurant`, Workout `fitness_center`, Study `school`, Sleep `bedtime`. No emoji, no hand-drawn SVGs. **No logo exists** — render "Daily Tracker" in plain type where a mark would go.

## Index
- `styles.css` — global entry; imports `tokens/` (fonts, colors, typography, spacing)
- `components/cards/` — ActivityCard
- `components/lists/` — ItemRow, BrandPickerRow
- `components/sheets/` — QuantitySheet, ConfirmSheet
- `guidelines/` — foundation specimen cards
- `canvas/index.html` — all five components in default / selected / disabled states
- `SKILL.md` — agent-facing usage guide

## Intentional additions
None — component inventory is exactly the five named in the spec.
