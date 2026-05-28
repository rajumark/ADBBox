# ADBStudio — Project Rules & Architecture

> Rules for a 1000+ command, 400+ screen Compose Desktop ADB tool. M3 UI rules in §20.

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

// Streaming commands (logcat, top, screenrecord, tcpdump) — not request/response
interface StreamingAdbCommand<T> : AdbCommand<T> {
    fun stream(): Flow<T>
}
```

**Command Metadata** (powers palette, docs, warnings, filtering):
```kotlin
data class CommandMetadata(
    val id: String,
    val category: CommandCategory,
    val dangerous: Boolean = false,
    val requiresRoot: Boolean = false,
    val minApi: Int? = null,
    val supportsStreaming: Boolean = false,
)
```

**Rules:**
- Every command defines its own **input type** (parameters) and **output type** (parsed result).
- One parser per command — parser is a pure function `(String) -> AdbOutput<T>`.
- Each command carries a `CommandMetadata` — registered at startup in a `Map<String, CommandMetadata>`.
- Commands never call ADB directly — they go through `AdbExecutor` (single point of execution).
- `AdbExecutor` handles: timeout enforcement, cancellation, retry, output capture.
- Streaming commands use `callbackFlow` or `channelFlow` with backpressure — never unbounded emission.
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
- **Feature-scoped ViewModels** — not one-per-screen. For tooling apps with tabs/docking, `LogcatWorkspaceViewModel` outlives individual logcat tabs. This avoids explosion when screens become reusable panels.
- ViewModels expose `StateFlow<UiState>` (or `mutableStateOf` on main thread, see nuance below).
- UI sends events via sealed class — no mutable state in composables.
- ViewModels use `viewModelScope` from lifecycle — auto-cancels on dispose.

**Event Bus (cross-feature communication at scale):**
- At 400+ screens, direct feature-to-feature imports create coupling.
- Use a shared `EventBus` for app-wide events:

```kotlin
sealed interface AppEvent {
    data class DeviceConnected(val device: DeviceInfo) : AppEvent
    data class DeviceDisconnected(val serial: String) : AppEvent
    data object AdbPathChanged : AppEvent
}

interface EventBus {
    suspend fun publish(event: AppEvent)
    fun events(): Flow<AppEvent>
}
```

- Features subscribe to relevant events — never import each other's ViewModels.

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
- At window scope (top-level state): OK — see §17 for desktop window state pattern.

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

## 6. Background Task Manager

ADB operations like install, pull, screenrecord, bugreport are long-running. Without a central manager, cancellation, progress, and lifecycle leak everywhere.

```kotlin
sealed interface TaskState {
    data object Queued : TaskState
    data object Running : TaskState
    data class Progress(val percent: Float, val message: String = "") : TaskState
    data object Completed : TaskState
    data class Failed(val error: AppError) : TaskState
}

interface TaskManager {
    suspend fun submit(task: AdbTask): TaskHandle
    fun activeTasks(serial: String?): Flow<List<TaskHandle>>
    fun observe(taskId: String): StateFlow<TaskState>
}

interface TaskHandle {
    val id: String
    val description: String
    val state: StateFlow<TaskState>
    fun cancel()
}
```

**Rules:**
- Every long-running ADB operation (`install`, `pull`, `push`, `screenrecord`, `bugreport`) goes through `TaskManager`.
- Tasks auto-cancel when the target device disconnects.
- UI shows task progress in a status bar or notification center.
- `TaskManager` caps concurrent tasks per device (default 3).

---

## 7. Error Model (Standardized)

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

## 8. Navigation System

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
- **Workspace/docking note:** A nav stack alone won't support split panes, tab groups, or floating inspectors. Design `Screen` as a reusable `WorkspaceNode` that can live in tabs, splits, or panels. Layout = tree of `WorkspaceNode` (Split | Tabs | Panel). Don't bake single-screen assumptions into the navigation model.

---

## 9. Device Session & Capability System

ADB queries are expensive. Repeated `getprop`, `dumpsys`, and `pm` calls tank performance. Cache device state in a session object:

```kotlin
class DeviceSession(
    val serial: String,
    val apiLevel: Int,
    val features: Set<DeviceCapability>,
    val connectionType: ConnectionType,
    val cachedProperties: Map<String, String>,
    val activeJobs: List<TaskHandle>,
) {
    fun supports(capability: DeviceCapability): Boolean
}

sealed interface DeviceCapability {
    data object WirelessDebugging : DeviceCapability
    data object IncrementalInstall : DeviceCapability
    data object ScreenRecording : DeviceCapability
    data object RootAccess : DeviceCapability
}
```

**Rules:**
- `DeviceSession` is created on connection, invalidated on disconnect.
- Capabilities replace `if (apiLevel >= 34)` scattered across features.
- Session caches expensive ADB results (package list, battery info, props) with TTL.
- `DeviceRepository` manages session lifecycle — features request sessions by serial.

---

## 10. Performance & Memory

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

## 11. DI Architecture (Koin)

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
    // one per feature domain
)
```

**Rules:**
- `single` for stateless services (executors, repositories, session caches).
- `single` for ViewModels — feature-scoped VMs outlive individual screen composables in desktop docking/tab environments. See §15 for lifecycle pattern.
- `factory` only for truly transient objects (one-shot dialogs, ephemeral helpers).
- No `CompositionLocal` for business logic — only for UI-level ambient state (theme).
- Modules are composed at application entry point, not scattered.

---

## 12. Testing Strategy

| Layer | What to Test | Tool |
|-------|-------------|------|
| **Commands** | toCliArgs() output, parameter validation | kotlin.test |
| **Parsers** | Parse known ADB output strings, malformed input, empty input | kotlin.test |
| **Use Cases** | Business logic with mocked repository | kotlin.test + Turbine |
| **ViewModels** | State transitions given events, error scenarios | kotlin.test + Turbine |
| **Parsers (API matrix)** | Test against output samples from Android 9, 11, 14, 16 | kotlin.test |
| **UI** | Smoke test screens render without crash | Compose UI Test (skiko) |

**Rules:**
- Parser tests are **most valuable** — write them first for every command.
- ViewModel tests use `TestScope` + `UnconfinedTestDispatcher`.
- Mock ADB responses at the executor level (never mock ProcessBuilder).
- Run: `./gradlew :shared:jvmTest`

---

## 13. Development Workflow (AI-Assisted)

| Principle | Practice |
|-----------|----------|
| **Define first, implement later** | Write `AdbCommand` + parser test + `UiState` before any composable. |
| **One command → one unit** | Each command is: model → parser → use case → vm → screen. |
| **Always leave tests** | AI must generate tests for every new parser/command. |
| **Follow existing pattern** | For a new feature, copy the closest existing feature folder structure. |
| **Split by responsibility** | No arbitrary line limit — split files when they do more than one thing. |
| **No experimental APIs** | Only stable Compose/Kotlin APIs. No `@ExperimentalMaterial3Api` unless unavoidable. |

**Structured logging (add now, pay off later):**
```kotlin
logger.info("adb_command_executed", mapOf(
    "serial" to serial, "command" to command.id,
    "durationMs" to duration, "success" to true,
))
```
Use correlation IDs per operation. Enables debugging, telemetry, crash analysis, AI assistance.

**AI integration boundary (future-proof):**
```kotlin
interface AiTool {
    val id: String
    suspend fun execute(input: JsonObject): JsonObject
}
```
Expose ADB commands, device queries, logcat search, screenshots as `AiTool` implementations. Local AI agents interact through this boundary — safe, typed, auditable.

---

## 14. Scalability Rules (for 1000+ commands)

1. **Command metadata registry** — all commands registered at startup with `CommandMetadata` (see §3). Powers command palette, docs, warnings, filtering, AI tooling.
2. **Lazy feature loading** — features loaded on navigation, not at startup. Koin `lazy` or `getLazy()`.
3. **Consistent naming** — each command file matches its ADB command: `PackageList.kt`, `DumpsysBattery.kt`, `SettingsGet.kt`.
4. **Output schema versioning** — ADB output changes per API level. Strategy Pattern (§3) with versioned parsers selected by target device API level.
5. **Plugin-like addition** — new command =: (a) model file, (b) parser, (c) register in command group. No other file changes.
6. **Command idempotency** — `AdbExecutor` tracks ongoing executions per `(serial, commandKey)`. Rejects duplicates. Debounce rapid clicks (300ms).

**Internal plugin architecture:**
```kotlin
interface AdbStudioFeature {
    val id: String
    val routes: List<AppScreen>
    val commands: List<CommandMetadata>
    val koinModules: List<Module>
}
```
Structure every feature as if it were a plugin. This enables feature isolation, lazy loading, optional modules, and enterprise/internal extensions without refactoring.

---

## 15. ViewModel Lifecycle & Scope (Desktop-Specific)

ViewModels in Compose Desktop don't have Android's `ViewModel` class. Manage scope explicitly:

**Pattern:**
```kotlin
class FeatureViewModel(
    private val adbManager: AdbManager,
    private val deviceRepository: DeviceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(FeatureUiState())
    val state: StateFlow<FeatureUiState> = _state.asStateFlow()

    fun onEvent(event: FeatureEvent) { ... }

    /** MUST be called on app dispose — cancels all coroutines. */
    fun close() { scope.cancel() }
}
```

**Rules:**
- Use `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` — never `viewModelScope` (doesn't exist outside Android).
- Expose `fun close()` to cancel scope. Call from `DisposableEffect.onDispose` at app root.
- `SupervisorJob` prevents one failed child from cancelling siblings.
- `Dispatchers.Main.immediate` for state updates — safe because Compose Desktop runs on Swing EDT.

**Koin registration:**
- Use `single` for ViewModels (not `factory`). Rationale: feature-scoped VMs outlive individual screen composables in a desktop docking/tab environment. `factory` would create new instances on every `koinInject()`.
- Exception: truly transient screens (e.g., a one-shot dialog) may use `factory`.

---

## 16. Structured Logging (Hard Requirement)

Every ADB operation, error, and significant state change must emit structured logs.

```kotlin
interface AppLogger {
    fun info(event: String, params: Map<String, Any> = emptyMap())
    fun warn(event: String, params: Map<String, Any> = emptyMap())
    fun error(event: String, params: Map<String, Any> = emptyMap(), throwable: Throwable? = null)
}
```

**Required events:**
| Event | When |
|-------|------|
| `adb_command_executed` | Every `AdbManager.run()` — include serial, command id, durationMs, success |
| `adb_command_failed` | Command returns `AppResult.Error` — include error details |
| `device_connected` | New device appears in `adb devices` |
| `device_disconnected` | Device drops from list |
| `device_selected` | User switches active device |
| `feature_screen_opened` | Navigation to any screen |
| `task_started` / `task_completed` / `task_failed` | TaskManager lifecycle |

**Rules:**
- Include correlation ID per operation (UUID or timestamp-based).
- Log at point of origin (AdbManager, DeviceRepository, ViewModel).
- Never log secrets, tokens, or full file paths with user data.

---

## 17. Desktop Window & Top-Level State

Compose Desktop has no Activity lifecycle. Top-level state lives in `main()`.

**Pattern:**
```kotlin
fun main() = application {
    startKoin { modules(appModules) }

    var themeMode by remember { mutableStateOf(ThemeMode.System) }
    var navigationItem by remember { mutableStateOf(NavigationItem.Apps) }

    // ... Window, MenuBar, content
}
```

**Rules:**
- `mutableStateOf` at window scope is allowed — mutations happen on Swing EDT (main thread).
- Navigation state (`NavigationItem`) and theme state are owned by `main()`, not injected.
- `DisposableEffect(Unit)` at window scope for starting/stopping repositories and cancelling VM scopes.
- Koin `startKoin` must be called once in `main()`, not inside composables.

---

## 18. Command Palette / Commander

The Commander (⌘⇧ double-shift) is a cross-cutting UI concern, not a feature.

**Rules:**
- `CommanderRegistry` is a `single` in Koin — holds all registered actions.
- Actions registered at startup in `LaunchedEffect(Unit)` within `main()`.
- Each feature can register its own actions via the registry.
- `CommanderDialog` is a pure composable — receives registry, emits action selections.
- No business logic in CommanderDialog — only filtering and selection.

---

## 19. What To Avoid

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

## 20. UI Rules — Material Design 3 (Tooling-Grade)

### Philosophy
**M3 is the foundation, not a prison.** This is a power-user desktop tool (like Android Studio, IntelliJ, Wireshark), not a consumer mobile app. Use M3 defaults for standard chrome, but allow custom composables where M3 is insufficient for desktop tooling.

### 20.1 Standard UI (M3 defaults required)
- Buttons, text fields, dialogs, top bars, navigation bars, switches, sliders
- Color scheme (`MaterialTheme.colorScheme.*`), typography scale (`MaterialTheme.typography.*`), shape system
- Icons (material-icons-extended)

### 20.2 Custom Composables (Allowed Where Needed)
Allowed for tooling-specific needs: data tables, dense log viewers, terminal output, split panes, tree explorers, property inspectors, device dashboards, virtualized lists, resizable panels, dockable windows, tab groups.

**Rules for custom composables:**
- Use `MaterialTheme.colorScheme.*` for colors and `MaterialTheme.typography.*` for text styles.
- Never define custom `Color` values — use only `colorScheme` properties.
- Never define custom `TextStyle` or font sizes — use only `typography` properties.
- Never hardcode `dp` values outside the M3 spacing scale (`4, 8, 12, 16, 24`).
- Wrapping M3 components for ergonomics is OK — no styling overrides in the wrapper.

### 20.3 Naming
- **Files**: `PascalCase.kt` matching primary class/composable.
- **Composables**: PascalCase, noun-based.
- **Functions**: camelCase, verb-based.
- **ViewModels**: `*ViewModel.kt`, expose `StateFlow` / `MutableStateFlow`.
- **Screens**: `*Screen.kt` — full-page or panel view composable.

### 20.4 UX Rules
- Use `Scaffold` with `TopAppBar` for primary screens. Panels/inspectors may opt out.
- Navigation: state-driven (no 3rd-party nav libs).
- Dialogs: prefer M3 `AlertDialog` — custom dialogs allowed for complex tooling UIs.
- Loading: M3 `LinearProgressIndicator` or `CircularProgressIndicator`.
- Empty states: brief `Text` message.
- Error states: dialog or inline text with `color = MaterialTheme.colorScheme.error`.
- Confirm destructive actions.

### 20.5 Code Conventions
- No HTML-like comments (`//` or `/* */` only where necessary).
- No `@Preview` in production code.
- `remember` / `derivedStateOf` for local state; `collectAsState()` for ViewModel state.
- `LaunchedEffect` for side effects (ADB commands, timers).
- `withContext(Dispatchers.IO)` for blocking ADB operations.
- Explicit M3 imports — no wildcard imports except `compose.material3.*`.

### 20.6 ADB-Specific Conventions
- All ADB shell commands through a single abstraction in `adb/`.
- Device connection state via `StateFlow` in `DeviceViewModel`.
- Logcat in `LazyColumn` with `reverseLayout = true` — M3 text styling for consistency.
- Device list uses `LazyColumn` with M3 `ListItem` or `Card`.

### 20.7 What We Do NOT Do

| Disallowed | Instead Use |
|---|---|
| Custom `Color` values | `MaterialTheme.colorScheme.*` |
| Custom `TextStyle` / font sizes | `MaterialTheme.typography.*` |
| Custom `Shape` / `RoundedCornerShape` | `MaterialTheme.shapes.*` |
| Custom color palettes | `MaterialTheme.colorScheme.*` |
| Third-party UI libraries | Not allowed |
| Custom window chrome | Default OS window chrome |
| Hardcoded colors | `Color.Unspecified` or `MaterialTheme.colorScheme.*` |
