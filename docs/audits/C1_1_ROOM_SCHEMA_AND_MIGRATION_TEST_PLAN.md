# C1.1 — Room schema and migration test preparation

## Baseline

- Branch: work/vireo-development
- Base commit: 6c4d2a5bc8c9cde80005bd6d340e4805e0d87383
- Room version: 2.8.5
- Database version: 14
- exportSchema: true
- KSP schema location: app/schemas/
- room-testing: 2.8.5

## Scope of this package

1. Publish generated Room schema JSON files as a separate GitHub Actions artifact.
2. Fail the workflow if the schema JSON files are not generated.
3. Preserve the existing unit-test, signing and APK-build steps.
4. Do not modify database entities, migrations or database version.
5. Do not modify Gradle, AGP, Kotlin, compileSdk or Media3 versions.
6. Do not modify signing configuration or GitHub secrets.

## Expected artifact

Artifact name:

vireo-room-schemas

Expected schema file:

app/schemas/14.json

The actual file must be inspected after the workflow run.
Its existence and contents must not be inferred from a successful APK build.

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

## Verification status

C1.1 script prepares schema artifact publication and this test plan.

No Gradle build or tests are run by the Termux script.
GitHub Actions verification remains pending.
