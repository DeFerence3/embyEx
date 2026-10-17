# Kotlin Multiplatform Coding Rules

Reviewed against official web documentation on 2026-10-17. Apply these rules to new and changed code; avoid unrelated rewrites. Repository-specific choices intentionally refine the general Kotlin conventions.

## 1. Repository facts

- Read the nearest `AGENTS.md`, `settings.gradle.kts`, relevant module build files, and `gradle/libs.versions.toml` before editing. Build files are the source of truth for targets and versions.
- Current modules: `:shared`, `:androidApp`, and `:desktopApp`. There is no `:composeApp`, `:shared:di`, or `:ui-design-system` module.
- Shared targets are Android, JVM desktop (`jvm()`), `iosArm64`, and `iosSimulatorArm64`. Web/Wasm is not currently configured; do not claim it is built or tested.
- Shared Kotlin code lives under `shared/src/commonMain/kotlin/app/deference/embycl`. Koin wiring is in `core/di`, theme definitions in `ui/theme`, and reusable UI in `ui/core/components`.
- Maintain Clean Architecture boundaries between UI, domain contracts/models, and data implementations within the existing structure. Organize features coherently; do not create Gradle modules just to satisfy an architecture label.
- Use pinned dependencies and toolchains. Do not upgrade libraries or enable experimental features as a side effect of another task.

## 2. Workflow and protected areas

- Work in small, reviewable checkpoints: UI/state contract, domain contracts/models, then data implementation and integration. Adapt when existing contracts already exist.
- Reuse existing abstractions. Avoid speculative frameworks, generic base classes, and pass-through use cases without a concrete need.
- Preserve unrelated local edits. Do not modify generated sources, build output, local preferences, credentials, or signing files during cleanup.
- Do not alter the native `iosApp` Xcode workspace or native Android configuration without explicit user permission.
- Do not alter the base Ktor engine setup or global serialization configuration.
- Do not write new tests unless the user explicitly requests them. Run existing checks/tests and ask the user to verify specific affected behavior.
- Report changes, verification results, and remaining platform limitations. Never describe failed, skipped, or unavailable checks as passing.

## 3. Kotlin style

Follow [official Kotlin conventions](https://kotlinlang.org/docs/coding-conventions.html), with these repository rules:

- Use four spaces in new files; preserve surrounding indentation in small edits. Avoid file-wide formatting churn.
- Put function/constructor parameters and call arguments on separate lines. Use trailing commas in multiline lists and a separate closing-parenthesis line.
- Name arguments wherever Kotlin permits. Exceptions include Java methods, function-type invocation, positional vararg elements, and operator/infix syntax. Idiomatic trailing lambdas are allowed.
- Put each chained Compose modifier on a separate line; preserve modifier order.
- Put each top-level class, interface, sealed type, enum, and object in its own descriptive file. Companion objects stay inside their owner. Keep related functions together; avoid catch-all utility files.
- Prefer `val`, explicit nullability, small functions, clear expression bodies, and descriptive names. Avoid `!!`, unchecked casts, deeply nested scope functions, wildcard imports, and redundant comments.
- Expose read-only collections/state. A `List` or `val` does not guarantee deep immutability; do not mutate backing data after publication.
- Use data classes for values and sealed types for closed alternatives. Handle sealed alternatives exhaustively instead of hiding new cases with `else`.
- Use the narrowest useful visibility. Document non-obvious contracts, ownership, units, and failure behavior.

## 4. Multiplatform boundaries

- Keep portable logic in `commonMain`; put Java, Android, Apple, and browser APIs in their applicable source sets. Platform-specific extensions of a KMP library are not automatically common APIs.
- Prefer common interfaces with platform implementations supplied through DI. Use small `expect`/`actual` functions, properties, or factories for platform entry points; avoid expected classes when an ordinary interface is sufficient.
- Match expected/actual packages and signatures for every consuming target. Keep shared business logic out of actual implementations. See [expected and actual declarations](https://kotlinlang.org/docs/multiplatform/multiplatform-expect-actual.html).
- Preserve platform filename suffixes, for example `Platform.kt`, `Platform.android.kt`, `Platform.ios.kt`, and `Platform.jvm.kt`. Expect/actual filenames need not be identical; platform suffixes also prevent JVM file-facade clashes.
- Prefer the default [source-set hierarchy](https://kotlinlang.org/docs/multiplatform/multiplatform-hierarchy.html). Inspect custom sharing before editing: `src/jvmAndroidMain/kotlin` is currently included in Android and JVM, not common code.
- Avoid reflection-dependent construction and JVM-only serialization in common code. Use explicit factories, existing Koin bindings, and generated serializers where applicable.
- Unicode text and emojis are not inherently Wasm compilation errors. Keep identifiers readable, use resources for user-facing text, and investigate actual API, dependency, browser, and font support. See [Kotlin strings](https://kotlinlang.org/docs/strings.html) and [Kotlin/Wasm](https://kotlinlang.org/docs/wasm-overview.html).

## 5. Dependencies and Gradle

- Before adding a dependency to `gradle/libs.versions.toml`, verify its exact version and target support through the klibs.io MCP server at `https://api.klibs.io/mcp`, as required by this project. If unavailable, report the verification blocker and defer the addition; never claim verification occurred.
- Confirm published variants and upstream documentation for every consuming source set. Common dependencies must support all active consuming targets; platform-only libraries belong in platform source sets.
- Prefer existing libraries. Centralize new versions/aliases in the catalog, avoid dynamic versions, and use `implementation` unless consumers need an exported API.
- Align the Compose compiler plugin version with Kotlin, not the Compose UI library version. Check the [Compose compatibility guide](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html) and [KMP compatibility guide](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html) before toolchain changes.
- Scope opt-ins narrowly. Do not globally suppress compatibility diagnostics or add experimental flags merely to silence errors.
- Use the wrapper and existing toolchains. Preserve lazy Gradle configuration and source-set dependency boundaries.

## 6. Architecture and dependency injection

- UI renders state and forwards actions. ViewModels coordinate presentation logic. Domain contracts describe behavior; data implementations own transport, storage, and mapping.
- Keep domain contracts independent of Compose, Android contexts, Ktor responses, database handles, and Koin lookup.
- Use constructor injection. Resolve Koin dependencies at application/navigation boundaries, not inside reusable UI or domain logic. Follow existing registration conventions and resource lifetimes.
- Keep transport/persistence details in the data layer when they differ from domain needs. Do not add duplicate models without a useful boundary.
- Represent expected failures consistently with existing project types. Do not turn failures into misleading empty/success results or catch broad exceptions without a recovery policy.
- Never log passwords, tokens, authorization headers, or credential-bearing URLs. Preserve account isolation, persistence migrations, and resource cleanup.

## 7. Coroutines and Flow

- Use structured concurrency: `viewModelScope` for screen work and caller-owned scopes for repository work. Avoid `GlobalScope`, unmanaged scopes, and `runBlocking` on UI paths.
- Make suspend APIs safe to call from the UI. Inject dispatchers when switching context; use `Default` for CPU work and an appropriate supported IO dispatcher for blocking IO. `suspend` alone does not move work off the main thread.
- Do not wrap already asynchronous APIs in arbitrary dispatcher switches. Verify a suitable `Dispatchers.Main` implementation on each runtime, including desktop.
- Propagate cancellation. Rethrow `CancellationException` before handling broader exceptions; `runCatching` also catches cancellation. Keep CPU loops cooperative and clean up resources in `finally`/`use` as appropriate.
- Expose `StateFlow`/`Flow` instead of mutable flows. Use suspend functions for one result and Flow for ongoing observations. Choose sharing scope and replay deliberately.
- Protect compound mutations with a suitable ownership or synchronization policy. Coroutines do not make mutable objects thread-safe. See [coroutine best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices).
- Prefer `MutableStateFlow.update` for atomic transitions. Its lambda may run more than once, so keep it free of side effects. See the [update API contract](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/update.html).

## 8. Compose UI and state

- Use shared `androidx.lifecycle.ViewModel` implementations in `commonMain`. Resolve the ViewModel in the route/screen entry point, then pass `state`, `onAction`, and an existing `eventFlow` only when needed. Never pass a ViewModel into screen content or reusable components.
- Scope ViewModels to the intended navigation entry. Preserve Navigation 3 state and ViewModel decorators; avoid retaining screen ViewModels for the whole application. See [multiplatform ViewModel](https://kotlinlang.org/docs/multiplatform/compose-viewmodel.html).
- Prefer `collectAsStateWithLifecycle()` where supported by the pinned lifecycle artifact; this repository already uses it in common code. Check versions and platform behavior before introducing APIs. See [multiplatform lifecycle](https://kotlinlang.org/docs/multiplatform/compose-lifecycle.html).
- Hoist state to the lowest owner that needs it. Keep local UI state in Compose, screen business state in ViewModels, and durable data in repositories. Use `rememberSaveable` for supported small UI values. See [state hoisting](https://developer.android.com/develop/ui/compose/state-hoisting).
- Keep composable bodies free of IO, navigation, and coroutine launches. Use keyed `LaunchedEffect` for composition-owned work, `DisposableEffect` for subscriptions requiring cleanup, and `rememberUpdatedState` for current callbacks without restarting effects. See [Compose effects](https://developer.android.com/develop/ui/compose/side-effects).
- Model outcomes that must survive collector absence as state, with acknowledgement when necessary. Existing transient event flows need explicit replay/loss behavior and one lifecycle-aware consumer; do not assume exactly-once delivery. See [UI events](https://developer.android.com/topic/architecture/ui-layer/events).
- Split large screens into focused `internal` components under the feature's `components/` directory. Reusable visual components accept `modifier: Modifier = Modifier` as the first optional parameter and apply it to their root.
- Use stable lazy-list item keys. Avoid expensive work during composition; optimize based on observed behavior. Apply `@Stable`/`@Immutable` only when the type satisfies the contract. See [Compose stability](https://developer.android.com/develop/ui/compose/performance/stability/fix).

Illustrative route boundary; content and ViewModel belong in separate files:

```kotlin
@Composable
internal fun SampleRoute() {
    val viewModel: SampleViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SampleScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}
```

## 9. Design system and accessibility

- Reuse `ui/theme` and shared components. Consume semantic colors, typography, shapes, and motion tokens through the theme.
- Do not hardcode hexadecimal colors or introduce raw Material 3 overrides in feature UI. Define missing tokens in the theme and reuse them.
- Use Compose resources for new user-facing strings and accessibility descriptions. Describe meaningful icons; decorative icons use `null`.
- Support text scaling, contrast, keyboard/focus navigation, and available window sizes. Do not infer layout solely from platform names.
- Keep loading, empty, error, retry, and disabled states explicit. Preserve semantics; do not convey state through color alone.

## 10. Build and verification

Use these repository task paths. Commands use POSIX wrapper syntax; on Windows PowerShell use `.\gradlew.bat` with the same arguments.

| Purpose | Command |
| --- | --- |
| Aggregate checks | `./gradlew check` |
| Compile shared JVM code | `./gradlew :shared:compileKotlinJvm` |
| Run common + JVM tests on JVM | `./gradlew :shared:jvmTest` |
| Shared multiplatform test aggregate | `./gradlew :shared:allTests` |
| Build Android debug app | `./gradlew :androidApp:assembleDebug` |
| Android app unit tests | `./gradlew :androidApp:testDebugUnitTest` |
| Shared Android host tests | `./gradlew :shared:testAndroidHostTest` |
| Desktop application tests | `./gradlew :desktopApp:test` |
| Link iOS simulator framework on macOS | `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` |

- Run local compilation and existing multiplatform tests before declaring completion. Use the relevant platform build for changed code and `:shared:allTests` for shared changes; run `check` for final integration verification when supported.
- `check` is an aggregate of registered checks, not proof every platform binary was built. Framework linking verifies compilation/linking, not test execution.
- Use an appropriate macOS/Xcode host for iOS framework linking and simulator execution. An `allTests` success on Windows does not prove iOS runtime coverage. Report disabled/skipped targets and request macOS verification when affected.
- If task names change, inspect `./gradlew :shared:tasks --all` and the relevant module's task report. Do not revive obsolete `:composeApp:*` or `:shared:desktopTest` commands.
- Report exact toolchain, signing, dependency, or compilation blockers. Do not weaken checks, modify protected configuration, or invent credentials to make them pass.
- Ask the user to verify affected flows with specific steps and expected outcomes, including relevant navigation, lifecycle, offline/error, and platform behavior. Do not create tests just to satisfy this checklist.

## 11. Keeping these rules current

Recheck linked official documentation before version-sensitive changes. Documentation snippets are examples, not instructions to copy their dependency versions. Record reasons for repository-specific exceptions and keep this file aligned with actual targets and paths.
