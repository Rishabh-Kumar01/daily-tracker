Bottom sheet for choosing a quantity: grams stepper (±step) with a live 4-up macro readout (kcal highlighted in accent) and Cancel / Add to log actions.

```jsx
<QuantitySheet brand="Fage" product="Total 0% Greek Yogurt"
  per100g={{ kcal: 54, protein: 10.3, carbs: 3.0, fat: 0.2 }}
  initialGrams={150} onAdd={(g) => log(g)} onCancel={close} />
```

- Macros recompute live as grams change; once edited, the grams readout turns accent (`edited` forces it).
- Render above a `--scrim` overlay, pinned to the bottom edge; sheet owns its 28px top radius and shadow.
- `disabled` renders the whole sheet inert at 38% opacity.
