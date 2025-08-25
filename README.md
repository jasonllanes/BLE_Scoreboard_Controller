# BLE Scoreboard Controller (Android)

A native Android controller app for a basketball scoreboard. It connects to one or more BLE gateways (Arduino + HM‑10 or similar) and sends single‑byte ASCII commands to manipulate game state (scores, fouls, horn, shot clock, arrows).

- Package: `com.example.ble_scoreboard`
- Min SDK: 30
- Target/Compile SDK: 34
- Primary screens: [LoginActivity](/app/src/main/java/com/example/ble_scoreboard/LoginActivity.java), [HomeActivity](/app/src/main/java/com/example/ble_scoreboard/HomeActivity.java), [SettingsActivity](/app/src/main/java/com/example/ble_scoreboard/SettingsActivity.java), [ControlPanelActivity](/app/src/main/java/com/example/ble_scoreboard/ControlPanelActivity.java)

## Why ASCII bytes?

The scoreboard firmware expects single‑byte commands corresponding to printable ASCII characters (legacy protocol from the client’s old app). We preserve this protocol for compatibility. In code we use character literals (e.g., `'v'`) that compile to the same byte values (e.g., 118).

## Features

- Login and simple user session (demo credentials)
- Settings to store device MAC/name pairs (currently up to 3)
- Control panel with buttons mapped to scoreboard commands
- BLE scanning, GATT connect, HM‑10 service/characteristic discovery
- Sends single‑byte ASCII commands on button press
- Status feedback and initial read/notifications

## App Flow

1. [MainActivity](/app/src/main/java/com/example/ble_scoreboard/MainActivity.java) shows a splash then routes to:
   - [LoginActivity](/app/src/main/java/com/example/ble_scoreboard/LoginActivity.java) (demo: `admin` / `password`), or
   - [HomeActivity](/app/src/main/java/com/example/ble_scoreboard/HomeActivity.java) if already logged in
2. [SettingsActivity](/app/src/main/java/com/example/ble_scoreboard/SettingsActivity.java) stores device MAC/name(s) in `SharedPreferences`
3. [ControlPanelActivity](/app/src/main/java/com/example/ble_scoreboard/ControlPanelActivity.java) scans for the configured device, connects, and sends commands

## Command Protocol (ASCII → Action)

The app sends exactly one byte per action. For readability we show both char and decimal:

- 48 '0' — Null
- 95 '\_' — Gametime/Shotclock Horn
- 113 'q' — Shot clock reset to 14
- 114 'r' — Shot clock reset to 24
- 118 'v' — New Game
- 106 'j' — Team A score +1
- 107 'k' — Team A score +2
- 108 'l' — Team A score −1
- 109 'm' — Team A foul +1
- 67 'C' — Team A foul −1
- 110 'n' — Team A TOL −1
- 68 'D' — Team A TOL +1
- 97 'a' — Team B score +1
- 98 'b' — Team B score +2
- 100 'd' — Team B score −1
- 99 'c' — Team B foul +1
- 65 'A' — Team B foul −1
- 101 'e' — Team B TOL −1
- 66 'B' — Team B TOL +1
- 86 'V' — Right arrow
- 87 'W' — Left arrow

Note: Earlier client notes showed 108 used for two actions; the app uses 109 ('m') for Team A foul +1 to avoid collision. Ensure the Arduino firmware matches this table.

## BLE Details

- Module: HM‑10 (UART‑over‑BLE)
- Service UUID: `0000ffe0-0000-1000-8000-00805f9b34fb`
- Characteristic UUID: `0000ffe1-0000-1000-8000-00805f9b34fb`
- Transport: write single byte to the characteristic. Notifications optional.

## Android Permissions

Declared in [app/src/main/AndroidManifest.xml](/app/src/main/AndroidManifest.xml):

- API ≤ 30: `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION` (scan requirement)
- API ≥ 31: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` (runtime)

Current code requests location; add runtime requests for `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` on Android 12+ for full compatibility.

## Build & Run

1. Open in Android Studio (Giraffe+), sync Gradle.
2. Build variants: `debug`/`release` (default config in [app/build.gradle.kts](/app/build.gradle.kts)).
3. Run on a device with Bluetooth enabled.

## Usage

1. Login: username `admin`, password `password`.
2. Go to Settings and enter your scoreboard device MAC/name (at least Device 1).
3. From Home, open Control Panel.
4. Tap “Connect” to scan and connect. Wait for “Ready to send commands.”
5. Use the command buttons (scores, fouls, horn, shot clock, arrows).

## Project Structure

- `app/src/main/java/com/example/ble_scoreboard/`
  - [MainActivity.java](/app/src/main/java/com/example/ble_scoreboard/MainActivity.java) — splash, navigation
  - [LoginActivity.java](/app/src/main/java/com/example/ble_scoreboard/LoginActivity.java) — demo login
  - [HomeActivity.java](/app/src/main/java/com/example/ble_scoreboard/HomeActivity.java) — entry actions
  - [SettingsActivity.java](/app/src/main/java/com/example/ble_scoreboard/SettingsActivity.java) — device configuration
  - [ControlPanelActivity.java](/app/src/main/java/com/example/ble_scoreboard/ControlPanelActivity.java) — BLE scan/connect and command UI
  - [utils/BLECommandUtil.java](/app/src/main/java/com/example/ble_scoreboard/utils/BLECommandUtil.java) — ASCII command constants and BLE write helper
- [app/src/main/AndroidManifest.xml](/app/src/main/AndroidManifest.xml) — permissions and activity declarations
- Layouts under `app/src/main/res/layout/` (not listed here)

## Architecture (current)

- UI buttons call [BLECommandUtil.processCommand(byte)](/app/src/main/java/com/example/ble_scoreboard/utils/BLECommandUtil.java) which:
  - Maps UI intent → byte (e.g., `'v'`)
  - Writes to HM‑10 characteristic via `BluetoothGatt`

## Roadmap (client goals)

- Multi‑device support
  - Manage multiple concurrent GATT connections
  - Device list UI with per‑device connect and “broadcast” send
  - Queue/serialize writes per device
- Platform fixes
  - Request `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` at runtime on API 31+
  - Remove class‑level `@RequiresApi(S)` and guard by SDK checks
  - Prompt to enable Bluetooth if off
- Reliability
  - Write CCC descriptor (0x2902) when enabling notifications
  - Reconnect/backoff policy
  - Better error/status reporting
- Config UX
  - Replace fixed 3 devices with an editable list (RecyclerView; persisted in SharedPreferences or Room)
- Transport abstraction
  - Keep the ASCII command table as a codec
  - BLE HM‑10 transport now; consider BT Classic SPP transport if needed later

## Development Notes

- Java 11 source/target; AndroidX appcompat/material/preference/constraintlayout.
- Ensure the command table stays in sync with firmware. When changing bytes, coordinate with the Arduino team.

## License

TBD.
