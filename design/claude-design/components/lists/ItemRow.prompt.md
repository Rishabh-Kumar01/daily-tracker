Checkable list row: checkbox, item name, right-aligned secondary value in mono type. Whole row is the tap target.

```jsx
<ItemRow name="Chicken breast" value="264 kcal" accent="diet" onChange={toggle} />
<ItemRow name="Bench press 4×8" value="60 kg" accent="workout" checked />
<ItemRow name="Flashcards" value="20 min" accent="study" disabled />
```

- Checked = accent-filled checkbox + strikethrough name + subtle surface fill.
- `accent` matches the parent activity; `value` uses `--type-numeric` (mono).
