# KeyMapper

## Project Overview

Android app for custom key/gamepad remapping, macros, and on-screen buttons. Supports Android 8.0+ (minSdk 26), targets API 36. Distributed on Google Play and F-Droid (FOSS flavor).

- **Package:** `io.github.sds100.keymapper`
- **Languages:** Kotlin (primary), Rust (event device handling), C++ (system bridge JNI)

## Build Commands

```bash
./gradlew assembleDebug       # debug APK
./gradlew assembleCi          # CI build (minified)
./gradlew assembleRelease     # release build (requires KEYSTORE_PASSWORD and KEY_PASSWORD env vars)
./gradlew clean               # clean build outputs
```

## Test & Lint Commands

```bash
# Kotlin unit tests
./gradlew testDebugUnitTest
./gradlew :base:testDebugUnitTest   # single module

# Kotlin lint
./gradlew ktlintCheck         # check
./gradlew ktlintFormat        # auto-fix

# Rust (run from evdev/src/main/rust/evdev_manager)
cargo fmt --check
cargo test --package evdev_manager_core
```

CI runs all of the above on every PR (see `.github/workflows/pull-request.yml`).

## Architecture & Modules

Multi-module Gradle project using MVVM, Hilt DI, Room, StateFlow/Flow, Jetpack Compose, and Coroutines.

| Module | Responsibility |
|---|---|
| `app/` | Entry point: `MainActivity`, `KeyMapperApp`, `MyAccessibilityService` |
| `base/` | UI screens and ViewModels (actions, constraints, triggers, keymaps, settings) |
| `common/` | Shared utilities and models |
| `data/` | Room database, repositories, DataStore, migrations |
| `system/` | Device APIs, permission management |
| `sysbridge/` | C++ JNI via CMake for elevated system access |
| `evdev/` | Rust event device handling + JNI bindings |
| `api/` | AIDL public interface definitions |
| `systemstubs/` | Mock stubs for system classes used in tests |

Most business logic lives in `base/src/main/java/io/github/sds100/keymapper/`.

## Code Style

- **Kotlin:** ktlint with android_studio code style (configured via `.editorconfig`)
  - No function expression bodies
  - Trailing commas allowed
  - Run `./gradlew ktlintFormat` before committing
- **Rust:** `cargo fmt`

## Commit Message Convention

```
#<issue_number> <type>: <description>
```

Types: `feat`, `fix`, `chore`, `refactor`, `style`

Example: `#2025 feat: add button to report bug on home screen`

## Branch Naming Convention

- Use `feature/123-branch-name` for new features.
- Use `fix/123-branch-name` for bug fixes.

## Pull Request Requirement

- Any user-facing behavior change, fix, feature, or notable internal change in a PR must include an
  update to `CHANGELOG.md`.
- If there is truly no changelog impact (for example, docs-only or CI-only changes), explicitly state
  that in the PR description.

## Compose Guidelines

- `Modifier` is always the first parameter after required params
- Use `LocalCustomColorsPalette` (not `MaterialTheme.colorScheme`) for non-standard colors
- Check `KeyMapperIcons` before using other icon sources
- Use `LocalUriHandler.openUriSafe` extension for URL launching — do not hoist URL launching logic up the call stack
- Use import statements; never use fully qualified names in Compose code
- Write `@Preview` composables for every screen

## Adding a New Action (10-step checklist)

First determine whether the action is editable. Then:

1. Add a new ID to `ActionId`
2. Create a new `ActionData` sealed class variant
3. Map to/from entity in `ActionDataEntityMapper`
4. Assign a category in `ActionUtils`
5. If editable, add to `isEditable` in `ActionUtils`
6. Add a title string in `strings.xml`
7. Add title and Compose icon in `ActionUtils` (ignore drawables)
8. Add title in `ActionUiHelper`
9. Stub out execution in `PerformActionsUseCase`
10. Handle creation in `CreateActionDelegate`

Do not delete existing action code. Follow existing naming conventions exactly. Add new code near similar existing actions.
