# C1.1 — Room schema and migration test preparation

**Status pakietu (2026-11-13):** zrealizowany w kodzie (`6c4d2a5`, `597d887`, `8d5a0a5`)
i potwierdzony w CI (workflow #158, SUCCESS). Niezaakceptowany przez właściciela.

## Baseline

- Branch: work/vireo-development
- Base commit: 6c4d2a5bc8c9cde80005bd6d340e4805e0d87383
- Room version: 2.8.5
- Database version: 14
- exportSchema: true
- KSP schema location: app/schemas/
- room-testing: 2.8.5

## Scope of this package

1. Publish generated Room schema JSON files as a separate GitHub Actions artifact. — **wykonane**
2. Fail the workflow if the schema JSON files are not generated. — **wykonane**
3. Preserve the existing unit-test, signing and APK-build steps. — **zachowane**
4. Do not modify database entities, migrations or database version. — **zachowane (v14 bez zmian)**
5. Do not modify Gradle, AGP, Kotlin, compileSdk or Media3 versions. — **zachowane**
6. Do not modify signing configuration or GitHub secrets. — **zachowane**

## Expected artifact

Artifact name:

vireo-room-schemas

Rzeczywista ścieżka pliku w artefakcie (potwierdzona przez `gh run download`):

app.vireo.data.local.VireoDatabase/14.json

Room zapisuje schemat w podkatalogu nazwanym od klasy bazy, dlatego ścieżka
w workflow to `app/schemas/**/*.json` (poprawka w `8d5a0a5`).

Potwierdzone dla schematu v14:

- rozmiar artefaktu: 3 068 B,
- `identityHash`: `410131225083a186b43291867168bf6e`,
- 14 encji,
- `library_items` bez FK,
- `live_channel_history` z FK tylko do `profiles`,
- indeks `index_epg_programs_sourceId_streamId_endTimeMs` istnieje.

Plik schematu nie jest zacommitowany w repozytorium — istnieje wyłącznie
jako artefakt CI.

## Migration 14 -> 15 test matrix

The future migration tests must cover:

- Existing profiles and their data remain intact.
- Active Xtream account is assigned to every existing profile.
- The active account becomes the default source for those profiles.
- Other saved accounts remain unassigned.
- No active account: profiles remain without source assignments.
- No profiles: migration completes without creating invalid references.
- Multiple profiles receive independent source assignments.
- Default source always belongs to the profile's assigned source set.
- Duplicate profile/source assignment is rejected.
- A profile cannot have more than one default source.
- Foreign-key integrity is preserved.
- Existing library, history, catalog and EPG data remain intact.
- Failed migration does not leave partially published data.

## Important implementation constraints

- Do not implement migration 14 -> 15 in C1.1.
- Do not change the Room database version in C1.1.
- Do not use destructive migration fallback.
- Do not assume Room v1 was deployed.
- Do not add dependencies beyond the already approved room-testing 2.8.5.
- Instrumentation runner and CI emulator requirements must be checked before adding
  any further test dependencies or workflow execution steps.

## Next stage

After obtaining and reviewing schema 14.json:

1. Confirm the exact Room schema and migration starting point.
2. Inspect existing Android test infrastructure.
3. Determine the minimum approved instrumentation test setup.
4. Implement and verify migration tests.
5. Only then implement production migration 14 -> 15.

Stan po 2026-11-13:

- Punkt 1 — schemat v14 pobrany i przejrzany (szczegóły wyżej).
- Punkt 2 — sprawdzone: brak `app/src/androidTest/`, brak `testInstrumentationRunner`,
  brak `MigrationTestHelper`. Zadeklarowana zależność `room-testing:2.8.5`
  nie jest używana.
- Punkt 3 — zatwierdzony przez właściciela: Wariant A (`androidTest` +
  `MigrationTestHelper`) i Opcja A1 w CI (emulator API 34 +
  `connectedDebugAndroidTest`).
- Punkt 4 — zaimplementowany w working tree (C1.2, 2026-11-13):
  `app/schemas/app.vireo.data.local.VireoDatabase/14.json`,
  `VireoDatabaseMigrationTest.kt`, konfiguracja `androidTest` w
  `app/build.gradle.kts`, job `android-test` w `.github/workflows/build.yml`.
  Weryfikacja CI i urządzenia — **nie wykonana** (brak commita).
- Punkt 5 — nierozpoczęty (wymaga zamknięcia C1.2).

## Verification status

C1.1 script prepares schema artifact publication and this test plan.

No Gradle build or tests are run by the Termux script.

GitHub Actions verification (potwierdzone dla `work/vireo-development`):

- #156 (`6c4d2a5`) — SUCCESS; brak jeszcze kroku publikacji schematu.
- #157 (`597d887`) — FAILURE; `Upload Room schema artifact` nie znalazł
  `app/schemas/*.json` (plik jest w podkatalogu per klasa bazy).
- #158 (`8d5a0a5`) — SUCCESS; artefakt `vireo-room-schemas` opublikowany
  (3 068 B, dostępność do 2026-12-12), obok `vireo-debug-apk` (14 363 395 B).

Wnioski:

- Publikacja schematu działa; schemat v14 został faktycznie wygenerowany.
- Nie istnieją jeszcze testy migracji — sam eksport schematu ich nie zastępuje.
- Sukces CI nie oznacza akceptacji pakietu C1.1.
