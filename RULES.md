# ADBStudio — Project Rules

## Philosophy

This project uses **pure Google Material Design 3 (M3)** with **zero custom UI**. Every component, color, and type scale must use M3 defaults as provided by `androidx.compose.material3`. No custom drawables, no overridden colors, no custom text sizes, no custom component shapes — nothing. If M3 doesn't provide it out of the box, we don't add it.

---

## 1. UI Rules (Material Design 3 — Strict Mode)

### 1.1 Components
- Use **only** `androidx.compose.material3.*` components.
- No custom composables that wrap or re-style M3 components.
- No custom `Button`, `TextField`, `Card`, `NavigationBar`, `TopAppBar`, `AlertDialog`, `ModalBottomSheet`, etc.
- Each M3 component must be used with **default parameters only** — no overriding `colors`, `shape`, `typography`, or `elevation`.
- Exception: functional parameters like `onClick`, `enabled`, `expanded`, `onDismiss` are allowed.

### 1.2 Color
- **Never** define custom `Color` values.
- Use only `MaterialTheme.colorScheme.*` properties.
- Never pass custom colors to `Modifier.background()` or `Modifier.foreground()`.
- Never override `colorScheme` in `MaterialTheme`.

### 1.3 Typography
- **Never** define custom `TextStyle` or font sizes.
- Use only `MaterialTheme.typography.*` styles (`bodyLarge`, `titleMedium`, `labelSmall`, etc.).
- The `Text` composable must use `style = MaterialTheme.typography.*` — no inline `fontSize`, `fontWeight`, or `lineHeight`.
- Never override `typography` in `MaterialTheme`.

### 1.4 Spacing / Layout
- Use only M3 reference sizes (`4.dp`, `8.dp`, `12.dp`, `16.dp`, `24.dp`) from `MaterialTheme` conventions.
- No hardcoded custom `dp` values for margins/paddings.
- Prefer `Modifier.sizeIn()`, `Modifier.widthIn()`, `Modifier.heightIn()` over fixed sizes for responsiveness.

### 1.5 Icons
- Use only `androidx.compose.material.icons.Icons.Default.*` or material-icons-extended.
- No custom SVG/vector drawables for UI icons.
- Icons must use `contentDescription` for accessibility.

### 1.6 Shape
- Never define custom `Shape` or `RoundedCornerShape` values.
- Use only `MaterialTheme.shapes.*` (`small`, `medium`, `large`, `extraLarge`).

### 1.7 Window
- Use default Compose Desktop window chrome (no custom title bar).
- Window size should use `WindowState` with reasonable defaults for an ADB tool (e.g., `DpSize(1200.dp, 800.dp)`).

---

## 2. Project Structure

```
ADBStudio/
├── desktopApp/             # JVM/Desktop entry point
│   └── src/main/kotlin/com/adbstudio/desktop/
│       └── main.kt         # Window & application entry
├── shared/                 # Shared KMP code
│   └── src/
│       ├── commonMain/kotlin/com/adbstudio/desktop/
│       │   ├── App.kt                    # Root composable
│       │   ├── ui/                       # UI screens & components
│       │   │   ├── screen/               # One file per screen
│       │   │   ├── component/            # Reusable M3-only composables
│       │   │   └── layout/               # App shell (navigation, scaffold)
│       │   ├── viewmodel/                # ViewModels per screen
│       │   ├── model/                    # Domain models / data classes
│       │   ├── adb/                      # ADB bridge / device interaction
│       │   └── util/                     # Pure utility functions (no UI)
│       ├── jvmMain/                      # Platform-specific (JVM)
│       └── commonTest/
└── RULES.md
```

### Naming
- **Files**: `PascalCase.kt` matching the primary class/composable.
- **Composables**: PascalCase, noun-based (`DeviceList`, `DeviceCard`).
- **Functions**: camelCase, verb-based (`connectToDevice`, `fetchLogs`).
- **ViewModels**: `*ViewModel.kt`, expose `StateFlow` / `MutableStateFlow`.
- **Screens**: `*Screen.kt` — a screen is a composable that is a full-page view.

---

## 3. UX Rules

- Every screen must use `Scaffold` with a `TopAppBar` and optional `NavigationBar`.
- Navigation: use a simple state-driven approach (no 3rd-party nav libs unless M3 provides one).
- Dialogs: use M3 `AlertDialog` only — no custom dialog composables.
- Loading states: use M3 `LinearProgressIndicator` or `CircularProgressIndicator`.
- Empty states: show a `Text` with `bodyLarge` style and a brief message.
- Error states: use M3 `AlertDialog` or inline `Text` in `style = MaterialTheme.typography.bodyMedium` with `color = MaterialTheme.colorScheme.error`.
- Confirm destructive actions with an M3 `AlertDialog` (delete device, disconnect, etc.).

---

## 4. Code Conventions

- **No HTML-like comments** (`//` or `/* */` only where strictly necessary).
- **No `@Preview`** in production code.
- Use `remember` and `derivedStateOf` for local state; `collectAsState()` for ViewModel state.
- Use `LaunchedEffect` for side effects (ADB commands, timers).
- Use `withContext(Dispatchers.IO)` for blocking ADB operations.
- Import M3 components explicitly — no wildcard imports except for `compose.material3.*`.

---

## 5. ADB-Specific Conventions

- All ADB shell commands go through a single abstraction in `shared/src/commonMain/.../adb/`.
- Device connection state is managed via `StateFlow` in a `DeviceViewModel`.
- Logcat output is displayed in an M3 `LazyColumn` with `Text(style = MaterialTheme.typography.bodySmall)`.
- Device list uses M3 `LazyColumn` with `ListItem` or `Card` composables.

---

## 6. What We Do NOT Do

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
| Hardcoded colors | Only `Color.Unspecified` or `MaterialTheme.colorScheme.*` |
