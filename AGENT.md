# Android Development Guide

## Build Commands
- Build: `./gradlew build` or `./gradlew assembleDebug`
- Test (unit): `./gradlew test` or `./gradlew testDebugUnitTest`
- Test (instrumented): `./gradlew connectedAndroidTest`
- Single test class: `./gradlew test --tests "ExampleUnitTest"`
- Single test method: `./gradlew test --tests "ExampleUnitTest.addition_isCorrect"`
- Lint: `./gradlew lint`
- Clean: `./gradlew clean`

## Project Structure
- Standard Android project with Kotlin + Jetpack Compose
- Package: `com.example.myapplication`
- Min SDK: 30, Target SDK: 36, Compile SDK: 36
- Main activity: `MainActivity.kt` with Compose UI
- Tests: Unit tests in `src/test/`, instrumented tests in `src/androidTest/`
- Dependencies managed via `gradle/libs.versions.toml` version catalog

## Code Style
- Kotlin with JVM target 11
- Use Jetpack Compose for UI (Material3)
- Follow Android naming conventions: PascalCase for classes, camelCase for functions/variables
- Package structure: `com.example.myapplication.{feature}`
- Compose functions use `@Composable` annotation and PascalCase naming
- Use `androidx` libraries, avoid deprecated Android Support Library
