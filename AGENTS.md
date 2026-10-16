# AI Coding Guardrails & Project Rules

## 1. Project Overview
This is a **Kotlin Multiplatform (KMP)** project targeting [Android / iOS / Desktop / WASM].
- **UI Framework:** Compose Multiplatform for shared UI.
- **Architecture:** Clean Architecture with Feature-by-Module separation.
- **Dependency Injection:** Koin (configured in `:shared:di`).

## 2. Critical KMP Compilation & Test Commands
AI agents must use these exact commands to verify changes. Do not guess platform targets.

- **Check Everything:** `./gradlew check`
- **Build Android:** `./gradlew :composeApp:assembleDebug`
- **Test Shared Common Logic:** `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` or `./gradlew :shared:desktopTest`
- **Run Android Tests:** `./gradlew :composeApp:testDebugUnitTest`
- **Run Shared Multiplatform Tests:** `./gradlew :shared:allTests`

*Note to Agent: Always run local compilation and multiplatform tests before declaring a task complete.*

## 3. Kotlin Multiplatform Architecture Invariants
- **Platform-Specific Code:** Always use the `expect`/`actual` pattern for platform-specific APIs. Place declarations in `commonMain` and implementations in respective directories (e.g., `androidMain`, `iosMain`).
- **File Naming:** For `actual` files, do not omit or change the file name structure. Ensure they align perfectly with the `expect` declarations.
- **Concurrency & Coroutines:** Always utilize thread-safe patterns. Avoid platform-specific threading hooks; rely on `Dispatchers.Main` and `Dispatchers.Default`.
- **WASM Constraints:** Avoid using emojis, UTF-8 non-standard characters, or reflection-heavy operations in `commonMain` as they break WebAssembly compilation.

## 4. Third-Party Library Selection Rule
- **Verified KMP Dependencies Only:** Before adding any new dependency to `libs.versions.toml`, you MUST verify its multiplatform compatibility via the klibs.io MCP server (`https://api.klibs.io/mcp`).
- Ensure the target library supports all active targets in this project. Do not inject pure JVM/Android dependencies into `commonMain`.

## 5. UI & State Invariants
- **State Management:** Use shared ViewModels extending `androidx.lifecycle.ViewModel` in `commonMain`.
- **Lifecycle UI:** Check active JetBrains Compose versions before applying lifecycle collectors like `collectAsStateWithLifecycle()` to prevent compilation mismatches.
- **Design System:** All components must inherit tokens from the custom theme in `:ui-design-system`. Hardcoded hexadecimal values or raw Material 3 overrides are strictly forbidden.

## 6. Prohibited Actions
- **No Manual iOS/Android Project Alterations:** Do not modify the native `/iosApp` Xcode workspace or native Android configurations without explicit human permission.
- **No Network Sync Overwrites:** Do not alter the base `Ktor` engine setup or global serialization configurations.

## 7. Coding Guidelines
- Always put arguments on separate lines when passing.
- Always put arguments on separate lines in classes
- Always name arguments when calling functions and creating classes
- Always put chained modifiers in separate lines
- Don't pass viewmodel to any screen content construct viewmodel outside screen content and pass event, onAction and state paramters : eg 
```kotlin
@Serializable
data object SampleScreen : Screen {
    @Composable 
    override fun Content() { 
		val vm: SampleVM = koinViewModel() 
        val state by vm.state.collectAsState() 
        val onAction = vm::onAction 
        val eventFlow = vm.eventFlow
        SampleScreenContent(
			state = state,
			onAction = onAction,
			eventFlow = eventFlow
		)
    }
}
```
- always put each Classes, interfaces, sealed classes, and objects on a seperate files
- Don't implement large screens in a single file. create components and put it in `/components` sub dir with `internal` modifier
- Don't implement a functionality in full single stretch. implement component by component preferably, ui → domain (repository and models) → data (repository impl) and do check point in each sprint
- Don't write tests. always ask user to verify and test, clearly state what should be tested