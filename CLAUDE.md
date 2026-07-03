# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Physi-Lock (`com.prototype.physi_lock`) is an Android app built with Kotlin and Jetpack Compose. The project is currently a fresh Android Studio template scaffold — a single `MainActivity` with a placeholder `Greeting` composable — with no application-specific logic implemented yet.

## Commands

Use the Gradle wrapper (`gradlew.bat` on Windows, via PowerShell) for all build tasks — there is no separate CLI/package manager involved.

- Build debug APK: `.\gradlew.bat assembleDebug`
- Install debug build to a connected device/emulator: `.\gradlew.bat installDebug`
- Run unit tests (JVM, `app/src/test`): `.\gradlew.bat test`
- Run a single unit test class: `.\gradlew.bat test --tests "com.prototype.physi_lock.ExampleUnitTest"`
- Run instrumented tests (device/emulator required, `app/src/androidTest`): `.\gradlew.bat connectedAndroidTest`
- Lint: `.\gradlew.bat lint`
- Clean: `.\gradlew.bat clean`

## Architecture

- **Module layout**: single `:app` module (see `settings.gradle.kts`); no multi-module split exists yet.
- **UI**: Jetpack Compose only (no XML layouts, `buildFeatures.compose = true` in `app/build.gradle.kts`). Theming lives under `app/src/main/java/com/prototype/physi_lock/ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`) following the standard Compose Material3 theme scaffold (`PhysiLockTheme`).
- **Entry point**: `MainActivity.kt` uses `ComponentActivity` + `setContent` + `enableEdgeToEdge()`, wrapping content in `PhysiLockTheme` and a Material3 `Scaffold`.
- **Dependency versions**: managed centrally via the Gradle version catalog at `gradle/libs.versions.toml` (referenced as `libs.*` in `app/build.gradle.kts`). Add new dependencies there rather than hardcoding versions in the module build file.
- **SDK levels**: `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`.
- **Java/Kotlin compatibility**: Java 11 (`sourceCompatibility`/`targetCompatibility`), Kotlin `2.2.10`.
