# Device Integrity

## Root detection
- Heuristics: su/magisk paths, test-keys, BusyBox, mount flags, `ro.debuggable`.
- Enable: `features.rootDetection = true`
- Threshold: `thresholds.rootSignalsToBlock`
- Policy: `policy.onRoot`

## Emulator detection
- Supported: Android Studio/QEMU, Genymotion, BlueStacks, Nox, LDPlayer, MEmu, MuMu.
- Signals: build/system props, QEMU/VBox device files, `/proc` markers, default IP 10.0.2.15, low sensor count.
- Enable: `features.emulatorDetection = true`
- Threshold: `thresholds.emulatorSignalsToBlock`
- Policy: `policy.onEmulator`

## Debugger / ptrace
- Enable: `features.debuggerDetection = true`
- Policy: `policy.onDebugger`

## USB debugging & developer options
- Enable: `features.usbDebugDetection = true`
- Policy: `policy.onUsbDebug`

## VPN / Proxy (MITM signals)
- Enable: `features.vpnDetection = true`, `features.mitmDetection = true`
- Policies: `policy.onVpn`, `policy.onMitm`
