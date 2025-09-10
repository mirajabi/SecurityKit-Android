# Policy & Thresholds

Map each signal to actions: ALLOW, WARN, BLOCK, TERMINATE.

## Policy example
```json
{
  "policy": {
    "onRoot": "BLOCK",
    "onEmulator": "BLOCK",
    "onDebugger": "WARN",
    "onUsbDebug": "WARN",
    "onVpn": "WARN",
    "onMitm": "BLOCK",
    "onPlayIntegrityFailure": "WARN",
    "onAppIntegrityFailure": "BLOCK",
    "onConfigTampering": "BLOCK",
    "onStrongBoxUnavailable": "WARN"
  }
}
```

## Thresholds
```json
{
  "thresholds": {
    "emulatorSignalsToBlock": 2,
    "rootSignalsToBlock": 2,
    "playIntegritySignalsToBlock": 1,
    "appIntegritySignalsToBlock": 1
  }
}
```

## Device overrides
```json
{
  "overrides": {
    "allowedModels": ["Pixel 6"],
    "allowedBrands": ["google"],
    "allowedManufacturers": ["Genymobile"],
    "allowedProducts": ["motion_phone_arm64"],
    "allowedDevices": ["motion_phone_arm64"],
    "allowedBoards": []
  }
}
```
If any allowed entry matches, all checks are bypassed (useful for QA/emulators).
