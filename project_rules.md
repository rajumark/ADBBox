# ADBStudio — Project Rules & Architecture

> Rules for a 1000+ command, 400+ screen Compose Desktop ADB tool. M3 UI rules are in §14.

---

## 1. Tech Stack

| Layer | Choice | Why |
|-------|--------|-----|
| DI | **Koin** (module-per-feature) | KMP-native, compile-safe with KSP, low boilerplate |
| Architecture | **MVVM + Clean Architecture** | Separates UI, domain, data. Scales to 400+ screens |
| State | **StateFlow** + **immutable data classes** | Thread-safe, compose-friendly, testable |
| Async | **kotlinx.coroutines** | Structured concurrency, cancellation propagation |
| Serialization | **kotlinx.serialization** | Workspace persistence, config, command params |
| Navigation | **State-driven sealed class** + back stack | No 3rd-party dep. Decoupled from UI layer |
| ADB Process | **ProcessBuilder** + coroutine wrappers | Directly in `jvmMain` (no `expect/actual`) |
| Testing | **kotlin.test** + **Turbine** (Flow test) | KMP-native, Flow assertion |

---

## 2. Module Structure

```
shared/src/commonMain/kotlin/com/adbstudio/
├── core/                          # Shared infrastructure (zero UI)
│   ├── di/                        # Koin module definitions
│   ├── error/                     # Standardized error model
│   ├── result/                    # Result<T, AppError> wrapper
│   ├── logging/                   # Logging abstraction
│   ├── coroutines/                # Dispatchers, supervised scope
│   └── persistence/               # Settings, workspace save/load
│
├── adb/                           # ADB command layer (no UI)
│   ├── model/                     # Typed command hierarchy (1000+)
│   │   ├── base/                  # AdbCommand sealed interface
│   │   ├── device/                # device-*, get-serialno, etc.
│   │   ├── package/               # pm list packages, install, etc.
│   │   ├── logcat/                # logcat commands
│   │   ├── shell/                 # Generic shell commands
│   │   ├── wireless/              # Pair, connect, tcpip
│   │   └── ...                    # One file per command category
│   ├── parser/                    # Output parsers (one per command)
│   ├── execution/                 # Command runner, timeout, retry
│   └── connection/                # Device discovery, state tracking
│
├── feature/                       # Feature modules (one per screen group)
│   ├── device-list/               # Feature example
│   │   ├── data/                  # Repository impl, adb source
│   │   ├── domain/                # Use cases, business logic
│   │   ├── presentation/          # ViewModel + UIState
│   │   └── ui/                    # Composables (screens + components)
│   ├── package-manager/
│   ├── logcat/
│   ├── file-explorer/
│   ├── screen-capture/
│   ├── wireless/
│   ├── performance/
│   └── ...                        # ~100+ features matching 400+ screens
│
├── navigation/                    # Navigation graph, back stack
├── ui/                            # Shared UI (only M3 components)
│   ├── component/                 # Reusable composables (shell, dialogs)
│   ├── screen/                    # Shared/utility screens
│   └── layout/                    # App shell, scaffold, menus
└── theme/                         # M3 theme (already RULES.md strict)
```

**Rules:**
- `core/` and `adb/` must have **zero Compose imports** — pure Kotlin.
- Each feature is a **self-contained logical module** (data → domain → presentation → ui) within a **physical Gradle module** grouping related features.
- **10–15 physical Gradle modules max** — grouping by domain (`:feature:device`, `:feature:packages`, `:feature:logcat`). Beyond that, Gradle config time and IDE indexing degrade sharply.
- Features communicate only through **core interfaces** or **events** — never direct imports.
- New command = new file in `adb/model/` + new parser in `adb/parser/`.

---

## 3. Typed Command System (The Core)

Every ADB command is a typed, self-describing object — never raw strings.

```kotlin
// adb/model/base/AdbCommand.kt
sealed interface AdbCommand<out T> {
    val serial: String?        // target device serial (null = first connected)
    val timeoutMs: Long        // per-command timeout
    fun toCliArgs(): List<String>   // e.g., ["-s", "1234", "shell", "dumpsys", "battery"]
}

sealed interface AdbOutput<out T> {
    data class Success<T>(val value: T) : AdbOutput<T>
    data class Error(val reason: AdbError) : AdbOutput<Nothing>
}
```

**Rules:**
- Every command defines its own **input type** (parameters) and **output type** (parsed result).
- One parser per command — parser is a pure function `(String) -> AdbOutput<T>`.
- Commands never call ADB directly — they go through `AdbExecutor` (single point of execution).
- `AdbExecutor` handles: timeout enforcement, cancellation, retry, output capture.
- All serial communication is in `jvmMain` via `ProcessBuilder` directly (no `expect/actual` — this is JVM-only).

**For 1000+ commands:**
- Group commands by ADB category (`device`, `package`, `logcat`, `dumpsys`, `settings`, `content`, `input`, `wm`, `am`, `pm`, `svc`, etc.).
- Use code generation (KSP) or convention over configuration to reduce boilerplate.
- Each command category gets a factory/sealed hierarchy.

**Parser Strategy Pattern (for Android API variance):**
- ADB output changes unpredictably across Android 9→16+. A single parser with `if/else` per API level is unmaintainable.
- Implement versioned parsers selected by target device API level:

```kotlin
interface AdbParser<T> {
    fun parse(output: String): AdbOutput<T>
}

class BatteryParserV21 : AdbParser<BatteryInfo>
class BatteryParserV34 : AdbParser<BatteryInfo>

// Factory selects parser based on target device API level
object ParserFactory {
    fun <T> parserFor(command: AdbCommand<T>, apiLevel: Int): AdbParser<T>
}
```

---

## 4. State Management (UDF)

```
User Action → ViewModel.onEvent(event) → UseCase.run() → AdbExecutor.run(command) 
    → Result → ViewModel._state.update { } → Compose collects StateFlow → UI renders
```

**Rules:**
- Every screen has a single `UiState` data class (immutable).
- One `ViewModel` per screen, exposing `StateFlow<UiState>`.
- UI sends events via sealed class — no mutable state in composables.
- ViewModels use `viewModelScope` from lifecycle — auto-cancels on dispose.

**Split state for high-frequency screens:**
- On high-throughput screens (logcat at 60Hz, real-time perf monitors), a single `UiState` class triggers full-screen recomposition on every update.
- Split into **static config** + **high-frequency stream**:

```kotlin
// In LogcatViewModel
val uiState: StateFlow<LogcatUiState>      // filters, pause, selection
val logLines: Flow<List<LogLine>>           // high-frequency stream, collected via LazyColumn directly
```

**`mutableStateOf` nuance (Compose Desktop):**
- `mutableStateOf` writes directly to the Compose snapshot system — faster than `StateFlow` + `collectAsState()`.
- Allowed in ViewModels **if** mutations are confined to `Dispatchers.Main.immediate`.
- For background/asynchronous state generation, use `MutableStateFlow` and update safely.
- In composables: OK for trivial UI-local state (text input, scroll pos, expanded sections).

```kotlin
// Feature pattern
data class DeviceListState(
    val devices: List<DeviceInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val selectedDevice: DeviceInfo? = null,
)

sealed interface DeviceListEvent {
    data object Refresh : DeviceListEvent
    data class Select(val device: DeviceInfo) : DeviceListEvent
    data class Disconnect(val serial: String) : DeviceListEvent
}
```

---

## 5. ADB Safety Rules

| Rule | Detail |
|------|--------|
| **Timeout every command** | No ADB command runs without a timeout. Default 15s, override per command. |
| **Never block main thread** | ADB execution always on `Dispatchers.IO`, `withTimeout()` wrapper. |
| **Always expect failure** | ADB hangs, devices disconnect, USB errors. Every call must handle `AdbOutput.Error`. |
| **Structured cancellation** | Use `currentCoroutineContext().ensureActive()` in long ADB operations. |
| **Rate-limit logcat** | Logcat output is unbounded. Use `channelFlow` with buffer, debounce, or backpressure. |
| **Logcat memory cap** | Hard limit of 10,000 lines in buffer. Drop oldest entries to prevent OOM. Configurable. |
| **Zombie process prevention** | `AdbExecutor` must call `process.destroyForcibly()` inside `invokeOnCancellation`. ADB processes don't auto-die when coroutines cancel. |
| **Command idempotency** | Debounce destructive/heavy commands (`reboot`, `install`, `wipe`). Track ongoing executions per serial in `AdbExecutor` — reject duplicates. |
| **Parse defensively** | ADB output format changes across Android versions. Never assume format stability. |
| **Escape shell args** | User-provided input in shell commands must be escaped. Use `CommandEscaper.escape(arg)`. |
| **Wireless is unreliable** | Wireless ADB retry logic with exponential backoff. Detect disconnection by polling. |
| **Multi-device isolation** | State per `DeviceSerial`. Switching devices cancels background jobs (logcat, monitoring) for the old serial. `AdbExecutor` routes commands by serial. |

---

## 6. Error Model (Standardized)

```kotlin
// core/error/AppError.kt
sealed interface AppError {
    data class AdbError(val command: String, val exitCode: Int, val stderr: String) : AppError
    data class AdbTimeout(val command: String, val timeoutMs: Long) : AppError
    data class DeviceDisconnected(val serial: String) : AppError
    data class ParseError(val rawOutput: String, val parser: String) : AppError
    data class NetworkError(val cause: Throwable) : AppError
    data class Unknown(val cause: Throwable) : AppError
}
```

**Rules:**
- Every layer returns `Result<T, AppError>` or a typed `AdbOutput<T>`.
- UI maps errors to user-facing messages via a `ErrorUiMapper`.
- `AppError` is serializable for crash logs.
- Unhandled errors are captured by a global `ErrorHandler` (logging + optional dialog).

---

## 7. Navigation System

```
FeatureNavigationProvider → navStack: List<AppScreen> → current: AppScreen
```

**For 400+ screens — avoid single sealed class bottleneck:**
- A single `sealed class Screen` with 400+ variants creates a massive file that every feature depends on (merge-conflict magnet, breaks encapsulation).
- Use **per-feature sealed classes** implementing a marker interface:

```kotlin
// In core/navigation
interface AppScreen

// In feature/package-manager
sealed class PackageManagerScreen : AppScreen {
    data class List(val serial: String) : PackageManagerScreen()
    data class Detail(val serial: String, val packageName: String) : PackageManagerScreen()
}

// In feature/logcat
sealed class LogcatScreen : AppScreen {
    data class Viewer(val serial: String, val filter: String = "") : LogcatScreen()
}
```

- Each feature exports a `FeatureNavigationProvider` that registers destinations into a central map at startup: `Map<String, (Args) -> AppScreen>`.
- Command palette (Ctrl+P) discovers all screens via this registry.
- Back stack stored as `List<AppScreen>` in a `NavigationViewModel`.
- Navigation is pure state machine — no 3rd-party libs.

---

## 8. Performance & Memory

| Concern | Practice |
|---------|----------|
| **LazyColumn** | Every list uses `LazyColumn` + `key` param. No `Column` wrapping `for` loops. |
| **Logcat** | Use `LazyColumn` with `reverseLayout = true`, max lines buffer, pagination or windowed view. |
| **State explosion** | Use `derivedStateOf` for computed states. For high-frequency screens, split state into static `UiState` + separate `Flow<T>` for streaming data (see §4). |
| **ProcessBuilder** | Cache and reuse `AdbExecutor` instance. Avoid creating process builders per command. |
| **Search** | Debounce at 300ms. Cancel previous search coroutine. Use `Flow.debounce()`. |
| **File explorer** | Virtualized tree — only render visible nodes. Load directory contents lazily. |
| **Command results** | Use `Flow` with `distinctUntilChanged()` — don't re-render on identical data. |
| **Compose desktop leaks** | Never capture Compose `Local` in coroutines. Clear `remember` objects on disposal. Use `DisposableEffect` for cleanup. |
| **Zombie processes** | `AdbExecutor` calls `process.destroyForcibly()` in `invokeOnCancellation`. Coroutine cancellation ≠ OS process death. |
| **Logcat OOM** | Hard cap of 10,000 lines in buffer. Drop oldest. Implement as ring buffer or evicting queue. |

---

## 9. DI Architecture (Koin)

```kotlin
// Each feature module has its own Koin module
val deviceListModule = module {
    factory { DeviceListViewModel(get(), get()) }
    factory { GetDevicesUseCase(get()) }
    single { DeviceRepository(get()) }
}

// Root composition
val sharedModules = listOf(
    coreModule,           // dispatchers, error handler, logger
    adbModule,            // AdbExecutor, command registry
    deviceListModule,
    packageManagerModule,
    logcatModule,
    // one per feature
)
```

**Rules:**
- `single` for stateless services (executors, repositories).
- `factory` for ViewModels (new instance per screen).
- No `CompositionLocal` for business logic — only for UI-level ambient state (theme).
- Modules are composed at application entry point, not scattered.

---

## 10. Testing Strategy

| Layer | What to Test | Tool |
|-------|-------------|------|
| **Commands** | toCliArgs() output, parameter validation | kotlin.test |
| **Parsers** | Parse known ADB output strings, malformed input, empty input | kotlin.test |
| **Use Cases** | Business logic with mocked repository | kotlin.test + Turbine |
| **ViewModels** | State transitions given events, error scenarios | kotlin.test + Turbine |
| **UI** | Screen renders without crash (smoke test) | Compose UI Test (skiko) |

**Rules:**
- Parser tests are **most valuable** — write them first for every command.
- ViewModel tests use `TestScope` + `UnconfinedTestDispatcher`.
- Mock ADB responses at the executor level (never mock ProcessBuilder).
- Run: `./gradlew :shared:jvmTest`

---

## 11. Development Workflow (AI-Assisted)

| Principle | Practice |
|-----------|----------|
| **Define first, implement later** | Write `AdbCommand` + parser test + `UiState` before any composable. |
| **One command → one PR** | Each command is: model → parser → use case → vm → screen. |
| **Always leave tests** | AI must generate tests for every new parser/command. |
| **Follow existing pattern** | For a new feature, copy the closest existing feature folder structure. |
| **Keep files small** | Max 200 lines per file. Split large features into multiple files. |
| **No experimental APIs** | Only stable Compose/Kotlin APIs. No `@ExperimentalMaterial3Api` unless unavoidable. |

---

## 12. Scalability Rules (for 1000+ commands)

1. **Command registry** — all 1000+ commands registered at startup in a `Map<String, AdbCommandFactory>`. Enables command palette, search, discovery.
2. **Lazy feature loading** — features are loaded on navigation, not at startup. Use Koin `lazy` or `getLazy()`.
3. **Consistent naming** — each command file matches its ADB command name: `PackageList.kt`, `DumpsysBattery.kt`, `SettingsGet.kt`.
4. **Output schema versioning** — ADB output changes per Android API level. Use **Strategy Pattern** (see §3): versioned parsers selected by a factory based on target device API level.
5. **Plugin-like addition** — adding a new command =: (a) create model file, (b) create parser, (c) register in command group module. No other file changes.
6. **Command idempotency** — `AdbExecutor` tracks ongoing command executions per `(serial, commandKey)`. Rejects or queues duplicate destructive commands. Debounce rapid user clicks (300ms window).

---

## 13. What To Avoid

| Anti-pattern | Instead |
|-------------|---------|
| `mutableStateOf` mutated off main thread | Use on `Dispatchers.Main`; use `StateFlow` for background-originated state |
| `runBlocking` anywhere | `withContext` + structured concurrency |
| Raw `ProcessBuilder` calls in commonMain | Put directly in `jvmMain` source set (no `expect/actual`) |
| `GlobalScope` | `viewModelScope`, custom `SupervisorJob` |
| Nested screens in one file | One file per screen |
| Business logic in `@Composable` | ViewModel + UseCase |
| Custom ADB path stored in multiple places | Single `AdbConfig` in core module |
| Hardcoded strings for ADB commands | Typed `AdbCommand.toCliArgs()` |

---

## 14. UI Rules — Material Design 3 (Strict Mode)

### Philosophy
Pure Google M3 with **zero custom UI**. Every component, color, type scale uses M3 defaults via `androidx.compose.material3`. No custom drawables, no overridden colors, no custom text sizes, no custom shapes. If M3 doesn't provide it out of the box, we don't add it.

### 14.1 Components
- Use **only** `androidx.compose.material3.*` components.
- No custom composables that wrap or re-style M3 components.
- No custom `Button`, `TextField`, `Card`, `NavigationBar`, `TopAppBar`, `AlertDialog`, `ModalBottomSheet`, etc.
- Each M3 component uses **default parameters only** — no overriding `colors`, `shape`, `typography`, or `elevation`.
- Exception: functional parameters like `onClick`, `enabled`, `expanded`, `onDismiss` are allowed.

### 14.2 Color
- **Never** define custom `Color` values. Use only `MaterialTheme.colorScheme.*`.
- Never pass custom colors to `Modifier.background()` or `Modifier.foreground()`.
- Never override `colorScheme` in `MaterialTheme`.

### 14.3 Typography
- **Never** define custom `TextStyle` or font sizes.
- Use only `MaterialTheme.typography.*` styles (`bodyLarge`, `titleMedium`, `labelSmall`, etc.).
- `Text` must use `style = MaterialTheme.typography.*` — no inline `fontSize`, `fontWeight`, or `lineHeight`.
- Never override `typography` in `MaterialTheme`.

### 14.4 Spacing / Layout
- Use only M3 reference sizes (`4.dp`, `8.dp`, `12.dp`, `16.dp`, `24.dp`).
- No hardcoded custom `dp` values for margins/paddings.
- Prefer `Modifier.sizeIn()`, `widthIn()`, `heightIn()` over fixed sizes.

### 14.5 Icons
- Use only `androidx.compose.material.icons.Icons.Default.*` or `material-icons-extended`.
- No custom SVG/vector drawables. Icons must have `contentDescription` for accessibility.

### 14.6 Shape
- Never define custom `Shape` or `RoundedCornerShape`. Use only `MaterialTheme.shapes.*` (`small`, `medium`, `large`, `extraLarge`).

### 14.7 Window
- Default Compose Desktop window chrome (no custom title bar). Window size: `WindowState` with `DpSize(1200.dp, 800.dp)`.

### 14.8 Naming
- **Files**: `PascalCase.kt` matching primary class/composable.
- **Composables**: PascalCase, noun-based (`DeviceList`, `DeviceCard`).
- **Functions**: camelCase, verb-based (`connectToDevice`, `fetchLogs`).
- **ViewModels**: `*ViewModel.kt`, expose `StateFlow` / `MutableStateFlow`.
- **Screens**: `*Screen.kt` — full-page view composable.

### 14.9 UX Rules
- Every screen uses `Scaffold` with a `TopAppBar` and optional `NavigationBar`.
- Navigation: state-driven (no 3rd-party nav libs).
- Dialogs: M3 `AlertDialog` only — no custom dialog composables.
- Loading: M3 `LinearProgressIndicator` or `CircularProgressIndicator`.
- Empty states: `Text` with `bodyLarge` style and brief message.
- Error states: M3 `AlertDialog` or inline `Text(style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)`.
- Confirm destructive actions with M3 `AlertDialog`.

### 14.10 Code Conventions
- No HTML-like comments (`//` or `/* */` only where necessary).
- No `@Preview` in production code.
- `remember` / `derivedStateOf` for local state; `collectAsState()` for ViewModel state.
- `LaunchedEffect` for side effects (ADB commands, timers).
- `withContext(Dispatchers.IO)` for blocking ADB operations.
- Explicit M3 imports — no wildcard imports except `compose.material3.*`.

### 14.11 ADB-Specific Conventions
- All ADB shell commands through a single abstraction in `adb/`.
- Device connection state via `StateFlow` in `DeviceViewModel`.
- Logcat in M3 `LazyColumn` with `Text(style = MaterialTheme.typography.bodySmall)`.
- Device list uses M3 `LazyColumn` with `ListItem` or `Card`.

### 14.12 What We Do NOT Do

| Disallowed | Instead Use |
|---|---|
| Custom-colored buttons | `Button(colors = ButtonDefaults.buttonColors())` (default) |
| Custom font sizes | `MaterialTheme.typography.*` |
| Custom shapes | `MaterialTheme.shapes.*` |
| Custom color palettes | `MaterialTheme.colorScheme.*` |
| Custom text styles | `MaterialTheme.typography.*` |
| Custom UI components | Built-in M3 composables only |
| Third-party UI libraries | Not allowed |
| Custom window chrome | Default OS window chrome |
| Hardcoded colors | `Color.Unspecified` or `MaterialTheme.colorScheme.*` |
