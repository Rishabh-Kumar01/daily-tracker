# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status

Kotlin/Compose Android project, scaffolded and building. Sole module is `app`. Build/test entry points: `./gradlew assembleDebug` (build), `./gradlew installDebug` (build + install to connected device), `./gradlew test` (unit), `./gradlew connectedAndroidTest` (instrumented — needs a device/emulator), `./gradlew test --tests '*ClassName.methodName'` (single test). Launch on device: `adb -s <device-id> shell am start -n dev.rishabh.dailytracker/.MainActivity`.

## Read these before writing anything

Two skills hold the authoritative design. They are not background reading; they are the spec.

- **`daily-tracker-architecture`** — WHAT to build. Core principles, the closed field-type vocabulary, template hierarchy, build phases, settled product decisions. Its references: `references/database-schema.md` (read before any Room/DAO/migration/query work) and `references/ai-pipelines.md` (read before any AI, nutrition, study, or sleep work).
- **`android-ondevice-ai`** — HOW to build it in Kotlin/Compose. Module layout, stack, the dynamic field renderer, LiteRT-LM lifecycle, WorkManager AI jobs, alarm/lock services, testing conventions.
- **`PHASE-2A-DIET.md`** (repo root) — the current milestone plan (M8–M11): completing the Diet module into a daily driver. Consult it for all Diet-completion work.

Extend those schemas; do not invent parallel ones. Follow the Phase 1→4 build order defined there.

## Progress

- **Phase 1 (M1–M7): complete**, committed, and verified on-device — scaffold, Compose theme from design tokens, full Room data layer + seeder, component library + generic FieldRenderer, Home + template navigation, Diet meal flow with read-time macros, and the barcode lane via Open Food Facts.
- **Phase 2a (M8–M11): complete**, committed, and verified on-device — see PHASE-2A-DIET.md. M8: bundled generic foods + USDA lookup + serving units. M9: My Foods library + product lifecycle (archive/edit). M10: product photos (OFF + camera) + re-scan feedback. M11: meal templates ("usual meals") for one-tap logging + a daily macro rollup with optional per-macro goals. Room DB at version 5.
- **Phase 2b onward: not started** — deterministic-smart features (FSRS spaced repetition, frequency-ranked suggestions, weakness heatmaps, the Sleep activity), then Phase 3 (local LLM) and Phase 4 (cloud lanes + insights).

## The shape of the app, in one paragraph

Offline-first, single-user Android tracker (Diet, Workout, Study, Sleep, plus user-defined activities). One template engine drives everything: **activities are data, not code** — a new activity is new rows in `activity_template → sub_menus → items → item_fields`, rendered by a single generic `FieldRenderer` Composable, never a new table or screen. Everything logged lands in a uniform `log_entry` + `log_values`, so calendar, streaks, exports, and cross-activity insights are written once. Nutrition is never stored computed — macros are `product_nutrients × grams / 100` at read time, so fixing a product retroactively fixes history.

## Rules that bite if forgotten

- **AI proposes, user disposes.** Every AI output (extracted labels, generated templates, chapter splits) renders in an editable confirmation screen before it is saved.
- **Deterministic validators sit between every LLM output and the database.** Strict kotlinx.serialization parsing, closed enums, app-generated IDs. Malformed → one repair retry → fail the job gracefully.
- **Sensitive media never leaves the device.** `media.sensitive = 1` rows are local-model-only, app-private, excluded from backup.
- **Call sites never know which AI backend ran.** Everything goes through the `LlmEngine` interface and the router.
- **Respect the phase order** (1: no AI · 2: deterministic smart · 3: local LLM · 4: cloud lanes + insights). Don't gold-plate an earlier phase.
- **Verify library versions and API names against current docs before adding dependencies.** The on-device AI surface (LiteRT-LM, MediaPipe) churns; don't trust memory here.
- **Never claim the sleep lock is unbypassable.** An unstoppable alarm/lock is a bug — the mission alarm falls back to math problems after N failed photo attempts.

## Target device

Sole target: **OnePlus 9 Pro 5G (Snapdragon 888, 12GB RAM, 256GB), Android 14, sideloaded** — no Play Store distribution or policy constraints.

- `minSdk = 34`, target the latest SDK. **No backward-compat code paths, ever.**
- Local LLM: **Gemma 3n E2B by default; E4B only if benchmarked acceptable on-device.** SD888 thermals are the real limit — heavy AI jobs (PDF ingest, MCQ bank generation) run only while charging.
- **OxygenOS aggressively kills background apps.** First-run setup must walk the user through excluding the app from battery optimization and enabling auto-launch — WorkManager jobs and the wake alarm depend on it.
- Alarms: `USE_EXACT_ALARM` (genuine alarm-clock functionality) + full-screen intent over lockscreen + `BOOT_COMPLETED` receiver that reschedules from `sleep_sessions`.
- Install path: ADB from the dev machine or APKs attached to `gh release`.

## Tooling

- **Use the `gh` CLI for all GitHub operations** (repo creation, branches, PRs, issues). There is no GitHub MCP server; do not try to add or call one. Authenticated as the repo owner.
- **UI designs arrive via the Claude Design MCP (`/design-sync`)** — pull the design system and screens from the "Daily Tracker" Claude Design project and build against those components; approve sync plans before they write. Fallback: handoff bundles (HTML/CSS + screenshots + README) committed under `design/`. Either way, treat designs as intent: match layout, spacing, states, and the named components (ActivityCard, ItemRow, BrandPickerRow, QuantitySheet, ConfirmSheet) — but implement them as Jetpack Compose per the `android-ondevice-ai` skill, not by porting the HTML.
- Conventional commits (`feat:`, `fix:`, `chore:`, `refactor:`, `test:`). Small, single-purpose commits; commit after each working increment.