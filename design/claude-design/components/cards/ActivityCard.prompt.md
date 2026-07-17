Home-screen entry card for one activity type (Diet, Workout, Study, Sleep) with icon chip, name, and a one-line today summary.

```jsx
<ActivityCard activity="diet" summary="1,840 kcal · 132g protein" onClick={openDiet} />
<ActivityCard activity="sleep" summary="7h 20m · bed 23:40" selected />
<ActivityCard activity="study" summary="Nothing logged yet" disabled />
```

- `activity` sets the accent hue and default icon/name; override with `name`/`icon`.
- `selected` = accent border + container fill + solid icon chip. `disabled` = 38% opacity, inert.
- Summary copy: values joined by `·`, no sentences.
