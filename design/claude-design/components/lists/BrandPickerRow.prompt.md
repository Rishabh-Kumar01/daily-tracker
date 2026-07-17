Product search-result row for picking a branded food: 44px thumbnail, uppercase brand caption, product name, mono per-100g macro line.

```jsx
<BrandPickerRow brand="Fage" product="Total 0% Greek Yogurt"
  per100g="per 100g · 54 kcal · 10.3P · 3.0C · 0.2F" onClick={pick} />
<BrandPickerRow brand="Barilla" product="Spaghetti n.5" per100g="per 100g · 359 kcal · 12.5P · 71.7C · 2.0F" selected />
```

- `selected` = accent border + container fill + trailing check_circle.
- No `thumbnailUrl` → striped placeholder (never draw product art).
- Macro line format: `per 100g · {kcal} kcal · {p}P · {c}C · {f}F`.
