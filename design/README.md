# design/

Imported UI design intent for Daily Tracker. **Nothing here ships.** No file in this
directory is built, bundled, or referenced by the Android app.

## What's here

`claude-design/` is a verbatim import of the "Daily Tracker Design System" project from
Claude Design, pulled 2026-07-17 via the claude_design MCP.

- Source project: `b685e127-39b4-4576-82b1-5423f0d3ee5b` (owner: Rishabh)
- Entry point named in the import request: `claude-design/Lunch Screen.dc.html`
- Files are byte-for-byte as fetched, so a future re-sync produces a clean diff.

The Lunch Screen covers three states: default food list, Paneer expanded into its brand
rows, and the QuantitySheet open over a scrim.

## How to use it

Per CLAUDE.md, treat these as **intent, not source**. Match the layout, spacing, states,
and the named components — ActivityCard, ItemRow, BrandPickerRow, QuantitySheet,
ConfirmSheet — but implement them as Jetpack Compose per the `android-ondevice-ai` skill.
**Never port the HTML/JSX.** The `.jsx` files exist to pin down props, states, and visual
rules; the `.prompt.md` and `.d.ts` files next to each are the most useful part for that.

The design's per-100g × grams macro math (`QuantitySheet.jsx`, and the Lunch Screen's
`renderVals`) already matches the architecture's read-time nutrition rule. That agreement
is the thing to preserve when this becomes Compose.

## Notes on imported content

Two things the security hooks flagged during import, both inherent to the upstream preview
harness and left unmodified on purpose:

- `ds-loader.js` uses `new Function()` to transpile the local `.jsx` sources, and the
  `.card.html` previews load React/Babel from unpkg without SRI hashes.

These only run if you open the preview HTML in a browser locally. They are not part of the
Android build and carry no runtime risk to the app. Left as-is so the import stays a clean
mirror of the source project — do not "fix" them here; fix them upstream in Claude Design
if it ever matters.
