Confirmation bottom sheet: title, a list of editable fields (label left, mono value right), and Cancel / Confirm actions.

```jsx
<ConfirmSheet title="Log workout" accent="workout"
  fields={[
    { label: 'Exercise', value: 'Bench press' },
    { label: 'Sets × reps', value: '4 × 8' },
    { label: 'Weight', value: '60', suffix: 'kg' },
  ]}
  onConfirm={save} onCancel={close} />
```

- `focusedField` highlights one field with an accent border (selected/editing state).
- Values are plain text inputs; wire `onFieldChange` for live editing.
- `disabled` renders the whole sheet inert at 38% opacity.
